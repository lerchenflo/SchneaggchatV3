package org.lerchenflo.schneaggchatv3mp.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.navigation.NavigationBadgeCounts
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.datasource.network.AppJson
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SocketConnectionManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SocketConnectionMessage
import org.lerchenflo.schneaggchatv3mp.app.domain.AppInfoRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.EventRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageRepository
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.hasEnded
import org.lerchenflo.schneaggchatv3mp.events.domain.isUnseenBy
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.IncomingDataManager
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import org.lerchenflo.schneaggchatv3mp.utilities.battery.BatteryService
import org.lerchenflo.schneaggchatv3mp.utilities.location.LocationService
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes


@OptIn(ExperimentalCoroutinesApi::class)
class GlobalViewModel(
    private val dataSyncCoordinator: DataSyncCoordinator,
    private val chatListRepository: ChatListRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository,
    private val appInfoRepository: AppInfoRepository,
    private val settingsRepository: SettingsRepository,
    private val socketConnectionManager: SocketConnectionManager,
    private val navigator: Navigator,
    private val locationService: LocationService,
    private val batteryService: BatteryService,
    private val appVersion: AppVersion
): ViewModel() {

    /** Guards the app-resume handler so overlapping resume events are dropped instead of queued. */
    private var appResumeJob: Job? = null

    // Events fall out of the badge as they end, which no database write announces - so the count
    // is re-evaluated on a slow tick as well as on every data change. A minute is far below the
    // resolution anyone reads a badge at, and each tick only re-counts an in-memory list.
    private val badgeClock = flow {
        while (true) {
            emit(Clock.System.now().toEpochMilliseconds())
            delay(1.minutes)
        }
    }

    /** Unread/unseen counts behind the bottom navigation bar's badges. */
    val navigationBadgeCounts: StateFlow<NavigationBadgeCounts> = combine(
        SessionCache.authState,
        chatListRepository.getUnreadChatCountFlow(),
        eventRepository.getAllEventsFlow(),
        badgeClock,
    ) { authState, unreadChats, events, nowMillis ->
        // Logged out there is nobody to count for, and the local database still holds the previous
        // session's rows until it is cleared - badging those would leak them into the login screen.
        val ownUserId = (authState as? SessionCache.AuthState.LoggedIn)?.userId

        if (ownUserId == null) {
            NavigationBadgeCounts()
        } else {
            NavigationBadgeCounts(
                unreadChats = unreadChats,
                unseenEvents = events.count { it.isUnseenBy(ownUserId) && !it.hasEnded(nowMillis) }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NavigationBadgeCounts()
    )

    init {

        NotificationManager.initialize()

        //A chat on screen - just opened, or the app came back with it open - has its notifications
        //dismissed, messages and reactions alike. Deliberately not tied to marking it read: a chat
        //read on another device has nothing unread here, but its notification must still go.
        viewModelScope.launch {
            combine(OpenChatTracker.current, AppLifecycleManager.isAppInForegroundFlow) { chat, foreground ->
                chat.takeIf { foreground }
            }
                .distinctUntilChanged()
                .filterNotNull()
                .collect { chat -> NotificationManager.removeChatNotifications(chat.chatId) }
        }


        viewModelScope.launch {
            AppLifecycleManager.appResumedEvent
                .combine(SessionCache.onlineFlow.asFlow()) { event, online ->
                    if (online) event else null
                }
                .filterNotNull()
                .combine(SessionCache.authState.asFlow()) { event, loggedin ->
                    if (loggedin is SessionCache.AuthState.LoggedIn) event else null
                }
                .filterNotNull()
                .collect {
                    //App resumed event, only if the user is logged in and online

                    // Discard resume events that arrive while a previous one is still being
                    // handled. The collector must not block: appResumedEvent has an
                    // extraBufferCapacity of 1, so suspending here would park an event that
                    // immediately re-triggers a full sync once we return.
                    if (appResumeJob?.isActive == true) return@collect

                    val ownId = SessionCache.requireLoggedIn()?.userId ?: return@collect

                    if (SessionCache.isLoggedIn()) {

                        appResumeJob = viewModelScope.launch {
                            startSocketConnection()
                            updateLocationTracking()

                            //println("App resumed and logged in, triggering sync...")
                            messageRepository.sendOfflineMessages(ownId)
                            dataSyncCoordinator.dataSync(reason = "appResumed")

                            //On resume clear all error notis
                            NotificationManager.removeNotification(NotificationManager.NotiIdType.ERROR.baseId)

                            //println("Incoming Data from app resume: ${IncomingDataManager.sharedText.value}")
                            if(IncomingDataManager.isNewDataAvailable()){
                                navigator.navigate(Route.MessageChatSelector) // todo build backstack?
                            }
                        }
                    }
                }
        }


















        viewModelScope.launch {
            while (true) {

                if (SessionCache.isOnline()) {
                    // After a drop the manager's own backoff loop owns reconnection; only step in
                    // when nothing is pending (first connect, or after that loop gave up).
                    if (!socketConnectionManager.isConnectedNow() && !socketConnectionManager.isReconnectPending()) {

                        if (appVersion.isDesktop()) { //On desktop always try to hold the socket connection for notifications
                            startSocketConnection()
                        } else { //On Mobile only connect if the app is active
                            if (AppLifecycleManager.isAppInForeground) {
                                startSocketConnection()
                            }
                        }
                    }

                    // Session (re)establishment is owned by AuthSessionManager's own scheduler;
                    // raising a Login action from here every 5 s is what produced the refresh storm.
                    SessionCache.requireLoggedIn()?.userId?.let {
                        messageRepository.sendOfflineMessages(it)
                    }

                } else {
                    //Offline
                    appInfoRepository.testServer(settingsRepository.getServerUrl())

                }

                delay(5000.milliseconds)
            }
        }

        viewModelScope.launch {
            // Plain collect (not collectLatest): a close in progress must never be cancelled by
            // a follow-up lifecycle event, or the socket could survive into the background.
            AppLifecycleManager.appBackgroundedEvent.collect {
                // Mobile: drop the socket the moment the app leaves the foreground (home, app
                // switch, screen lock), so the server switches this user to FCM/APNs delivery.
                // Desktop: the socket is the only notification channel, so it stays up while the
                // window is minimized; the manager reconnects it on its own if it drops.
                if (!appVersion.isDesktop()) {
                    socketConnectionManager.close()
                }
                stopLocationTracking()
            }
        }

        // Handles a notification tap requesting a chat be opened. AppLifecycleManager holds the
        // request as a StateFlow (not a one-shot event) so a tap that arrives before this
        // collector exists - e.g. a cold app start - is not silently dropped, and only acted on
        // once the user is logged in AND the auth-gated startup navigation is done (otherwise
        // that navigation's own backstack reset would immediately undo it).
        viewModelScope.launch {
            combine(
                AppLifecycleManager.pendingNotificationOpen,
                SessionCache.authState,
                AppLifecycleManager.startupRoutingDone,
            ) { pending, authState, routingDone -> Triple(pending, authState, routingDone) }
                // Plain collect (not collectLatest): consuming the request below writes null back
                // into pendingNotificationOpen, which re-emits through the combine - collectLatest
                // took that as "newer value" and cancelled this very block at its next suspension
                // point, so the navigation could be dropped halfway and the sync never ran at all.
                .collect { (pending, authState, routingDone) ->
                    if (pending == null || !routingDone) return@collect
                    val ownId = (authState as? SessionCache.AuthState.LoggedIn)?.userId ?: return@collect

                    val data = AppLifecycleManager.consumePendingNotificationOpen() ?: return@collect
                    println("App started from notification, synching data")

                    // Its own coroutine: the navigate() calls below park until the app is STARTED
                    // again (ObserveAsEvents only collects from then on), and the sync that brings
                    // the chat up to date must not queue behind that.
                    viewModelScope.launch {
                        messageRepository.sendOfflineMessages(ownId)
                        dataSyncCoordinator.dataSync(reason = "notificationOpened")
                    }

                    // Navigate to the chat that the notification belongs to. Switch to the chat
                    // tab first - a bare Route.Chat push lands on whichever tab is currently
                    // selected (see App.kt's NavigationAction.Navigate handling).
                    data.chatId?.let { chatId ->
                        navigator.navigate(Route.ChatSelector)
                        navigator.navigate(Route.Chat(chatId = chatId, isGroup = data.isGroup))
                    }
                }
        }

        // Track the own user's "share my location" setting reactively and (re-)evaluate
        // location tracking whenever the login state or that setting changes.
        viewModelScope.launch {
            SessionCache.authState.flatMapLatest { authState ->
                if (authState is SessionCache.AuthState.LoggedIn) {
                    userRepository.getUserByIdFlow(authState.userId)
                } else {
                    flowOf(null)
                }
            }.collectLatest { ownUser ->
                ownLocationShared = ownUser?.locationShared ?: false
                updateLocationTracking()
            }
        }
    }

    private var ownLocationShared = false

    /** Re-evaluates login state, foreground state, and the share setting, then starts/stops tracking. */
    private fun updateLocationTracking() {
        if (SessionCache.isLoggedIn() && AppLifecycleManager.isAppInForeground && ownLocationShared) {
            startLocationTracking()
        } else {
            stopLocationTracking()
        }
    }


    fun startSocketConnection() {
        viewModelScope.launch {
            if (!socketConnectionManager.isConnectedNow() && SessionCache.isLoggedIn()){
                val serverurl = SocketConnectionManager.getSocketUrl(settingsRepository.getServerUrl())
                socketConnectionManager.connect(
                    serverUrl = serverurl,
                    onError = {
                        //startSocketConnection()
                        if (!socketConnectionManager.isConnectedNow()) {
                            //SessionCache.updateOnline(false)
                        }
                    },
                    onClose = {}
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            socketConnectionManager.close()
        }
    }




    private var permissionRequestJob: Job? = null
    private var locationTrackingJob: Job? = null

    private fun startLocationTracking() {
        // Desktop has no GPS and can't request the permission - never touch location tracking
        // (or the synced "share my location" setting) from here on non-mobile platforms, so a
        // desktop session never disables location sharing for the user's phone.
        if (!appVersion.isMobile()) return
        if (locationTrackingJob != null || permissionRequestJob != null) return // Already tracking or requesting
        if (!ownLocationShared) return // Sharing disabled - don't even ask for permission

        // Requesting the permission shows a system dialog, which briefly pauses/backgrounds
        // our activity. That must NOT cancel this in-flight request (stopLocationTracking()
        // only cancels locationTrackingJob), otherwise we'd lose the result and re-prompt
        // again on every resume.
        permissionRequestJob = viewModelScope.launch {
            val permission = locationService.requestLocationPermission()
            permissionRequestJob = null

            if (permission == PermissionState.DENIED) {
                // User explicitly denied the permission - turn sharing back off so we
                // don't prompt again on every resume/sync; they can re-enable it manually.
                println("Location tracking: Permission denied, disabling location sharing")
                ownLocationShared = false
                userRepository.disableLocationSharingForAllFriends()
            }

            if (permission != PermissionState.GRANTED) {
                println("Location tracking: Permission not granted")
                return@launch
            }

            locationTrackingJob = viewModelScope.launch {

                locationService.getLocationFlow()
                    .filterNotNull() //filter location for not null
                    .combine(socketConnectionManager.connectedFlow) { location, connected ->
                        if (connected) location else null
                    }
                    .filterNotNull() //filter for connected
                    .collect { location ->

                        // Speed and heading are sent if any user in the friends list has shareSpeedHeading enabled
                        val shareSpeedHeading = userRepository.getFriends("").any { it.shareSpeedHeading }
                        val update = SocketConnectionMessage.LocationUpdate(
                            lat = location.coordinates.lat,
                            long = location.coordinates.long,
                            speed = if (shareSpeedHeading) location.speed else null,
                            heading = if (shareSpeedHeading) location.heading?.toDouble() else null,
                            altitude = location.altitude?.toDouble(),
                            batteryLevel = batteryService.getBatteryLevel(),
                        )
                        socketConnectionManager.sendMessage(AppJson.instance.encodeToString(update as SocketConnectionMessage))
                    }



                /*
                locationService.getLocationFlow().collectLatest { location ->


                    if (location != null && socketConnectionManager.isConnectedNow()) {

                    }
                }

                 */
            }
        }
    }

    private fun stopLocationTracking() {
        locationTrackingJob?.cancel()
        locationTrackingJob = null
    }
}
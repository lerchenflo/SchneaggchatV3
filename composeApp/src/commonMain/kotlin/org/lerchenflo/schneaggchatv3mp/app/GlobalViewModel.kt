package org.lerchenflo.schneaggchatv3mp.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.NotSelected
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.isNotSelected
import org.lerchenflo.schneaggchatv3mp.chat.domain.toSelectedChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.AppJson
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SocketConnectionManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SocketConnectionMessage
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.IncomingDataManager
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import org.lerchenflo.schneaggchatv3mp.utilities.battery.BatteryService
import org.lerchenflo.schneaggchatv3mp.utilities.location.LocationService
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalCoroutinesApi::class)
class GlobalViewModel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager,
    private val socketConnectionManager: SocketConnectionManager,
    private val navigator: Navigator,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository,
    private val locationService: LocationService,
    private val batteryService: BatteryService,
    private val appVersion: AppVersion
): ViewModel() {

    init {

        NotificationManager.initialize()

        // Sync when app is resumed
        viewModelScope.launch {
            AppLifecycleManager.appResumedEvent.collectLatest {
                //println("App resumed, checking loggedin status")
                val ownId = SessionCache.requireLoggedIn()?.userId ?: return@collectLatest

                if (SessionCache.isLoggedIn()) {
                    //println("App resumed and logged in, triggering sync...")
                    appRepository.sendOfflineMessages(ownId)
                    appRepository.dataSync()

                    //On resume clear all error notis
                    NotificationManager.removeNotification(NotificationManager.NotiIdType.ERROR.baseId)

                    //println("Incoming Data from app resume: ${IncomingDataManager.sharedText.value}")
                    if(IncomingDataManager.isNewDataAvailable()){
                        navigator.navigate(Route.MessageChatSelector) // todo build backstack?
                    }

                    startSocketConnection()
                    updateLocationTracking()
                }
            }
        }

        viewModelScope.launch {
            while (true) {

                if (SessionCache.isOnline()) {
                    if (!socketConnectionManager.isConnectedNow()) {

                        if (appVersion.isDesktop()) { //On desktop always try to hold the socket connection for notifications
                            startSocketConnection()
                        } else { //On Mobile only connect if the app is in the foreground
                            if (AppLifecycleManager.isAppInForeground) {
                                startSocketConnection()
                            }
                        }
                    }

                    if (SessionCache.isLoggedIn()) {
                        SessionCache.requireLoggedIn()?.userId?.let {
                            appRepository.sendOfflineMessages(it)
                        }
                    } else {
                        AppRepository.ActionChannel.sendActionSuspend(AppRepository.ActionChannel.ActionEvent.Login)
                    }

                } else {
                    //Offline
                    appRepository.testServer(preferenceManager.getServerUrl())

                }

                delay(5000.milliseconds)
            }
        }

        viewModelScope.launch {
            AppLifecycleManager.appBackgroundedEvent.collectLatest {
                socketConnectionManager.close()
                stopLocationTracking()
            }
        }

        viewModelScope.launch {
            AppLifecycleManager.notificationOpenedEvent.collectLatest {
                println("App started from notification, synching data")
                val ownId = SessionCache.requireLoggedIn()?.userId ?: return@collectLatest
                if (SessionCache.isLoggedIn()) {
                    appRepository.sendOfflineMessages(ownId)
                    appRepository.dataSync()
                }
            }
        }

        // Track the own user's "share my location" setting reactively and (re-)evaluate
        // location tracking whenever the login state or that setting changes.
        viewModelScope.launch {
            SessionCache.authState.flatMapLatest { authState ->
                if (authState is SessionCache.AuthState.LoggedIn) {
                    appRepository.getUserByIdFlow(authState.userId)
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
                val serverurl = SocketConnectionManager.getSocketUrl(preferenceManager.getServerUrl())
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




    // Internal flow to track currently selected chat ID and type
    private data class ChatTarget(val chatId: String?, val isGroup: Boolean)
    private val _selectedChatTarget = MutableStateFlow(ChatTarget(null, false))

    // Reactive selectedChat flow that automatically updates from database
    val selectedChat = _selectedChatTarget.flatMapLatest { target ->
        if (target.chatId == null) {
            flowOf(NotSelected())
        } else {
            val isGroup = target.isGroup
            if (isGroup) {
                combine(
                    groupRepository.getGroupFlow(target.chatId),
                    messageRepository.getMessagesByUserIdFlow(target.chatId, true)
                ) { group, messages ->
                    val lastMessage = messages.maxByOrNull { it.sendDate }
                    var unreadCount = 0
                    var unsentCount = 0
                    
                    messages.forEach { message ->
                        // Only count as unread if it's NOT my message and NOT read by me
                        if (!message.myMessage && !message.readByMe) {
                            unreadCount++
                        }
                        if (!message.sent) unsentCount++
                    }
                    
                    group?.toSelectedChat(
                        unreadCount = unreadCount,
                        unsentCount = unsentCount,
                        lastMessage = lastMessage
                    ) ?: NotSelected()
                }
            } else {
                combine(
                    userRepository.getUserFlow(target.chatId),
                    messageRepository.getMessagesByUserIdFlow(target.chatId, false),
                    userRepository.onlineFriendIdsFlow
                ) { user, messages, onlineFriendIds ->
                    val lastMessage = messages.maxByOrNull { it.sendDate }
                    var unreadCount = 0
                    var unsentCount = 0

                    messages.forEach { message ->
                        // Only count as unread if it's NOT my message and NOT read by me
                        if (!message.myMessage && !message.readByMe) {
                            unreadCount++
                        }
                        if (!message.sent) unsentCount++
                    }

                    user?.toSelectedChat(
                        unreadCount = unreadCount,
                        unsentCount = unsentCount,
                        lastMessage = lastMessage,
                        isOnline = target.chatId in onlineFriendIds
                    ) ?: NotSelected()
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly, //Start emission directly
        initialValue = NotSelected()
    )

    suspend fun onSelectChat(chat: SelectedChat) {
        _selectedChatTarget.value = ChatTarget(chat.id, chat.isGroup)

        //Await selectedchat emission to not leave chat directly
        selectedChat.first { !it.isNotSelected() }
    }

    fun onLeaveChat(){
        _selectedChatTarget.value = ChatTarget(null, false)
    }

    private var permissionRequestJob: kotlinx.coroutines.Job? = null
    private var locationTrackingJob: kotlinx.coroutines.Job? = null

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
                appRepository.disableLocationSharingForAllFriends()
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

                        // Altitude/battery are sent by default whenever location sharing is on at
                        // all - the server already always shares them once a friend can see your
                        // location. Speed/heading are more revealing (live driving telemetry), so
                        // those only go out when "Advanced location sharing" is on.
                        val advanced = preferenceManager.getAdvancedLocationSharing()
                        val update = SocketConnectionMessage.LocationUpdate(
                            lat = location.coordinates.lat,
                            long = location.coordinates.long,
                            speed = if (advanced) location.speed else null,
                            heading = if (advanced) location.heading?.toDouble() else null,
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
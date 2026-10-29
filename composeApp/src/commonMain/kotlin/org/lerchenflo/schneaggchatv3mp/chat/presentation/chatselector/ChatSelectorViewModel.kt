@file:OptIn(FlowPreview::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Person3
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.announcement.domain.Announcement
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementRepository
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageSearchResult
import org.lerchenflo.schneaggchatv3mp.chat.domain.applySearchAndFilter
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.app.domain.AppInfoRepository
import org.lerchenflo.schneaggchatv3mp.login.domain.AuthRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageRepository
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncState
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.isNetworkerror
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.ChangelogEntry
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import org.lerchenflo.schneaggchatv3mp.utilities.isNewVersionHigher
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.Notifier
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.groups
import schneaggchatv3mp.composeapp.generated.resources.none
import schneaggchatv3mp.composeapp.generated.resources.persons
import schneaggchatv3mp.composeapp.generated.resources.unread
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/** How long the contribute popup stays away between two appearances. */
private val CONTRIBUTE_POPUP_INTERVAL_MILLIS = 19.days.inWholeMilliseconds

class ChatSelectorViewModel(
    private val appVersion: AppVersion,
    private val dataSyncCoordinator: DataSyncCoordinator,
    private val chatListRepository: ChatListRepository,
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    private val appInfoRepository: AppInfoRepository,
    private val navigator: Navigator,
    private val loggingRepository: Logger,
    private val permissionManager: PermissionManager,
    private val settingsRepository: SettingsRepository,
    private val announcementRepository: AnnouncementRepository,
    private val notifier: Notifier,
): ViewModel() {

    private val _notificationPermissionState = MutableStateFlow(PermissionState.GRANTED)
    val notificationPermissionState: StateFlow<PermissionState> = _notificationPermissionState.asStateFlow()

    private val _pendingFriendCount = MutableStateFlow(0)
    val pendingFriendCount: StateFlow<Int> = _pendingFriendCount.asStateFlow()

    /** Changelog of the current version, shown once after an update. */
    private val _changelogPopup = MutableStateFlow<ChangelogEntry?>(null)
    val changelogPopup: StateFlow<ChangelogEntry?> = _changelogPopup.asStateFlow()

    private val _contributePopupShown = MutableStateFlow(false)
    val contributePopupShown: StateFlow<Boolean> = _contributePopupShown.asStateFlow()

    /** Desktop only: tag of a newer GitHub release, null if up to date. */
    private val _newVersionAvailable = MutableStateFlow<String?>(null)
    val newVersionAvailable: StateFlow<String?> = _newVersionAvailable.asStateFlow()

    /** Newest announcement from the server, loaded once at start. */
    private val _latestAnnouncement = MutableStateFlow<Announcement?>(null)

    /**
     * The announcement banner, null when there is none to show. Hidden as soon as its number is
     * dismissed (here, or on another device via the synced settings) and when it expires, also
     * while the chat list stays open.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val announcement: StateFlow<Announcement?> = combine(
        _latestAnnouncement,
        settingsRepository.getLastDismissedAnnouncementIdFlow(),
    ) { latest, lastDismissedId ->
        latest?.takeIf { it.id > lastDismissedId }
    }
        .distinctUntilChanged()
        .flatMapLatest { shown ->
            flow {
                val remainingMillis = shown?.let { it.expiresAt - Clock.System.now().toEpochMilliseconds() } ?: 0L
                if (shown != null && remainingMillis > 0) {
                    emit(shown)
                    delay(remainingMillis.milliseconds)
                }
                emit(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Progress of the full sync, for the sync icon and its detail popup. */
    val dataSyncState: StateFlow<DataSyncState> = dataSyncCoordinator.dataSyncState

    /** Desktop has a reload button and no notification permission banner. */
    val isDesktop: Boolean = appVersion.isDesktop()

    val onboardingCompleted: StateFlow<Boolean> = settingsRepository.getOnboardingSeenFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val highlightTodaysTimestamp: StateFlow<Boolean> = settingsRepository.getHighlightTodaysMessageTimestampFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** The server URL as saved right now; the website and donation links open this one. */
    val serverUrl: StateFlow<String> = settingsRepository.getServerUrlFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, BASE_SERVER_URL)

    init {
        checkNotificationPermission()

        viewModelScope.launch {
            val token = notifier.getToken()

            //println("Chatselector notificationtoken: $token")

            if (!token.isNullOrEmpty()) {
                authRepository.setNotificationToken(token)
            }
        }

        viewModelScope.launch {
            val ownId = SessionCache.requireLoggedIn()?.userId ?: return@launch
            chatListRepository.getPendingFriends("").collectLatest { list ->
                val size = list.filter {
                    it.requesterId != ownId
                }.size

                _pendingFriendCount.value = size
            }
        }

        // Runs once per ViewModel, so the popups survive rotation and don't re-trigger when
        // navigating back to the chat selector
        viewModelScope.launch {
            checkStartupPopups()
        }

        viewModelScope.launch {
            loadAnnouncement()
        }

        if (appVersion.isDesktop()) {
            viewModelScope.launch {
                _newVersionAvailable.value = checkNewGitHubRelease()
            }
        }

    }


    private var refreshJob: Job? = null

    fun refresh() {
        if (refreshJob?.isActive == true) return

        val ownId = SessionCache.requireLoggedIn()?.userId ?: return

        refreshJob = viewModelScope.launch {
            try {
                messageRepository.sendOfflineMessages(ownId)
                dataSyncCoordinator.dataSync(reason = "chatSelectorRefresh")
            } catch (e: Exception) {
                ensureActive()
                loggingRepository.logWarning("ChatSelector refresh failed: ${e.message}")
            }
        }
    }


    //Navigation
    fun onChatSelected(selectedChat: ChatListItem) {
        viewModelScope.launch {

            //Chat opened, clear searchterm
            _searchTerm.value = ""

            navigator.navigate(Route.Chat(chatId = selectedChat.id, isGroup = selectedChat.isGroup))
        }
    }

    fun onOpenChatDetails(selectedChat: ChatListItem) {
        viewModelScope.launch {
            navigator.navigate(Route.ChatDetails(chatId = selectedChat.id, isGroup = selectedChat.isGroup))
        }
    }

    // for MessageChatSelector
    fun onBackClick() {
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }


    fun onChatSelectedWithMessage(selectedChat: ChatListItem) {
        viewModelScope.launch {

            //Chat opened, clear searchterm
            _searchTerm.value = ""

            navigator.navigate(
                Route.Chat(chatId = selectedChat.id, isGroup = selectedChat.isGroup),
                navigationOptions = Navigator.NavigationOptions(exitPreviousScreen = true)
            )
        }
    }

    fun onNewChatClick(){
        viewModelScope.launch {
            navigator.navigate(Route.NewChat)
        }
    }
    fun onSettingsClick() {
        viewModelScope.launch {
            navigator.navigate(Route.SettingsScreen)
        }
    }
    fun onRecapClick() {
        viewModelScope.launch {
            navigator.navigate(Route.Recap)
        }
    }
    fun onFeedbackClick() {
        viewModelScope.launch {
            navigator.navigate(Route.Feedback)
        }
    }
    fun onToolsAndGamesClick() {
        viewModelScope.launch {
            navigator.navigate(Route.GamesSelector)
        }
    }
    fun onMapClick(){
        viewModelScope.launch {
            navigator.navigate(Route.Schneaggmap())
        }
    }


    //Search / Filter
    private val _searchTerm = MutableStateFlow("")
    val searchTerm: StateFlow<String> = _searchTerm.asStateFlow()

    fun updateSearchterm(newValue: String) {
        _searchTerm.value = newValue
    }

    private val _filter = MutableStateFlow(ChatFilter.NONE)
    val filter: StateFlow<ChatFilter> = _filter.asStateFlow()

    fun updateFilter(newValue: ChatFilter) {
        _filter.value = newValue
    }

    fun pinUnpinChat(chat: ChatListItem) {
        viewModelScope.launch {
            chatListRepository.setPinnedChat(chat.id, group = chat.isGroup, pinned = chat.pinned <= 0L)
        }
    }




    /** Changelog after an update, otherwise maybe the contribute nudge - never both in one launch. */
    private suspend fun checkStartupPopups() {
        // The chat selector only shows its content once logged in (autologin may still be running),
        // and the contribute timestamp is written to the server
        SessionCache.authState.first { it is SessionCache.AuthState.LoggedIn }

        val currentVersion = appInfoRepository.getVersionName()

        if (settingsRepository.getLastStartedVersion() != currentVersion) {
            val changelog = appInfoRepository.getChangeLog(currentVersion)
            if (changelog != null) {
                // The version is only saved on dismiss, so a changelog that was never seen
                // (process death before it appeared) shows again on the next start
                _changelogPopup.value = changelog
            } else {
                // Nothing to show (offline or no entry for this version)
                settingsRepository.saveLastStartedVersion(currentVersion)
            }
            return
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val lastShown = settingsRepository.getLastContributePopupShown()

        when {
            // Never seeded on any device yet: seed the timestamp so the popup is due
            // one interval from now, not immediately. Normally already set server-side
            // (at registration, or by the migration for older accounts) and picked up by
            // the next sync — this only covers that sync not having landed yet.
            lastShown == 0L -> settingsRepository.setLastContributePopupShown(now)

            now - lastShown >= CONTRIBUTE_POPUP_INTERVAL_MILLIS -> {
                _contributePopupShown.value = true
                settingsRepository.setLastContributePopupShown(now)
            }
        }
    }

    private suspend fun loadAnnouncement() {
        SessionCache.authState.first { it is SessionCache.AuthState.LoggedIn }

        when (val result = announcementRepository.getLatest()) {
            is NetworkResult.Success -> _latestAnnouncement.value = result.data
            is NetworkResult.Error -> {
                // Offline: nothing to show, the next start tries again
                if (!result.error.isNetworkerror()) {
                    loggingRepository.logWarning("Loading the latest announcement failed: ${result.error}")
                }
            }
        }
    }

    fun onAnnouncementDismiss(announcement: Announcement) {
        viewModelScope.launch {
            settingsRepository.setLastDismissedAnnouncementId(announcement.id)
        }
    }

    fun onChangelogDismiss() {
        _changelogPopup.value = null
        viewModelScope.launch {
            settingsRepository.saveLastStartedVersion(appInfoRepository.getVersionName())
        }
    }

    fun onContributeDismiss() {
        _contributePopupShown.value = false
    }

    fun onNewVersionDismiss() {
        _newVersionAvailable.value = null
    }

    private suspend fun checkNewGitHubRelease(): String?{
        // get the github release name
        val tagName = appInfoRepository.getLatestGitHubVersionAsString() ?: "v0.0.0"
        println("github tag name: ${tagName}")

        // get the current app name
        val appversion = appInfoRepository.getVersionName()
        println("app version: ${appversion}")

        // check if the GitHub version is newer than your version
        try {
            if(isNewVersionHigher(appversion, tagName))
                return tagName
        } catch (e: IllegalArgumentException) {
            // Handle the bad data safely without crashing the app
            println("Error checking version: ${e.message}")
            return null
        }
        return null
    }



    //The unfiltered chat list. Started eagerly so the query is already running while the screen is
    //still being composed, and kept subscribed for the ViewModel's lifetime so coming back from a
    //chat never pays for it again.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val allChats: StateFlow<List<ChatListItem>> = SessionCache.authState
        .flatMapLatest { authState ->
            val loggedIn = authState as? SessionCache.AuthState.LoggedIn
                ?: return@flatMapLatest flowOf(emptyList())

            chatListRepository.getChatListFlow(userId = loggedIn.userId)
        }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    //Search and filter run on the already loaded list, so a keystroke never touches the database.
    val chatSelectorState: StateFlow<List<ChatListItem>> = combine(
        allChats,
        _searchTerm,
        _filter
    ) { chats, term, filter ->
        chats.applySearchAndFilter(searchTerm = term, filter = filter)
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )


    //Messages matching the search term, shown underneath the matching chats. Debounced because
    //every keystroke otherwise re-scans every message.
    @OptIn(ExperimentalCoroutinesApi::class)
    val messageSearchResults: StateFlow<List<MessageSearchResult>> = combine(
        _searchTerm.debounce(250.milliseconds),
        SessionCache.authState
    ) { term, authState -> term to authState }
        .flatMapLatest { (term, authState) ->
            val loggedIn = authState as? SessionCache.AuthState.LoggedIn
                ?: return@flatMapLatest flowOf(emptyList<MessageSearchResult>())

            chatListRepository.getMessageSearchFlow(
                searchTerm = term,
                userId = loggedIn.userId
            )
        }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Opens the chat a search result lives in and jumps to that message. */
    fun onMessageSearchResultSelected(result: MessageSearchResult) {
        viewModelScope.launch {

            //Chat opened, clear searchterm
            _searchTerm.value = ""

            navigator.navigate(
                Route.Chat(
                    chatId = result.chatId,
                    isGroup = result.isGroup,
                    highlightMessageId = result.messageId
                )
            )
        }
    }

    fun checkNotificationPermission() {
        viewModelScope.launch {
            _notificationPermissionState.value = permissionManager.checkNotificationPermission()
        }
    }

    fun requestNotificationPermission() {
        viewModelScope.launch {
            val result = permissionManager.requestNotificationPermission(openSettings = false)
            _notificationPermissionState.value = result
            if (result != PermissionState.GRANTED) {
                permissionManager.requestNotificationPermission(openSettings = true)
            }
        }
    }
}

enum class ChatFilter{
    NONE,
    PERSONS,
    GROUPS,
    UNREAD;
    fun toUiText(): UiText = when (this) {
        NONE -> UiText.StringResourceText(Res.string.none)
        PERSONS -> UiText.StringResourceText(Res.string.persons)
        GROUPS   -> UiText.StringResourceText(Res.string.groups)
        UNREAD -> UiText.StringResourceText(Res.string.unread)
    }
    fun getIcon(): ImageVector = when (this) {
        NONE -> Icons.Default.FilterNone
        PERSONS -> Icons.Default.Person3
        GROUPS   -> Icons.Default.Groups
        UNREAD -> Icons.Default.MarkChatUnread
    }
}

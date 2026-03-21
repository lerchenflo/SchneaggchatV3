@file:OptIn(FlowPreview::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Person3
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.PinnedChat
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.ChangelogEntry
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.groups
import schneaggchatv3mp.composeapp.generated.resources.none
import schneaggchatv3mp.composeapp.generated.resources.persons
import schneaggchatv3mp.composeapp.generated.resources.unread
import kotlin.time.Clock

class ChatSelectorViewModel(
    private val appRepository: AppRepository,
    private val navigator: Navigator,
    private val globalViewModel: GlobalViewModel,
    private val loggingRepository: LoggingRepository,
    private val preferenceManager: Preferencemanager,
): ViewModel() {


    private val _pendingFriendCount = MutableStateFlow(0)
    val pendingFriendCount: StateFlow<Int> = _pendingFriendCount.asStateFlow()

    init {

        viewModelScope.launch {
            val token = NotificationManager.getToken()

            if (token.isNotEmpty()) {
                appRepository.setFirebaseToken(token)
            }
        }

        viewModelScope.launch {
            val ownId = SessionCache.requireLoggedIn()?.userId ?: return@launch
            appRepository.getPendingFriends("").collectLatest { list ->
                val size = list.filter {
                    it.requesterId != ownId
                }.size

                _pendingFriendCount.value = size
            }
        }

        viewModelScope.launch {
            getChangelog()
        }
    }


    private var refreshJob: Job? = null

    fun refresh() {

        //println("Chatselector: Pull to refresh")
        // if a refresh is already running, do nothing
        if (refreshJob?.isActive == true) {
            println("Refreshjob already running, abort")
            return
        }

        val ownId = SessionCache.requireLoggedIn()?.userId ?: run {
            println("Refresh: userid not set, aborting")
            return
        }

        updateIsLoadingMessages(true)

        refreshJob = viewModelScope.launch {

            try {
                // send queued messages
                appRepository.sendOfflineMessages(ownId)

                appRepository.dataSync()
            } catch (e: Exception) {
                ensureActive()
                loggingRepository.logWarning("ChatSelector refresh failed: ${e.message}")
            }

            delay(200) //Debounce

            updateIsLoadingMessages(false)
        }
    }

    var isLoadingMessages by mutableStateOf(false)
        private set
    fun updateIsLoadingMessages(newValue: Boolean) {
        isLoadingMessages = newValue
    }


    //Navigation
    fun onChatSelected(selectedChat: SelectedChat) {
        viewModelScope.launch {

            //Chat opened, clear searchterm
            _searchTerm.value = ""

            globalViewModel.onSelectChat(selectedChat)
            navigator.navigate(Route.Chat)
        }
    }

    fun onNewChatClick(){
        viewModelScope.launch {
            navigator.navigate(Route.NewChat)
        }
    }
    fun onSettingsClick() {
        viewModelScope.launch {
            navigator.navigate(Route.Settings)
        }
    }
    fun onToolsAndGamesClick() {
        viewModelScope.launch {
            navigator.navigate(Route.Games)
        }
    }
    fun onMapClick(){
        viewModelScope.launch {
            navigator.navigate(Route.Schneaggmap)
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

    fun pinUnpinChat(chat: SelectedChat) {
        viewModelScope.launch {
            if(chat.pinned > 0L){
                preferenceManager.removePinnedChat(chat.id)
            } else {
                val now = Clock.System.now().toEpochMilliseconds()
                preferenceManager.addPinnedChat(PinnedChat(chat.id, now))
            }
        }
    }




    suspend fun getChangelog(): ChangelogEntry? {

        val appversion = appRepository.appVersion.getVersionName()
        val changelogEntry = appRepository.getChangeLog(appversion)
        println("CHANGELOG: $changelogEntry")
        return changelogEntry
    }



    @OptIn(ExperimentalCoroutinesApi::class)
    private val chatSelectorFlow: Flow<List<SelectedChat>> = combine(
        _searchTerm,
        _filter,
        SessionCache.authState

    ) { term, filter, authState -> Triple(term, filter, authState) }
        .flatMapLatest { (term, filter, authState) ->
            val loggedIn = authState as? SessionCache.AuthState.LoggedIn
                ?: return@flatMapLatest flowOf(emptyList())

            appRepository.getChatSelectorFlow(
                searchTerm = term,
                userId = loggedIn.userId,
                filter = filter
            )
        }
        .flowOn(Dispatchers.Default)

    val chatSelectorState: StateFlow<List<SelectedChat>> = chatSelectorFlow
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
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

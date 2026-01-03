@file:OptIn(FlowPreview::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person3
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.utilities.JwtUtils
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.groups
import schneaggchatv3mp.composeapp.generated.resources.none
import schneaggchatv3mp.composeapp.generated.resources.persons
import schneaggchatv3mp.composeapp.generated.resources.unread

class ChatSelectorViewModel(
    private val appRepository: AppRepository,
    private val navigator: Navigator,
    private val globalViewModel: GlobalViewModel
): ViewModel() {

    init {

        viewModelScope.launch {
            NotificationManager.removeNotification() //Clear all notis
            val token = NotificationManager.getToken()
            appRepository.setFirebaseToken(token)
        }

        //TODO: Maybe try login every 5 sec if not logged in (No sync all 5 secs)
        //Chat verlassen

        viewModelScope.launch {
            appRepository.getPendingFriends("").collectLatest { list ->
                val size = list.filter {
                    it.requesterId != SessionCache.getOwnIdValue()
                }.size

                _pendingFriendCount.value = size
            }
        }
    }

    private val _pendingFriendCount = MutableStateFlow(0)
    val pendingFriendCount: StateFlow<Int> = _pendingFriendCount.asStateFlow()


    fun clearChat(){
        viewModelScope.launch {
            globalViewModel.onLeaveChat()
        }
    }


    private var refreshJob: Job? = null

    fun refresh() {

        println("Chatselector: Pull to refresh")
        // if a refresh is already running, do nothing
        if (refreshJob?.isActive == true) {
            println("Refreshjob already running, abort")

            return
        }
        updateIsLoadingMessages(true)


        println("refreshjob starting")


        refreshJob = viewModelScope.launch {

            if (!SessionCache.loggedIn){

                println("Not logged in, trying to refresh token")
                appRepository.refreshTokens()

                val becameLoggedIn = withTimeoutOrNull(10_000L) {
                    while (!SessionCache.loggedIn) {
                        delay(1_000L) // retry after 1 second
                    }
                    true // logged in
                } ?: false // timed out -> false

                if (!becameLoggedIn) {
                    // timed out waiting for login — exit silently or log
                    println("refresh() aborted: user did not log in within 10s")
                    return@launch
                }
            }

            // at this point we're guaranteed logged in (or the condition was already true)
            try {
                // send queued messages
                appRepository.sendOfflineMessages()

                appRepository.dataSync()
            } catch (e: Exception) {
                ensureActive()
                e.printStackTrace()
            }

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
            navigator.navigate(Route.Todolist)
        }
    }
    fun onMapClick(){
        viewModelScope.launch {
            navigator.navigate(Route.UnderConstruction)
        }
    }


    //Search / Filter
    private val _searchTerm = MutableStateFlow("")
    val searchterm: StateFlow<String> = _searchTerm.asStateFlow()

    fun updateSearchterm(newValue: String) {
        _searchTerm.value = newValue
    }

    private val _filter = MutableStateFlow(ChatFilter.NONE)
    val filter: StateFlow<ChatFilter> = _filter.asStateFlow()

    fun updateFilter(newValue: ChatFilter) {
        _filter.value = newValue
    }





    @OptIn(ExperimentalCoroutinesApi::class)
    private val chatSelectorFlow: Flow<List<SelectedChat>> = combine(
        _searchTerm,
        _filter,
        SessionCache.ownId //Add own id to automatically update the chats if the own id changes (On create)
    ) { term, filter, ownid -> term to filter }
        .flatMapLatest { (term, filter) ->
            // repository should accept the filter param (you already implemented that)
            appRepository.getChatSelectorFlow(term, filter)
        }
        .flowOn(Dispatchers.Default)

    val chatSelectorState: StateFlow<List<SelectedChat>> = chatSelectorFlow
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
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
        else -> Icons.Default.Menu
    }
}
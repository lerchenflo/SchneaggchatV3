package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person3
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.groups
import schneaggchatv3mp.composeapp.generated.resources.none
import schneaggchatv3mp.composeapp.generated.resources.persons
import schneaggchatv3mp.composeapp.generated.resources.unread

class ChatSelectorViewModel(
    private val appRepository: AppRepository,
): ViewModel() {



    val globalViewModel: GlobalViewModel = KoinPlatform.getKoin().get()

    init {
        refresh()

        //TODO: Maybe try login every 5 sec if not logged in (No sync all 5 secs)
        //Chat verlassen
        //globalViewModel.onLeaveChat()
    }


    private var refreshJob: Job? = null

    fun refresh() {
        println("Refresh started")

        // if a refresh is already running, do nothing
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {

            if (!SessionCache.loggedIn){

                println("Not logged in, refreshing token")
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

            // wait up to 10 seconds for login, checking every 1 second


            // at this point we're guaranteed logged in (or the condition was already true)
            try {
                globalViewModel.viewModelScope.launch {
                    CoroutineScope(Dispatchers.IO).launch {
                        // send queued messages
                        //TODO: Messagees
                        //appRepository.sendOfflineMessages()

                        // run your sync callback
                        /*
                        appRepository.executeSync { isLoadingMessages1 ->
                            updateIsLoadingMessages(isLoadingMessages1)
                            println("Loading messages: $isLoadingMessages")
                        }
                         */

                    }.join()
                }
            } catch (e: Exception) {
                ensureActive()
                e.printStackTrace()
            }
        }
    }

    var isLoadingMessages by mutableStateOf(false)
        private set
    fun updateIsLoadingMessages(newValue: Boolean) {
        isLoadingMessages = newValue
    }


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
    private val chatSelectorFlow: Flow<List<SelectedChat>> = kotlinx.coroutines.flow.combine(
        _searchTerm,
        _filter
    ) { term, filter -> term to filter }
        .flatMapLatest { (term, filter) ->
            // repository should accept the filter param (you already implemented that)
            appRepository.getChatSelectorFlow(term, filter)
        }
        .map { list ->
            // remove yourself (own user item) and keep UI-level safety checks;
            // repository may already apply the filter, but this keeps the UI invariant.
            list
                .filter { !(it.id == SessionCache.getOwnIdValue() && !it.isGroup) }
                .sortedByDescending { it.lastmessage?.getSendDateAsLong() ?: 0L }
        }
        .flowOn(Dispatchers.Default)

    val chatSelectorState: StateFlow<List<SelectedChat>> = chatSelectorFlow
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
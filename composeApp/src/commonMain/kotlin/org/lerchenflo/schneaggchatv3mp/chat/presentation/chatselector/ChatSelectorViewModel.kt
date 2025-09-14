package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import org.lerchenflo.schneaggchatv3mp.database.AppRepository

class ChatSelectorViewModel(
    private val appRepository: AppRepository
): ViewModel() {



    val globalViewModel: GlobalViewModel = KoinPlatform.getKoin().get()

    init {
        refresh()

        //Chat verlassen
        globalViewModel.onLeaveChat()
    }


    private var refreshJob: Job? = null

    fun refresh() {
        // if a refresh is already running, do nothing
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {
            // wait up to 10 seconds for login, checking every 1 second
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

            // at this point we're guaranteed logged in (or the condition was already true)
            try {
                globalViewModel.viewModelScope.launch {
                    CoroutineScope(Dispatchers.IO).launch {
                        // send queued messages
                        appRepository.sendOfflineMessages()

                        // run your sync callback
                        appRepository.executeSync { isLoadingMessages1 ->
                            updateIsLoadingMessages(isLoadingMessages1)
                            println("Loading messages: $isLoadingMessages")
                        }
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



    @OptIn(ExperimentalCoroutinesApi::class)
    val chatSelectorFlow: Flow<List<SelectedChat>> = _searchTerm
        .flatMapLatest { term ->
            appRepository.getChatSelectorFlow(term)
        }
        .map { list ->
            list
                // remove yourself if the item is a user with your OWNID
                .filter { !(it.id == SessionCache.getOwnIdValue() && !it.isGroup) }
                // already sorted in repository, but safe to sort again
                .sortedByDescending { it.lastmessage?.getSendDateAsLong() }
        }
        .flowOn(Dispatchers.Default)

    val chatSelectorState: StateFlow<List<SelectedChat>> = chatSelectorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

}
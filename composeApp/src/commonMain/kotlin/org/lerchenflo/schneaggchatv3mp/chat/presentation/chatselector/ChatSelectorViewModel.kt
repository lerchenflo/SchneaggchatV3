package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.LOGGEDIN
import org.lerchenflo.schneaggchatv3mp.OWNID
import org.lerchenflo.schneaggchatv3mp.chat.presentation.SharedViewModel
import org.lerchenflo.schneaggchatv3mp.database.AppRepository

class ChatSelectorViewModel(
    private val appRepository: AppRepository
): ViewModel() {



    val sharedViewModel: SharedViewModel = KoinPlatform.getKoin().get()

    init {
        refresh()
    }


    fun refresh() {
        viewModelScope.launch {
            // todo wenn ma vom loginscreen kummt würrend nochrichta ned glada

            if (LOGGEDIN){
                //Ungesendete nachrichten versuacha senden
                appRepository.sendOfflineMessages()


                sharedViewModel.executeuserandmsgidsync { isLoadingMessages1 ->
                    updateIsLoadingMessages(isLoadingMessages1)
                    println("Loading messages: $isLoadingMessages")
                }

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
    val chatSelectorFlow: Flow<List<ChatSelectorItem>> = _searchTerm
        .flatMapLatest { term ->
            appRepository.getChatSelectorFlow(term)
        }
        .map { list ->
            list
                // remove yourself if the item is a user with your OWNID
                .filter { !(it.id == OWNID && !it.gruppe) }
                // already sorted in repository, but safe to sort again
                .sortedByDescending { it.lastmessage?.getSendDateAsLong() }
        }
        .flowOn(Dispatchers.Default)

    val chatSelectorState: StateFlow<List<ChatSelectorItem>> = chatSelectorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

}
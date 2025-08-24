package org.lerchenflo.schneaggchatv3mp.chat.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.mp.KoinPlatform.getKoin
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.database.tables.MessageWithReaders
import kotlin.reflect.KClass

class ChatViewModel(
    private val appRepository: AppRepository
): ViewModel() {

    val sharedViewModel: SharedViewModel = getKoin().get()


    var sendText by mutableStateOf("")
        private set
    fun updatesendText(newValue: String) {
        sendText = newValue
    }




    @OptIn(ExperimentalCoroutinesApi::class)
    val messagesFlow: Flow<List<MessageWithReaders>> =
        sharedViewModel.selectedChat
            .flatMapLatest { chat ->
                //TODO: Eig kriagt jede message an bool gruppenmessage
                appRepository.getMessagesByUserId(chat?.id ?: 0)
            }
            .map { list ->
                list.sortedByDescending { it.message.sendDate }
            }
            .flowOn(Dispatchers.Default)

    // Expose as StateFlow so UI can collect easily and get a current value
    val messagesState: StateFlow<List<MessageWithReaders>> = messagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

}
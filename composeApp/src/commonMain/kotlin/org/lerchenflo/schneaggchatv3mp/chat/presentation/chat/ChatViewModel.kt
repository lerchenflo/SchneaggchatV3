package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.network.TEXTMESSAGE
import org.lerchenflo.schneaggchatv3mp.settings.data.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisString

class ChatViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository
): ViewModel() {

    val globalViewModel: GlobalViewModel = KoinPlatform.getKoin().get()



    //TODO: Null check ob an selectegegner gwählt isch (Oder einfach id und bool gruppe übergia denn hot ma des clean grichtet und sharedviewmodel selected bruchts num)
    //Sharedviewmodel bruchama trotzdem für networktasks

    var sendText by mutableStateOf("")
        private set
    fun updatesendText(newValue: String) {
        sendText = newValue
    }

    fun sendMessage(){

        if (sendText == "") return

        //TODO: Do wechla bild und sunschwas
        var msgtype = TEXTMESSAGE
        val content = sendText
        updatesendText("")

        //Im sharedviewmodel dassas ewig leabig isch
        globalViewModel.viewModelScope.launch {
            appRepository.sendMessage(
                msgtype = msgtype,
                empfaenger = globalViewModel.selectedChat.value?.id ?: 0,
                gruppe = globalViewModel.selectedChat.value?.gruppe ?: false,
                content = content,
                answerid = -1, //TODO: Antworten
                sendedatum = getCurrentTimeMillisString()
            )
        }
    }




    @OptIn(ExperimentalCoroutinesApi::class)
    val messagesFlow: Flow<List<MessageWithReaders>> =
        globalViewModel.selectedChat
            .flatMapLatest { chat ->
                appRepository.getMessagesByUserId(chat?.id ?: 0, chat?.gruppe ?: false)
            }
            .map { list ->
                list.sortedByDescending { it.message.sendDate }
            }
            .flowOn(Dispatchers.Default)

    // Expose as StateFlow so UI can collect easily and get a current value
    val messagesState: StateFlow<List<MessageWithReaders>> = messagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun initPrefs(){
        viewModelScope.launch {
            settingsRepository.getUsemd()
                .catch { exception ->
                    println("Problem getting MD preference: ${exception.printStackTrace()}")
                }
                .collect { value ->
                    markdownEnabled = value
                }

        }
    }

    var markdownEnabled by mutableStateOf(false)
        private set

}
package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
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
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.settings.data.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisString

class ChatViewModel(
    private val appRepository: AppRepository,
    private val messageRepository: MessageRepository,
    private val settingsRepository: SettingsRepository,
    private val navigator: Navigator
): ViewModel() {

    val globalViewModel: GlobalViewModel = KoinPlatform.getKoin().get()

    var markdownEnabled by mutableStateOf(false)
        private set

    init {
        initPrefs()

        setAllMessagesRead()
    }

    fun setAllMessagesRead() {
        CoroutineScope(Dispatchers.IO).launch {
            appRepository.setAllChatMessagesRead(globalViewModel.selectedChat.value.id, globalViewModel.selectedChat.value.isGroup, getCurrentTimeMillisString())
        }
    }

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
        val content = sendText
        updatesendText("")

        //Im sharedviewmodel dassas ewig leabig isch
        globalViewModel.viewModelScope.launch {


            appRepository.sendTextMessage(
                empfaenger = globalViewModel.selectedChat.value.id,
                gruppe = globalViewModel.selectedChat.value.isGroup,
                content = content,
                answerid = null, //TODO: Antworten
            )
        }
    }


    fun onBackClick() {
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

    fun onChatDetailsClick() {
        viewModelScope.launch {
            navigator.navigate(Route.ChatDetails)
        }
    }



    @OptIn(ExperimentalCoroutinesApi::class)
    val messagesFlow: Flow<List<Message>> =
        globalViewModel.selectedChat
            .flatMapLatest { chat ->
                messageRepository.getMessagesByUserIdFlow(chat.id, chat.isGroup)
            }
            .map { list ->
                list.sortedByDescending { it.sendDate }
            }
            .flowOn(Dispatchers.Default)

    // Expose as StateFlow so UI can collect easily and get a current value
    val messagesState: StateFlow<List<Message>> = messagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
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




}
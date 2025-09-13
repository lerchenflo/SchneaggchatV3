package org.lerchenflo.schneaggchatv3mp.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.chat.domain.NotSelected
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.database.AppRepository

class GlobalViewModel(

    private val appRepository: AppRepository

    ): ViewModel() {

    init {
        print("SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT")

        //TODO: Sync alle 10 sek oda so der an login macht wenn du offline bisch und location und so
    }


    fun executeuserandmsgidsync(onLoadingStateChange: (Boolean) -> Unit){
        viewModelScope.launch {
            appRepository.executeSync(onLoadingStateChange)
        }
    }









    //aktuell ausgewählter chat
    private var _selectedChat = MutableStateFlow<SelectedChat>(NotSelected())
        private set
    val selectedChat = _selectedChat.asStateFlow()

    fun onSelectChat(chat: SelectedChat) {
        _selectedChat.update {
            chat
        }
    }

    fun onLeaveChat(){
        _selectedChat.update {
            NotSelected()
        }
    }
}
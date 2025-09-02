package org.lerchenflo.schneaggchatv3mp.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatSelectorItem

class SharedViewModel(

    private val appRepository: AppRepository,

):ViewModel() {

    init {
        print("SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT")

        //TODO: Sync alle 10 sek oda so der an login macht wenn du offline bisch und location und so
    }


    fun executeuserandmsgidsync(onLoadingStateChange: (Boolean) -> Unit){
        viewModelScope.launch {
            appRepository.executeSync(onLoadingStateChange)
        }
    }


    suspend fun areLoginCredentialsSaved(): Boolean {
        return appRepository.areLoginCredentialsSaved()
    }








    //aktuell ausgewählter chat
    private val _selectedChat = MutableStateFlow<ChatSelectorItem?>(null)
    val selectedChat = _selectedChat.asStateFlow()

    fun onSelectChat(chat: ChatSelectorItem) {
        _selectedChat.value = chat
    }

    fun onLeaveChat(){
        _selectedChat.value = null
    }
}
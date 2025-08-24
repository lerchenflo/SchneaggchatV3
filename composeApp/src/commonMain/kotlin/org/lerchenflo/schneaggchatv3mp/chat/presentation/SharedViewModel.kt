package org.lerchenflo.schneaggchatv3mp.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.LOGGEDIN
import org.lerchenflo.schneaggchatv3mp.OWNID
import org.lerchenflo.schneaggchatv3mp.SESSIONID
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.database.tables.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.database.tables.User
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.network.util.onError
import org.lerchenflo.schneaggchatv3mp.network.util.onSuccessWithBody
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager

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
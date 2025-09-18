package org.lerchenflo.schneaggchatv3mp.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmk.kmpnotifier.notification.NotifierManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.chat.domain.NotSelected
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager

class GlobalViewModel(

    private val appRepository: AppRepository,
    private val networkUtils: NetworkUtils


): ViewModel() {

    init {
        println("SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT")


        NotificationManager.initialize(networkUtils)

        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                println("Firebasetoken: ${NotificationManager.getToken()}")
            }
        }


        //TODO: Sync alle 10 sek oda so der an login macht wenn du offline bisch und location und so
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
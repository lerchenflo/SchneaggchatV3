package org.lerchenflo.schneaggchatv3mp.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.chat.domain.NotSelected
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SocketConnectionManager
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.preferences.Preferencemanager

class GlobalViewModel(
    private val appRepository: AppRepository,
    private val preferencemanager: Preferencemanager,
    private val socketConnectionManager: SocketConnectionManager
): ViewModel() {

    init {
        //println("SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT + SHAREDVIEWMODEL INIT")


        NotificationManager.initialize()

        // Sync when app is resumed
        viewModelScope.launch {
            AppLifecycleManager.appResumedEvent.collectLatest {
                if (SessionCache.isLoggedInValue()) {
                    println("App resumed and logged in, triggering sync...")
                    appRepository.dataSync()
                    appRepository.sendOfflineMessages()

                    //On resume clear all error notis
                    NotificationManager.removeNotification(NotificationManager.NotiIdType.ERROR.baseId)


                    startSocketConnection()
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                if (SessionCache.isLoggedInValue()) {
                    if (!SessionCache.isOnlineValue()) {
                        //You are logged in, but there is no connection to the server

                        //Ping server
                        appRepository.testServer() //If online will automatically set the online bool
                    }

                    if (!socketConnectionManager.isConnectedNow()) {
                        startSocketConnection()
                    }
                }

                delay(5000)
            }
        }

    }


    fun startSocketConnection() {
        viewModelScope.launch {
            println("Socketconnection connected: ${socketConnectionManager.isConnectedNow()}")
            if (!socketConnectionManager.isConnectedNow() && SessionCache.isLoggedInValue()){
                println("Connecting to socket connection -----------------------")
                val serverurl = SocketConnectionManager.getSocketUrl(preferencemanager.getServerUrl())
                println(serverurl)
                socketConnectionManager.connect(
                    serverUrl = serverurl,
                    onError = {
                        println("SOCKETCONNECTION error: " + it.message)
                    },
                    onClose = {}
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            socketConnectionManager.close()
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
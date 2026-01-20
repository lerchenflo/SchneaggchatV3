package org.lerchenflo.schneaggchatv3mp.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.chat.domain.NotSelected
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.SocketConnectionManager
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager

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

                    NotificationManager.removeNotification()

                    startSocketConnection()
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                if (SessionCache.isLoggedInValue() && !SessionCache.isOnlineValue()) {
                    //You are logged in, but there is no connection to the server

                    //Ping server
                    appRepository.testServer() //If online will automatically set the online bool

                    startSocketConnection()
                }

                delay(5000)
            }
        }

        viewModelScope.launch {
            startSocketConnection()
        }

    }


    suspend fun startSocketConnection() {
        println("Socketconnection connected: ${socketConnectionManager.isConnected()}")
        if (!socketConnectionManager.isConnected() && SessionCache.isLoggedInValue()){
            socketConnectionManager.connect(
                serverUrl = SocketConnectionManager.getSocketUrl(preferencemanager.getServerUrl()),
                onMessage = {
                    //TODO handle messages
                },
                onError = {
                    println("SOCKETCONNECTION error: " + it.message)

                },
                onClose = {}
            )
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
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
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager

class GlobalViewModel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager,
    private val socketConnectionManager: SocketConnectionManager
): ViewModel() {

    init {

        NotificationManager.initialize()

        // Sync when app is resumed
        viewModelScope.launch {
            AppLifecycleManager.appResumedEvent.collectLatest {
                println("App resumed, checking loggedin status")
                val ownId = SessionCache.requireLoggedIn()?.userId ?: return@collectLatest

                if (SessionCache.isLoggedIn()) {
                    println("App resumed and logged in, triggering sync...")
                    appRepository.sendOfflineMessages(ownId)
                    appRepository.dataSync()

                    //On resume clear all error notis
                    NotificationManager.removeNotification(NotificationManager.NotiIdType.ERROR.baseId)

                    startSocketConnection()
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                if (!SessionCache.isOnline()) {
                    appRepository.testServer(preferenceManager.getServerUrl())
                }

                if (SessionCache.isLoggedIn() && !socketConnectionManager.isConnectedNow()) {
                    startSocketConnection()
                }

                if (SessionCache.isLoggedIn() && SessionCache.isOnline()) {
                    SessionCache.requireLoggedIn()?.userId?.let {
                        appRepository.sendOfflineMessages(it)
                    }
                }

                delay(5000)
            }
        }

        viewModelScope.launch {
            AppLifecycleManager.appBackgroundedEvent.collectLatest {
                socketConnectionManager.close()
            }
        }

    }


    fun startSocketConnection() {
        viewModelScope.launch {
            if (!socketConnectionManager.isConnectedNow() && SessionCache.isLoggedIn()){
                val serverurl = SocketConnectionManager.getSocketUrl(preferenceManager.getServerUrl())
                socketConnectionManager.connect(
                    serverUrl = serverurl,
                    onError = {
                        //startSocketConnection()
                        if (!socketConnectionManager.isConnectedNow()) {
                            //SessionCache.updateOnline(false)
                        }
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
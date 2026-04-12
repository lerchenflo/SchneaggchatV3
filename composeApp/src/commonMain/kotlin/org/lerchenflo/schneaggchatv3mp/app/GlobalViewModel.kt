package org.lerchenflo.schneaggchatv3mp.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.chat.domain.NotSelected
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.toSelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.isNotSelected
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SocketConnectionManager
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalViewModel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager,
    private val socketConnectionManager: SocketConnectionManager,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository
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

                if (SessionCache.isOnline()) {
                    if (!socketConnectionManager.isConnectedNow()) {
                        startSocketConnection()
                    }

                    if (SessionCache.isLoggedIn()) {
                        SessionCache.requireLoggedIn()?.userId?.let {
                            appRepository.sendOfflineMessages(it)
                        }
                    } else {
                        AppRepository.ActionChannel.sendActionSuspend(AppRepository.ActionChannel.ActionEvent.Login)
                    }

                } else {
                    //Offline
                    appRepository.testServer(preferenceManager.getServerUrl())

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




    // Internal flow to track currently selected chat ID and type
    private data class ChatTarget(val chatId: String?, val isGroup: Boolean)
    private val _selectedChatTarget = MutableStateFlow(ChatTarget(null, false))

    // Reactive selectedChat flow that automatically updates from database
    val selectedChat = _selectedChatTarget.flatMapLatest { target ->
        if (target.chatId == null) {
            flowOf(NotSelected())
        } else {
            val isGroup = target.isGroup
            if (isGroup) {
                combine(
                    groupRepository.getGroupFlow(target.chatId),
                    messageRepository.getMessagesByUserIdFlow(target.chatId, true)
                ) { group, messages ->
                    val lastMessage = messages.maxByOrNull { it.sendDate }
                    var unreadCount = 0
                    var unsentCount = 0
                    
                    messages.forEach { message ->
                        // Only count as unread if it's NOT my message and NOT read by me
                        if (!message.myMessage && !message.readByMe) {
                            unreadCount++
                        }
                        if (!message.sent) unsentCount++
                    }
                    
                    group?.toSelectedChat(
                        unreadCount = unreadCount,
                        unsentCount = unsentCount,
                        lastMessage = lastMessage
                    ) ?: NotSelected()
                }
            } else {
                combine(
                    userRepository.getUserFlow(target.chatId),
                    messageRepository.getMessagesByUserIdFlow(target.chatId, false)
                ) { user, messages ->
                    val lastMessage = messages.maxByOrNull { it.sendDate }
                    var unreadCount = 0
                    var unsentCount = 0
                    
                    messages.forEach { message ->
                        // Only count as unread if it's NOT my message and NOT read by me
                        if (!message.myMessage && !message.readByMe) {
                            unreadCount++
                        }
                        if (!message.sent) unsentCount++
                    }
                    
                    user?.toSelectedChat(
                        unreadCount = unreadCount,
                        unsentCount = unsentCount,
                        lastMessage = lastMessage
                    ) ?: NotSelected()
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly, //Start emission directly
        initialValue = NotSelected()
    )

    suspend fun onSelectChat(chat: SelectedChat) {
        _selectedChatTarget.value = ChatTarget(chat.id, chat.isGroup)

        //Await selectedchat emission to not leave chat directly
        //selectedChat.first { !it.isNotSelected() }
    }

    fun onLeaveChat(){
        _selectedChatTarget.value = ChatTarget(null, false)
    }
}
@file:OptIn(FlowPreview::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.datasource.domain.Logger
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListRepository
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.AvailableUser
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.datasource.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.copied_to_clipboard
import schneaggchatv3mp.composeapp.generated.resources.error_friend_request
import schneaggchatv3mp.composeapp.generated.resources.friend_request_sent
import schneaggchatv3mp.composeapp.generated.resources.invitation_text
import kotlin.time.Duration.Companion.milliseconds

class NewChatViewModel (
    private val appRepository: AppRepository,
    private val chatListRepository: ChatListRepository,
    private val userRepository: UserRepository,
    private val navigator: Navigator,
    private val loggingRepository: Logger,
    private val shareUtils: ShareUtils,
    private val settingsRepository: SettingsRepository
): ViewModel() {

    private val _searchTerm = MutableStateFlow("")
    val searchterm: StateFlow<String> = _searchTerm.asStateFlow()

    fun updateSearchterm(newValue: String) {
        _searchTerm.value = newValue
    }

    private val _availableChats = MutableStateFlow<List<AvailableUser>>(emptyList())
    val availableChats: StateFlow<List<AvailableUser>> = _availableChats.asStateFlow()


    private val _pendingFriends = MutableStateFlow<List<ChatListItem>>(emptyList())
    val pendingFriends: StateFlow<List<ChatListItem>> = _pendingFriends.asStateFlow()

    init {
        //On start remove all friend request notis
        NotificationManager.removeNotification(NotificationManager.NotiIdType.FRIEND_REQUEST.baseId)

        // Kombiniere searchTerm mit einem Flow der die Daten lädt
        viewModelScope.launch {
            searchterm
                .debounce(300.milliseconds) // Warte 300ms nach letzter Eingabe
                .distinctUntilChanged() // Nur wenn sich der Wert ändert
                .collectLatest { term ->
                    loadAvailableUsers(term)
                }
        }

        viewModelScope.launch {
            searchterm
                .collectLatest {
                    chatListRepository.getPendingFriends(it).collectLatest { pendingfriendsList ->
                        _pendingFriends.value = pendingfriendsList
                    }
                }
        }
    }

    private suspend fun loadAvailableUsers(searchTerm: String) {
        try {
            val response = userRepository.getAvailableUsers(searchTerm)
            println("reloading users: $response")
            _availableChats.value = response
                .sortedByDescending { it.commonFriendCount }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            // Handle error
            loggingRepository.logWarning("NewChat loadAvailableUsers failed: ${e.message}")
            _availableChats.value = emptyList()
        }
    }

    fun addFriend(friendId: String) {
        CoroutineScope(Dispatchers.IO).launch { //Launch in coroutinescope to not access the db on main thread
            val success = userRepository.sendFriendRequest(friendId)
            SnackbarManager.showMessage(
                if (success) getString(Res.string.friend_request_sent) else getString(Res.string.error_friend_request)
            )
            loadAvailableUsers(searchterm.value)
        }
    }






    //Friend accept / Deny
    private val _pendingFriendPopup = MutableStateFlow<ChatListItem?>(null)
    val pendingFriendPopup: StateFlow<ChatListItem?> = _pendingFriendPopup.asStateFlow()

    fun dismissPendingFriendDialog() {
        _pendingFriendPopup.value = null
    }

    fun acceptFriend(friendId: String){
        viewModelScope.launch {
            userRepository.sendFriendRequest(friendId)
        }
        dismissPendingFriendDialog()
    }

    fun denyFriend(friendId: String){
        viewModelScope.launch {
            userRepository.denyFriendRequest(friendId)
        }
        dismissPendingFriendDialog()
    }

    fun onPendingFriendRequestClick(selectedChat: ChatListItem) {
        if (selectedChat.friendshipStatus != FriendshipStatus.PENDING) {
            //You are already friends with this person, open the chat
            viewModelScope.launch {
                navigator.navigate(Route.Chat(chatId = selectedChat.id, isGroup = selectedChat.isGroup), navigationOptions = Navigator.NavigationOptions(removeAllScreensByClass = listOf(Route.NewChat::class)))
            }
        }else {
            //Friendshipstatus pending
            _pendingFriendPopup.value = selectedChat
        }
    }




    fun onBackClick() {
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

    fun onGroupCreatorClick() {
        viewModelScope.launch {
            navigator.navigate(Route.GroupCreator)
        }
    }

    fun onInviteFriendClick() {

        viewModelScope.launch {
            val serverUrl = settingsRepository.getServerUrl()
            shareUtils.shareString(getString(Res.string.invitation_text) + "\n$serverUrl")

            if (appRepository.appVersion.isDesktop()) {
                SnackbarManager.showMessage(getString(Res.string.copied_to_clipboard))
            }
        }
    }


}


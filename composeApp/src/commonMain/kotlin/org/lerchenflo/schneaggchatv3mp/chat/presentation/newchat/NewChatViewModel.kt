@file:OptIn(FlowPreview::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.copied_to_clipboard
import schneaggchatv3mp.composeapp.generated.resources.error_friend_request
import schneaggchatv3mp.composeapp.generated.resources.friend_request_sent
import schneaggchatv3mp.composeapp.generated.resources.invitation_text

class NewChatViewModel (
    private val appRepository: AppRepository,
    private val navigator: Navigator,
    private val loggingRepository: LoggingRepository,
    private val shareUtils: ShareUtils
): ViewModel() {

    private val _searchTerm = MutableStateFlow("")
    val searchterm: StateFlow<String> = _searchTerm.asStateFlow()

    fun updateSearchterm(newValue: String) {
        _searchTerm.value = newValue
    }

    private val _availableChats = MutableStateFlow<List<NetworkUtils.NewFriendsUserResponse>>(emptyList())
    val availableChats: StateFlow<List<NetworkUtils.NewFriendsUserResponse>> = _availableChats.asStateFlow()


    private val _pendingFriends = MutableStateFlow<List<SelectedChat>>(emptyList())
    val pendingFriends: StateFlow<List<SelectedChat>> = _pendingFriends.asStateFlow()

    init {
        //On start remove all friend request notis
        NotificationManager.removeNotification(NotificationManager.NotiIdType.FRIEND_REQUEST.baseId)

        // Kombiniere searchTerm mit einem Flow der die Daten lädt
        viewModelScope.launch {
            searchterm
                .debounce(300) // Warte 300ms nach letzter Eingabe
                .distinctUntilChanged() // Nur wenn sich der Wert ändert
                .collectLatest { term ->
                    loadAvailableUsers(term)
                }
        }

        viewModelScope.launch {
            searchterm
                .collectLatest {
                    appRepository.getPendingFriends(it).collectLatest { pendingfriendsList ->
                        _pendingFriends.value = pendingfriendsList
                    }
                }
        }
    }

    private suspend fun loadAvailableUsers(searchTerm: String) {
        try {
            val response = appRepository.getAvailableUsers(searchTerm)
            println("reloading users: $response")
            _availableChats.value = response
                .sortedByDescending { it.commonFriendCount }
        } catch (e: Exception) {
            // Handle error
            loggingRepository.logWarning("NewChat loadAvailableUsers failed: ${e.message}")
            _availableChats.value = emptyList()
        }
    }

    fun addFriend(friendId: String) {
        CoroutineScope(Dispatchers.IO).launch { //Launch in coroutinescope to not access the db on main thread
            val success = appRepository.sendFriendRequest(friendId)
            SnackbarManager.showMessage(
                if (success) getString(Res.string.friend_request_sent) else getString(Res.string.error_friend_request)
            )
            loadAvailableUsers(searchterm.value)
        }
    }






    //Friend accept / Deny
    private val _pendingFriendPopup = MutableStateFlow<SelectedChat?>(null)
    val pendingFriendPopup: StateFlow<SelectedChat?> = _pendingFriendPopup.asStateFlow()

    fun dismissPendingFriendDialog() {
        _pendingFriendPopup.value = null
    }

    fun acceptFriend(friendId: String){
        viewModelScope.launch {
            appRepository.sendFriendRequest(friendId)
        }
        dismissPendingFriendDialog()
    }

    fun denyFriend(friendId: String){
        viewModelScope.launch {
            appRepository.denyFriendRequest(friendId)
        }
        dismissPendingFriendDialog()
    }

    fun onPendingFriendRequestClick(selectedChat: SelectedChat) {
        //You are friends with this person, open chat
        if (selectedChat.friendshipStatus != NetworkUtils.FriendshipStatus.PENDING) {

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
            shareUtils.shareString(getString(Res.string.invitation_text) + "\n$BASE_SERVER_URL")

            if (appRepository.appVersion.isDesktop()) {
                SnackbarManager.showMessage(getString(Res.string.copied_to_clipboard))
            }
        }
    }


}


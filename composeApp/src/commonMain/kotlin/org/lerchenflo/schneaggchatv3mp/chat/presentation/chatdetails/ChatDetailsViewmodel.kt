@file:OptIn(ExperimentalCoroutinesApi::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.domain.models.GalleryPhotoResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.MAX_GROUPNAME_LENGTH
import org.lerchenflo.schneaggchatv3mp.MIN_GROUPNAME_LENGTH
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.toChatListItem
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.ErrorMessage
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_friend_request
import schneaggchatv3mp.composeapp.generated.resources.friend_request_sent
import schneaggchatv3mp.composeapp.generated.resources.please_restart_app


data class GroupMemberWithUser(
    val groupMember: GroupMember,
    val user: User?
)

/**
 * Screen state for the chat details screen. Either the enriched user details or the enriched
 * group details, depending on what chat the screen was opened for.
 */
sealed interface ChatDetailsState {
    val name: String
    val profilePictureUrl: String
    val description: String?
    val status: String?

    data object Loading : ChatDetailsState {
        override val name: String = ""
        override val profilePictureUrl: String = ""
        override val description: String? = null
        override val status: String? = null
    }

    data class UserDetails(
        val user: User,
        val commonGroups: List<Group> = emptyList()
    ) : ChatDetailsState {
        override val name: String get() = user.name
        override val profilePictureUrl: String get() = user.profilePictureUrl
        override val description: String? get() = user.description
        override val status: String? get() = user.status
    }

    data class GroupDetails(
        val group: Group,
        val members: List<GroupMemberWithUser> = emptyList()
    ) : ChatDetailsState {
        override val name: String get() = group.name
        override val profilePictureUrl: String get() = group.profilePictureUrl
        override val description: String get() = group.description
        override val status: String? get() = null
    }
}

class ChatDetailsViewmodel(
    val chatId: String,
    val isGroup: Boolean,
    private val groupRepository: GroupRepository,
    private val navigator: Navigator,
    private val userRepository: UserRepository,
    private val appRepository: AppRepository,
    private val pictureManager: PictureManager,
) : ViewModel() {


    var selectedNewMembers by mutableStateOf<List<ChatListItem>>(emptyList())
    private val _searchTerm = MutableStateFlow("")
    val searchterm = _searchTerm.asStateFlow()

    /** Friends that can still be added to this group (groups only, empty for user chats). */
    val availableNewMembers: StateFlow<List<ChatListItem>> = if (!isGroup) {
        MutableStateFlow<List<ChatListItem>>(emptyList())
    } else {
        combine(
            _searchTerm.flatMapLatest { term -> appRepository.getFriendsFlow(term) },
            groupRepository.getGroupFlow(chatId)
        ) { friends, group ->
            val currentMemberIds = group?.members?.map { it.userId }?.toSet() ?: emptySet()

            // Only show friends who are NOT already in the group
            friends
                .filter { friend -> friend.id !in currentMemberIds }
                .map { it.toChatListItem() }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }


    fun onSearchTermChange(newValue: String) {
        _searchTerm.value = newValue
    }

    fun onUserSelected(user: ChatListItem){
        selectedNewMembers = selectedNewMembers + user
    }
    fun onUserDeSelected(user: ChatListItem){
        selectedNewMembers = selectedNewMembers - user
    }


    fun onBackClick(){
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

    var descriptionText by mutableStateOf(TextFieldValue(""))
        private set
    fun updateDescriptionText(newValue: TextFieldValue) {
        descriptionText = newValue
    }


    /**
     * Enriched chat details, kept up to date from the database:
     * - For groups: group with resolved members
     * - For users: user with common groups
     */
    val chatDetails: StateFlow<ChatDetailsState> = if (isGroup) {
        groupRepository.getGroupFlow(chatId).map { group ->
            if (group == null) {
                ChatDetailsState.Loading
            } else {
                ChatDetailsState.GroupDetails(
                    group = group,
                    members = getGroupMembersWithUsers(chatId)
                )
            }
        }
    } else {
        userRepository.getUserFlow(chatId).map { user ->
            if (user == null) {
                ChatDetailsState.Loading
            } else {
                val ownId = SessionCache.requireLoggedIn()?.userId
                val commonGroups = ownId?.let {
                    groupRepository.getCommonGroups(
                        ownId = it,
                        otherUserId = chatId
                    )
                } ?: emptyList()

                ChatDetailsState.UserDetails(
                    user = user,
                    commonGroups = commonGroups
                )
            }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatDetailsState.Loading
        )


    private suspend fun getGroupMembersWithUsers(groupId: String): List<GroupMemberWithUser> {
        val members = groupRepository.getGroupMembers(groupId)
        return members.map { member ->
            val user = userRepository.getUserById(member.userId)
            if (user != null) {
                GroupMemberWithUser(member, user)
            } else {
                // todo was tuat ma mit lüt in ana gruppe wo ned in da userdatenbank sind?
                GroupMemberWithUser(member, null)
            }
        }
    }

    fun navigateToChat(chatId: String, isGroup: Boolean){
        viewModelScope.launch {
            navigator.navigate(
                Route.Chat(chatId = chatId, isGroup = isGroup),
                navigationOptions = Navigator.NavigationOptions(exitPreviousScreen = true)
            )
        }
    }

    fun updateDescription(){
        viewModelScope.launch {
            if (isGroup) {
                appRepository.changeGroupDescription(chatId, descriptionText.text)
            }else {
                appRepository.changeUserDetails(newStatus = null, newDescription =  descriptionText.text, userId = chatId)
            }
        }
    }

    fun removeFriend() {
        if (!isGroup) {
            viewModelScope.launch {
                if(appRepository.removeFriend(chatId)){
                    navigator.navigate(Route.ChatSelector, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
                }
            }
        }
    }

    fun changeAdminStatus(member: GroupMember){
        viewModelScope.launch{
            appRepository.changeGroupMembers(
                action = if(member.admin) NetworkUtils.GroupMemberAction.REMOVE_ADMIN else NetworkUtils.GroupMemberAction.MAKE_ADMIN,
                memberId = member.userId,
                groupId = member.groupId
            )
        }
    }

    fun updateProfilePic (profilePicUri: GalleryPhotoResult) {
        viewModelScope.launch {
            val bytearray = profilePicUri
                .loadBytes()

            //println("Unscaled image size: ${bytearray.size}")

            val downscaledimage = pictureManager.downscaleImage(bytearray)

            //println("Downscaled image size: ${downscaledimage.size}")
            val success = appRepository.changeGroupProfilePic(
                groupId = chatId,
                newPic = downscaledimage
            )

            if (success) {
                SnackbarManager.showMessage(getString(Res.string.please_restart_app)) //Des image caching vo coil isch so guat dassas sich ned abtöta loht. ma künnt profilbild mit datum speichra aber denn wirds ned überschrieba und unendlich speicherverbrauch
            }
        }
    }

    fun addMember(userId: String){
        viewModelScope.launch {
            appRepository.changeGroupMembers(
                action = NetworkUtils.GroupMemberAction.ADD_USER,
                memberId = userId,
                groupId = chatId
            )
        }
    }

    fun removeMember(memberId: String){
        viewModelScope.launch {
            appRepository.changeGroupMembers(
                action = NetworkUtils.GroupMemberAction.REMOVE_USER,
                memberId = memberId,
                groupId = chatId
            )
        }
    }

    fun validateGroupName(name: String): ErrorMessage? {
        return if (name.length > MAX_GROUPNAME_LENGTH) ErrorMessage.NAME_TO_LONG else if (name.length < MIN_GROUPNAME_LENGTH) ErrorMessage.NAME_TO_SHORT else null
    }

    fun updateGroupName(name: String){
        viewModelScope.launch {
            appRepository.changeGroupName(chatId, name)
        }
    }

    fun sendFriendRequest(id: String){
        CoroutineScope(Dispatchers.IO).launch { //Launch in coroutinescope to not access the db on main thread
            val success = appRepository.sendFriendRequest(id)
            SnackbarManager.showMessage(
                if (success) getString(Res.string.friend_request_sent) else getString(Res.string.error_friend_request)
            )
        }
    }

    fun navigateChatSelExitAllPrevious(){
        viewModelScope.launch {
            navigator.navigate(Route.ChatSelector, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
        }
    }


    fun updateNickname(newNickname: String) {
        viewModelScope.launch {
            appRepository.changeUserDetails(
                userId = chatId,
                newNickName = newNickname
            )
        }
    }


}

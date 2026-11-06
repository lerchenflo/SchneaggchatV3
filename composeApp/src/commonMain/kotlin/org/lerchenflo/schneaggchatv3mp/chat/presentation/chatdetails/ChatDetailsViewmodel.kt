@file:OptIn(ExperimentalCoroutinesApi::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.runtime.getValue
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.isSuccess
import org.lerchenflo.schneaggchatv3mp.app.domain.onSuccess
import org.lerchenflo.schneaggchatv3mp.app.domain.reportErrorTo
import org.lerchenflo.schneaggchatv3mp.app.domain.sendError
import org.lerchenflo.schneaggchatv3mp.app.launchOnMain
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.sharedcontent.SharedContentTab
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ismoy.imagepickerkmp.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.picker.GalleryPhotoResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.MAX_GROUPNAME_LENGTH
import org.lerchenflo.schneaggchatv3mp.MIN_GROUPNAME_LENGTH
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMemberAction
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.toChatListItem
import org.lerchenflo.schneaggchatv3mp.events.domain.EventRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.WakeOutcome
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.ExtensionListAction
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.ErrorMessage
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import org.lerchenflo.schneaggchatv3mp.utilities.extractLinks
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.friend_request_sent
import schneaggchatv3mp.composeapp.generated.resources.please_restart_app
import schneaggchatv3mp.composeapp.generated.resources.wake_no_devices
import schneaggchatv3mp.composeapp.generated.resources.wake_not_allowed_group
import schneaggchatv3mp.composeapp.generated.resources.wake_not_allowed_user
import schneaggchatv3mp.composeapp.generated.resources.wake_not_friends
import schneaggchatv3mp.composeapp.generated.resources.wake_sent_skipped
import schneaggchatv3mp.composeapp.generated.resources.wake_sent_user
import schneaggchatv3mp.composeapp.generated.resources.wake_sent_users
import schneaggchatv3mp.composeapp.generated.resources.network_error_no_internet
import schneaggchatv3mp.composeapp.generated.resources.network_error_timeout
import schneaggchatv3mp.composeapp.generated.resources.wake_failed


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
    private val errorChannel: ErrorChannel,
    private val groupRepository: GroupRepository,
    private val navigator: Navigator,
    private val userRepository: UserRepository,
    private val pictureManager: PictureManager,
    private val eventRepository: EventRepository,
    private val messageRepository: MessageRepository,
    private val groupExtensionRepository: GroupExtensionRepository,
    private val applicationScope: ApplicationScope,

    val chatId: String,
    val isGroup: Boolean,
) : ViewModel() {


    var selectedNewMembers by mutableStateOf<List<ChatListItem>>(emptyList())
    private val _searchTerm = MutableStateFlow("")
    val searchterm = _searchTerm.asStateFlow()

    /** All groups the user is in, kept around to expand a selected group into its member friends. */
    private val allGroupsWithMembers: StateFlow<List<Group>> = groupRepository.getAllGroupsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Friends and other groups that can be picked to add to this group (groups only, empty for
     * user chats). Picking a group is handled in [onUserSelected] - it expands into that group's
     * member friends rather than being added itself.
     */
    val availableNewMembers: StateFlow<List<ChatListItem>> = if (!isGroup) {
        MutableStateFlow<List<ChatListItem>>(emptyList())
    } else {
        combine(
            _searchTerm.flatMapLatest { term -> userRepository.getFriendsFlow(term) },
            groupRepository.getGroupByIdFlow(chatId),
            allGroupsWithMembers,
            _searchTerm
        ) { friends, group, allGroups, term ->
            val currentMemberIds = group?.members?.map { it.userId }?.toSet() ?: emptySet()

            // Only show friends who are NOT already in the group
            val availableFriends = friends
                .filter { friend -> friend.id !in currentMemberIds }
                .map { it.toChatListItem() }

            // Other groups, picking one adds all its member friends
            val availableGroups = allGroups
                .filter { it.id != chatId }
                .filter { it.name.contains(term, ignoreCase = true) }
                .map { it.toChatListItem() }

            availableGroups + availableFriends
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }


    /**
     * Ask the server to wake this chat's user or group. The server filters out everyone who has
     * not allowed us, so the outcome is reported back rather than silently swallowed - otherwise
     * a wake nobody permitted would look identical to one that worked.
     */
    fun sendWake(reason: String) {
        applicationScope.launchOnMain {
            val chatName = chatDetails.value.name
            val result = userRepository.sendWake(
                targetId = chatId,
                isGroup = isGroup,
                reason = reason
            )

            val response = when (result) {
                is DataResult.Success -> result.data
                is DataResult.Error -> {
                    //The server rejects waking a non-friend outright rather than reporting it
                    //as an outcome, so it arrives here as a 403.
                    errorChannel.sendError(
                        UiText.StringResourceText(
                            when (result.error) {
                                is DataError.Forbidden -> Res.string.wake_not_friends
                                is DataError.NoInternetConnection -> Res.string.network_error_no_internet
                                is DataError.NetworkTimeout -> Res.string.network_error_timeout
                                else -> Res.string.wake_failed
                            }
                        )
                    )
                    return@launchOnMain
                }
            }

            when (response.outcome) {
                WakeOutcome.WOKEN -> {
                    val base = if (isGroup) {
                        getString(Res.string.wake_sent_users, response.userCount, response.deviceCount)
                    } else {
                        getString(Res.string.wake_sent_user, chatName, response.deviceCount)
                    }
                    SnackbarManager.showMessage(
                        if (response.skippedCount > 0) {
                            getString(Res.string.wake_sent_skipped, base, response.skippedCount)
                        } else {
                            base
                        }
                    )
                }

                WakeOutcome.NOT_ALLOWED -> errorChannel.sendError(
                    if (isGroup) UiText.StringResourceText(Res.string.wake_not_allowed_group)
                    else UiText.StringResourceText(Res.string.wake_not_allowed_user, arrayOf(chatName))
                )

                WakeOutcome.NO_DEVICES -> errorChannel.sendError(
                    UiText.StringResourceText(Res.string.wake_no_devices, arrayOf(chatName))
                )
            }
        }
    }

    fun onSearchTermChange(newValue: String) {
        _searchTerm.value = newValue
    }

    private fun groupMemberIds(groupId: String): Set<String> =
        allGroupsWithMembers.value.find { it.id == groupId }?.members?.map { it.userId }?.toSet() ?: emptySet()

    fun onUserSelected(user: ChatListItem){
        if (user.isGroup) {
            // Picking a group adds all its member friends instead of the group itself
            val memberIds = groupMemberIds(user.id)
            val friendsInGroup = availableNewMembers.value.filter { !it.isGroup && it.id in memberIds }
            selectedNewMembers = (selectedNewMembers + friendsInGroup).toSet().toList()
        } else {
            selectedNewMembers = selectedNewMembers + user
        }
    }
    fun onUserDeSelected(user: ChatListItem){
        if (user.isGroup) {
            val memberIds = groupMemberIds(user.id)
            selectedNewMembers = selectedNewMembers.filterNot { it.id in memberIds }
        } else {
            selectedNewMembers = selectedNewMembers - user
        }
    }

    /** True for a friend already picked, or a group whose member friends are all already picked. */
    fun isItemSelected(item: ChatListItem): Boolean {
        return if (item.isGroup) {
            val memberIds = groupMemberIds(item.id)
            val relevantIds = availableNewMembers.value.filter { !it.isGroup && it.id in memberIds }.map { it.id }
            relevantIds.isNotEmpty() && relevantIds.all { id -> selectedNewMembers.any { it.id == id } }
        } else {
            selectedNewMembers.any { it.id == item.id }
        }
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
        groupRepository.getGroupByIdFlow(chatId).map { group ->
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
        userRepository.getUserByIdFlow(chatId).map { user ->
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


    /** The event this group was created for, if any (groups only, empty for user chats). */
    val connectedEvent: StateFlow<Event?> = if (!isGroup) {
        MutableStateFlow(null)
    } else {
        eventRepository.getEventFlowForGroup(chatId)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun navigateToConnectedEvent(eventId: String) {
        viewModelScope.launch {
            navigator.navigate(Route.Events(selectedEventId = eventId))
        }
    }

    /**
     * How many images and how many links this chat has shared, for the two rows that open the
     * shared content screen. Counted here rather than in SQL because a link only counts once the
     * regex has actually found one in the message text - `content LIKE '%http%'` alone would also
     * match a message that just happens to contain the word.
     */
    val sharedImageCount: StateFlow<Int> = messageRepository
        .getImageMessagesForChatFlow(chatId, isGroup)
        .map { messages -> messages.count { !it.pictureUrl.isNullOrEmpty() } }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val sharedLinkCount: StateFlow<Int> = messageRepository
        .getLinkCandidateMessagesForChatFlow(chatId, isGroup)
        .map { messages -> messages.sumOf { extractLinks(it.content).size } }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val messageCount: StateFlow<Int> = messageRepository
        .getMessageCountForChatFlow(chatId, isGroup)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun navigateToSharedContent(initialTab: SharedContentTab) {
        viewModelScope.launch {
            navigator.navigate(
                Route.ChatSharedContent(chatId = chatId, isGroup = isGroup, initialTab = initialTab)
            )
        }
    }

    fun navigateToBirthdays() {
        viewModelScope.launch {
            navigator.navigate(Route.Birthdays)
        }
    }

    fun detachConnectedEvent(eventId: String, deleteGroup: Boolean, deleteEvent: Boolean) {
        applicationScope.launchOnMain {
            eventRepository.detachEvent(
                eventId = eventId,
                groupId = chatId,
                deleteGroup = deleteGroup,
                deleteEvent = deleteEvent,
            ).reportErrorTo(errorChannel)
        }
    }

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

    /** Extensions of this group, only groups own extensions for now. */
    val groupExtensions: StateFlow<List<GroupExtension>> = (
        if (isGroup) groupExtensionRepository.getExtensionsForOwnerFlow(chatId)
        else flowOf(emptyList())
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onExtensionAction(action: ExtensionListAction) {
        when (action) {
            is ExtensionListAction.OnExtensionClick -> {
                viewModelScope.launch {
                    navigator.navigate(Route.GroupExtension(extensionId = action.extensionId))
                }
            }
            is ExtensionListAction.OnCreateExtension -> {
                applicationScope.launchOnMain {
                    groupExtensionRepository.createExtension(
                        groupId = chatId,
                        type = action.type,
                        title = action.title,
                        settings = action.settings,
                    ).reportErrorTo(errorChannel).onSuccess { extensionId ->
                        // Don't pull the user back here when the screen was left meanwhile
                        if (viewModelScope.isActive) {
                            navigator.navigate(Route.GroupExtension(extensionId = extensionId))
                        }
                    }
                }
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
        applicationScope.launchOnMain {
            if (isGroup) {
                groupRepository.changeGroupDescription(chatId, descriptionText.text).reportErrorTo(errorChannel)
            }else {
                userRepository.changeUserDetails(newStatus = null, newDescription =  descriptionText.text, userId = chatId).reportErrorTo(errorChannel)
            }
        }
    }

    fun removeFriend() {
        if (!isGroup) {
            applicationScope.launchOnMain {
                if (userRepository.removeFriend(chatId).reportErrorTo(errorChannel).isSuccess) {
                    navigator.navigate(Route.ChatSelector, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
                }
            }
        }
    }

    fun changeAdminStatus(member: GroupMember){
        applicationScope.launchOnMain{
            groupRepository.changeGroupMembers(
                action = if(member.admin) GroupMemberAction.REMOVE_ADMIN else GroupMemberAction.MAKE_ADMIN,
                memberId = member.userId,
                groupId = member.groupId
            ).reportErrorTo(errorChannel)
        }
    }

    fun updateProfilePic (profilePicUri: GalleryPhotoResult) {
        applicationScope.launchOnMain {
            val bytearray = profilePicUri
                .loadBytes()

            //println("Unscaled image size: ${bytearray.size}")

            val downscaledimage = pictureManager.downscaleImage(bytearray)

            //println("Downscaled image size: ${downscaledimage.size}")
            val success = groupRepository.changeGroupProfilePic(
                groupId = chatId,
                newPic = downscaledimage
            ).reportErrorTo(errorChannel).isSuccess

            if (success) {
                SnackbarManager.showMessage(getString(Res.string.please_restart_app)) //Des image caching vo coil isch so guat dassas sich ned abtöta loht. ma künnt profilbild mit datum speichra aber denn wirds ned überschrieba und unendlich speicherverbrauch
            }
        }
    }

    fun addMember(userId: String){
        applicationScope.launchOnMain {
            groupRepository.changeGroupMembers(
                action = GroupMemberAction.ADD_USER,
                memberId = userId,
                groupId = chatId
            ).reportErrorTo(errorChannel)
        }
    }

    fun removeMember(memberId: String){
        applicationScope.launchOnMain {
            groupRepository.changeGroupMembers(
                action = GroupMemberAction.REMOVE_USER,
                memberId = memberId,
                groupId = chatId
            ).reportErrorTo(errorChannel)
        }
    }

    fun validateGroupName(name: String): ErrorMessage? {
        return if (name.length > MAX_GROUPNAME_LENGTH) ErrorMessage.NAME_TO_LONG else if (name.length < MIN_GROUPNAME_LENGTH) ErrorMessage.NAME_TO_SHORT else null
    }

    fun updateGroupName(name: String){
        applicationScope.launchOnMain {
            groupRepository.changeGroupName(chatId, name).reportErrorTo(errorChannel)
        }
    }

    /**
     * Set, move or clear (null) the group's delete timer. Admins only - the server rejects anyone
     * else, and the UI only offers it to admins.
     */
    fun setGroupExpiry(expiresAt: Long?) {
        applicationScope.launchOnMain {
            groupRepository.changeGroupExpiry(chatId, expiresAt).reportErrorTo(errorChannel)
        }
    }

    /**
     * Delete the whole group for every member. Admins only - the server rejects anyone else, and
     * the UI only offers it to admins.
     */
    fun deleteGroup() {
        applicationScope.launchOnMain {
            if (groupRepository.deleteGroup(chatId).reportErrorTo(errorChannel).isSuccess) {
                navigator.navigate(Route.ChatSelector, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
            }
        }
    }

    fun sendFriendRequest(id: String){
        applicationScope.launchOnMain { //The repository is main-safe
            userRepository.sendFriendRequest(id).reportErrorTo(errorChannel).onSuccess {
                SnackbarManager.showMessage(getString(Res.string.friend_request_sent))
            }
        }
    }

    fun navigateChatSelExitAllPrevious(){
        viewModelScope.launch {
            navigator.navigate(Route.ChatSelector, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
        }
    }


    fun updateNickname(newNickname: String) {
        applicationScope.launchOnMain {
            userRepository.changeUserDetails(
                userId = chatId,
                newNickName = newNickname
            ).reportErrorTo(errorChannel)
        }
    }


}

@file:OptIn(ExperimentalCoroutinesApi::class)

package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ismoy.imagepickerkmp.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.picker.GalleryPhotoResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.isSuccess
import org.lerchenflo.schneaggchatv3mp.app.domain.onSuccess
import org.lerchenflo.schneaggchatv3mp.app.domain.reportErrorTo
import org.lerchenflo.schneaggchatv3mp.app.domain.sendError
import org.lerchenflo.schneaggchatv3mp.app.launchOnMain
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMemberAction
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentity
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityName
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.WakeOutcome
import org.lerchenflo.schneaggchatv3mp.chat.domain.toChatListItem
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.events.domain.EventRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.statusOf
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.ExtensionListAction
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.utilities.format.extractLinks
import org.lerchenflo.schneaggchatv3mp.utilities.format.toStringResource
import org.lerchenflo.schneaggchatv3mp.utilities.format.validateName
import org.lerchenflo.schneaggchatv3mp.utilities.media.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.ui.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.ui.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.event_friends_invited
import schneaggchatv3mp.composeapp.generated.resources.network_error_no_internet
import schneaggchatv3mp.composeapp.generated.resources.network_error_timeout
import schneaggchatv3mp.composeapp.generated.resources.please_restart_app
import schneaggchatv3mp.composeapp.generated.resources.wake_failed
import schneaggchatv3mp.composeapp.generated.resources.wake_no_devices
import schneaggchatv3mp.composeapp.generated.resources.wake_not_allowed_group
import schneaggchatv3mp.composeapp.generated.resources.wake_not_allowed_user
import schneaggchatv3mp.composeapp.generated.resources.wake_not_friends
import schneaggchatv3mp.composeapp.generated.resources.wake_sent_skipped
import schneaggchatv3mp.composeapp.generated.resources.wake_sent_user
import schneaggchatv3mp.composeapp.generated.resources.wake_sent_users
import kotlin.time.Clock

/**
 * Backs the details of one user or group chat: keeps [state] up to date from the database and
 * handles every [ChatDetailsAction]. Server changes run in the [ApplicationScope], so they finish
 * even when the screen is left right after the tap.
 */
class ChatDetailsViewModel(
    private val errorChannel: ErrorChannel,
    private val groupRepository: GroupRepository,
    private val navigator: Navigator,
    private val userRepository: UserRepository,
    private val userIdentityRepository: UserIdentityRepository,
    private val pictureManager: PictureManager,
    private val eventRepository: EventRepository,
    private val messageRepository: MessageRepository,
    private val groupExtensionRepository: GroupExtensionRepository,
    private val settingsRepository: SettingsRepository,
    private val applicationScope: ApplicationScope,

    private val chatId: String,
    private val isGroup: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatDetailsState(isGroup = isGroup))
    val state = _state.asStateFlow()

    private val ownIdFlow: Flow<String?> = SessionCache.authState
        .map { (it as? SessionCache.AuthState.LoggedIn)?.userId }
        .distinctUntilChanged()

    init {
        ownIdFlow.onEach { ownId -> _state.update { it.copy(ownId = ownId) } }.launchIn(viewModelScope)
        chatFlow().onEach { chat -> _state.update { it.copy(chat = chat) } }.launchIn(viewModelScope)
        observeCounts()
        observeMapPreview()
        if (isGroup) observeGroupOnly()
    }

    fun onAction(action: ChatDetailsAction) {
        when (action) {
            ChatDetailsAction.OnBackClick -> navigate { navigator.navigateBack() }
            ChatDetailsAction.OnDialogDismiss -> closeDialog()

            ChatDetailsAction.OnGroupSettingsClick -> openDialog(ChatDetailsDialog.GroupSettings)
            ChatDetailsAction.OnNicknameClick -> openDialog(ChatDetailsDialog.Nickname)
            is ChatDetailsAction.OnAddExtensionClick -> openDialog(ChatDetailsDialog.AddExtension(action.type))
            ChatDetailsAction.OnSetGroupTimerClick -> openDialog(ChatDetailsDialog.ExpiryPicker)
            ChatDetailsAction.OnRemoveGroupTimerClick -> openDialog(ChatDetailsDialog.RemoveExpiry)
            ChatDetailsAction.OnWakeClick -> openDialog(ChatDetailsDialog.WakeReason)
            ChatDetailsAction.OnLeaveGroupClick -> openDialog(ChatDetailsDialog.LeaveGroup)
            ChatDetailsAction.OnDeleteGroupClick -> openDialog(ChatDetailsDialog.DeleteGroup)
            ChatDetailsAction.OnDeleteEventClick -> openDialog(ChatDetailsDialog.DeleteEvent)
            ChatDetailsAction.OnRemoveFriendClick -> openDialog(ChatDetailsDialog.RemoveFriend)

            ChatDetailsAction.OnEditPictureClick -> openDialog(ChatDetailsDialog.PicturePicker)
            is ChatDetailsAction.OnPicturePicked -> {
                closeDialog()
                updateGroupPicture(action.photo)
            }
            ChatDetailsAction.OnBirthdayClick -> navigate { navigator.navigate(Route.Birthdays) }
            ChatDetailsAction.OnEditDescriptionClick -> _state.update {
                it.copy(
                    dialog = ChatDetailsDialog.Description,
                    descriptionDraft = TextFieldValue(it.chat.description.orEmpty())
                )
            }
            ChatDetailsAction.OnMapPreviewClick -> openMap()
            ChatDetailsAction.OnConnectedEventClick -> _state.value.connectedEvent?.let { event ->
                navigate { navigator.navigate(Route.EventDetail(eventId = event.id)) }
            }
            is ChatDetailsAction.OnExtensionAction -> onExtensionAction(action.action)
            is ChatDetailsAction.OnSharedContentClick -> navigate {
                navigator.navigate(Route.ChatSharedContent(chatId = chatId, isGroup = isGroup, initialTab = action.tab))
            }
            is ChatDetailsAction.OnOpenChat -> navigate {
                navigator.navigate(
                    Route.Chat(chatId = action.chatId, isGroup = action.isGroup),
                    navigationOptions = Navigator.NavigationOptions(exitPreviousScreen = true)
                )
            }
            is ChatDetailsAction.OnChangeAdminStatus -> changeAdminStatus(action.member)
            is ChatDetailsAction.OnRemoveMember -> changeMember(GroupMemberAction.REMOVE_USER, action.memberId)
            is ChatDetailsAction.OnOpenUnknownUser -> navigate {
                navigator.navigate(Route.UnknownUser(userId = action.userId, name = action.name))
            }

            ChatDetailsAction.OnAddMembersClick -> _state.update {
                it.copy(dialog = ChatDetailsDialog.AddMembers, selectedNewMembers = emptyList(), newMemberSearchTerm = "")
            }
            is ChatDetailsAction.OnNewMemberSearchChange -> _state.update { it.copy(newMemberSearchTerm = action.term) }
            is ChatDetailsAction.OnNewMemberSelected -> selectNewMember(action.item)
            is ChatDetailsAction.OnNewMemberDeselected -> deselectNewMember(action.item)
            ChatDetailsAction.OnAddMembersConfirm -> addSelectedMembers()

            is ChatDetailsAction.OnGroupSettingsConfirm -> saveGroupSettings(action.name, action.description, action.membersCanAdd)
            is ChatDetailsAction.OnNicknameConfirm -> {
                closeDialog()
                runOnServer { userRepository.changeUserDetails(userId = chatId, newNickName = action.nickname) }
            }
            is ChatDetailsAction.OnDescriptionDraftChange -> _state.update { it.copy(descriptionDraft = action.value) }
            ChatDetailsAction.OnDescriptionConfirm -> {
                val description = _state.value.descriptionDraft.text
                closeDialog()
                updateDescription(description)
            }
            is ChatDetailsAction.OnWakeConfirm -> {
                closeDialog()
                sendWake(action.reason)
            }
            is ChatDetailsAction.OnGroupTimerConfirm -> {
                closeDialog()
                setGroupExpiry(action.expiresAt)
            }
            ChatDetailsAction.OnRemoveGroupTimerConfirm -> {
                closeDialog()
                setGroupExpiry(null)
            }
            ChatDetailsAction.OnLeaveGroupConfirm -> leaveGroup()
            ChatDetailsAction.OnDeleteGroupConfirm -> deleteGroup()
            is ChatDetailsAction.OnDeleteEventConfirm -> detachConnectedEvent(action.deleteGroup, action.deleteEvent)
            ChatDetailsAction.OnRemoveFriendConfirm -> removeFriend()
        }
    }

    // ---- state from the database ------------------------------------------------------------

    /** The user with the common groups, or the group with its resolved members. */
    private fun chatFlow(): Flow<ChatDetailsChat> = if (isGroup) {
        groupRepository.getGroupByIdFlow(chatId).flatMapLatest { group ->
            if (group == null) return@flatMapLatest flowOf(ChatDetailsChat.Loading)
            userIdentityRepository.observeUsers(group.members.map { it.userId }).map { identities ->
                ChatDetailsChat.GroupDetails(group = group, members = groupMembersWithUsers(identities))
            }
        }
    } else {
        combine(userRepository.getUserByIdFlow(chatId), ownIdFlow) { user, ownId ->
            if (user == null) return@combine ChatDetailsChat.Loading
            val commonGroups = ownId?.let { groupRepository.getCommonGroups(ownId = it, otherUserId = chatId) }
            ChatDetailsChat.UserDetails(user = user, commonGroups = commonGroups.orEmpty())
        }
    }

    private suspend fun groupMembersWithUsers(identities: Map<String, UserIdentity>): List<GroupMemberWithUser> =
        groupRepository.getGroupMembers(chatId).map { member ->
            GroupMemberWithUser(
                groupMember = member,
                user = userRepository.getUserById(member.userId),
                identity = identities[member.userId] ?: UserIdentity(member.userId, UserIdentityName.Unknown),
            )
        }

    /**
     * How many images, links and messages the chat has shared. Links are counted here rather than
     * in SQL because a link only counts once the regex has found one in the message text -
     * `content LIKE '%http%'` alone would also match a message that just contains the word.
     */
    private fun observeCounts() {
        messageRepository.getImageMessagesForChatFlow(chatId, isGroup)
            .map { messages -> messages.count { !it.pictureUrl.isNullOrEmpty() } }
            .flowOn(Dispatchers.Default)
            .onEach { count -> _state.update { it.copy(sharedImageCount = count) } }
            .launchIn(viewModelScope)
        messageRepository.getLinkCandidateMessagesForChatFlow(chatId, isGroup)
            .map { messages -> messages.sumOf { extractLinks(it.content).size } }
            .flowOn(Dispatchers.Default)
            .onEach { count -> _state.update { it.copy(sharedLinkCount = count) } }
            .launchIn(viewModelScope)
        messageRepository.getMessageCountForChatFlow(chatId, isGroup)
            .onEach { count -> _state.update { it.copy(messageCount = count) } }
            .launchIn(viewModelScope)
    }

    /**
     * The map preview: friends who share a valid location (the user, or the group's members) and
     * the group's event when it has a location. Null hides the preview.
     */
    private fun observeMapPreview() {
        combine(
            _state.map { Triple(it.chat, it.connectedEvent, it.ownId) }.distinctUntilChanged(),
            userRepository.onlineFriendIdsFlow,
            settingsRepository.getMapStyleUrlFlow(),
        ) { (chat, event, ownId), onlineUserIds, styleUrl ->
            val users = when (chat) {
                is ChatDetailsChat.UserDetails -> listOf(chat.user)
                is ChatDetailsChat.GroupDetails -> chat.members.mapNotNull { it.user }
                ChatDetailsChat.Loading -> emptyList()
            }.filter { it.id != ownId && it.isLocationValid() }
            val eventWithLocation = event?.takeIf { it.location != null }

            if (users.isEmpty() && eventWithLocation == null) return@combine null
            ChatMapPreviewState(
                users = users,
                onlineUserIds = onlineUserIds,
                event = eventWithLocation,
                isEventJoined = ownId != null && eventWithLocation?.statusOf(ownId) == EventParticipationStatus.ACCEPTED,
                styleUrl = styleUrl,
            )
        }
            .onEach { preview -> _state.update { it.copy(mapPreview = preview) } }
            .launchIn(viewModelScope)
    }

    /** The group's event, its extensions (unfinished types stay hidden) and the people that can be added. */
    private fun observeGroupOnly() {
        eventRepository.getEventFlowForGroup(chatId)
            .onEach { event -> _state.update { it.copy(connectedEvent = event) } }
            .launchIn(viewModelScope)
        groupExtensionRepository.getExtensionsForOwnerFlow(chatId)
            .map { extensions -> extensions.filter { it.type.isAvailable } }
            .onEach { extensions -> _state.update { it.copy(groupExtensions = extensions) } }
            .launchIn(viewModelScope)
        observeAvailableNewMembers()
    }

    /**
     * Friends who aren't in the group yet, and the other groups: picking a group adds its member
     * friends instead of the group itself.
     */
    private fun observeAvailableNewMembers() {
        val searchTerm = _state.map { it.newMemberSearchTerm }.distinctUntilChanged()
        combine(
            searchTerm.flatMapLatest { term -> userRepository.getFriendsFlow(term) },
            groupRepository.getGroupByIdFlow(chatId),
            groupRepository.getAllGroupsFlow(),
            searchTerm,
            eventRepository.getEventGroupIdsFlow(),
        ) { friends, group, allGroups, term, eventGroupIds ->
            val currentMemberIds = group?.members.orEmpty().map { it.userId }.toSet()
            val availableFriends = friends.filter { it.id !in currentMemberIds }.map { it.toChatListItem() }
            val availableFriendIds = availableFriends.map { it.id }.toSet()
            val otherGroups = allGroups.filter { it.id != chatId && it.name.contains(term, ignoreCase = true) }

            val memberIdsByGroup = otherGroups.associate { other ->
                other.id to other.members.map { it.userId }.filter { it in availableFriendIds }.toSet()
            }
            val available = otherGroups.map { it.toChatListItem(hasEvent = it.id in eventGroupIds) } + availableFriends
            available to memberIdsByGroup
        }
            .onEach { (available, memberIdsByGroup) ->
                _state.update { it.copy(availableNewMembers = available, newMemberIdsByGroup = memberIdsByGroup) }
            }
            .launchIn(viewModelScope)
    }

    // ---- actions ----------------------------------------------------------------------------

    private fun openDialog(dialog: ChatDetailsDialog) = _state.update { it.copy(dialog = dialog, groupNameError = null) }

    private fun closeDialog() = _state.update { it.copy(dialog = null, groupNameError = null) }

    private fun navigate(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    /** Runs a server change that must finish even when the screen is left; failures go to the error popup. */
    private fun runOnServer(block: suspend () -> DataResult<*, DataError>) {
        applicationScope.launchOnMain { block().reportErrorTo(errorChannel) }
    }

    private fun goToChatList() = navigate {
        navigator.navigate(Route.ChatSelector, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
    }

    private fun selectNewMember(item: ChatListItem) = _state.update { state ->
        val added = if (item.isGroup) {
            val memberIds = state.newMemberIdsByGroup[item.id].orEmpty()
            state.availableNewMembers.filter { !it.isGroup && it.id in memberIds }
        } else {
            listOf(item)
        }
        state.copy(selectedNewMembers = (state.selectedNewMembers + added).distinctBy { it.id })
    }

    private fun deselectNewMember(item: ChatListItem) = _state.update { state ->
        val removedIds = if (item.isGroup) state.newMemberIdsByGroup[item.id].orEmpty() else setOf(item.id)
        state.copy(selectedNewMembers = state.selectedNewMembers.filterNot { it.id in removedIds })
    }

    /**
     * One request with only what differs from the stored group (the server rejects admin fields
     * from a non-admin, even unchanged ones). A rejected name keeps the popup open with its error.
     */
    private fun saveGroupSettings(name: String, description: String, membersCanAdd: Boolean) {
        val error = validateName(name)?.let { UiText.StringResourceText(it.toStringResource()) }
        if (error != null) {
            _state.update { it.copy(groupNameError = error) }
            return
        }
        closeDialog()
        applicationScope.launchOnMain {
            val group = groupRepository.getGroupById(chatId) ?: return@launchOnMain
            val changedName = name.takeIf { it != group.name }
            val changedDescription = description.takeIf { it != group.description }
            val changedMembersCanAdd = membersCanAdd.takeIf { it != group.membersCanAdd }
            if (changedName == null && changedDescription == null && changedMembersCanAdd == null) return@launchOnMain

            groupRepository.changeGroupSettings(
                groupId = chatId,
                name = changedName,
                description = changedDescription,
                membersCanAdd = changedMembersCanAdd,
            ).reportErrorTo(errorChannel)
        }
    }

    private fun updateDescription(description: String) = runOnServer {
        if (isGroup) {
            groupRepository.changeGroupSettings(groupId = chatId, description = description)
        } else {
            userRepository.changeUserDetails(newStatus = null, newDescription = description, userId = chatId)
        }
    }

    private fun changeAdminStatus(member: GroupMember) = runOnServer {
        groupRepository.changeGroupMembers(
            action = if (member.admin) GroupMemberAction.REMOVE_ADMIN else GroupMemberAction.MAKE_ADMIN,
            memberId = member.userId,
            groupId = member.groupId
        )
    }

    /** Adds the picked friends; a non-admin's add in an event group only invites them, so that gets its own confirmation. */
    private fun addSelectedMembers() {
        val selected = _state.value.selectedNewMembers
        val invitesOnly = _state.value.addInvitesToEvent
        closeDialog()
        applicationScope.launchOnMain {
            val allSent = selected.map { member ->
                groupRepository.changeGroupMembers(action = GroupMemberAction.ADD_USER, memberId = member.id, groupId = chatId)
                    .reportErrorTo(errorChannel)
                    .isSuccess
            }.all { it }
            if (invitesOnly && allSent) SnackbarManager.showMessage(getString(Res.string.event_friends_invited))
        }
    }

    private fun changeMember(action: GroupMemberAction, memberId: String) = runOnServer {
        groupRepository.changeGroupMembers(action = action, memberId = memberId, groupId = chatId)
    }

    /** Set, move or clear (null) the group's delete timer. Admins only - the server rejects anyone else. */
    private fun setGroupExpiry(expiresAt: Long?) = runOnServer { groupRepository.changeGroupExpiry(chatId, expiresAt) }

    private fun leaveGroup() {
        closeDialog()
        val ownId = _state.value.ownId ?: return
        changeMember(GroupMemberAction.REMOVE_USER, ownId)
        goToChatList()
    }

    /** Deletes the group for every member. Admins only - the server rejects anyone else. */
    private fun deleteGroup() {
        closeDialog()
        applicationScope.launchOnMain {
            if (groupRepository.deleteGroup(chatId).reportErrorTo(errorChannel).isSuccess) goToChatList()
        }
    }

    private fun removeFriend() {
        closeDialog()
        applicationScope.launchOnMain {
            if (userRepository.removeFriend(chatId).reportErrorTo(errorChannel).isSuccess) goToChatList()
        }
    }

    private fun detachConnectedEvent(deleteGroup: Boolean, deleteEvent: Boolean) {
        closeDialog()
        val eventId = _state.value.connectedEvent?.id ?: return
        runOnServer {
            eventRepository.detachEvent(eventId = eventId, groupId = chatId, deleteGroup = deleteGroup, deleteEvent = deleteEvent)
        }
    }

    private fun updateGroupPicture(photo: GalleryPhotoResult) {
        applicationScope.launchOnMain {
            val picture = pictureManager.downscaleImage(photo.loadBytes())
            val success = groupRepository.changeGroupProfilePic(groupId = chatId, newPic = picture)
                .reportErrorTo(errorChannel).isSuccess
            if (success) {
                // Coil's image cache keeps the old picture until the app restarts
                SnackbarManager.showMessage(getString(Res.string.please_restart_app))
            }
        }
    }

    /** Opens the Schneaggmap on the user, or for a group with every shown pin in view. */
    private fun openMap() {
        val preview = _state.value.mapPreview ?: return
        navigate {
            navigator.navigate(
                Route.Schneaggmap(
                    focusArea = if (isGroup) preview.focus else emptyList(),
                    focusUserId = chatId.takeUnless { isGroup },
                    openRequestId = Clock.System.now().toEpochMilliseconds(),
                )
            )
        }
    }

    private fun onExtensionAction(action: ExtensionListAction) {
        when (action) {
            is ExtensionListAction.OnExtensionClick -> navigate {
                navigator.navigate(Route.GroupExtension(extensionId = action.extensionId))
            }
            is ExtensionListAction.OnCreateExtension -> {
                closeDialog()
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

    /**
     * Ask the server to wake this chat's user or group. The server filters out everyone who has
     * not allowed us, so the outcome is reported back rather than silently swallowed - otherwise
     * a wake nobody permitted would look identical to one that worked.
     */
    private fun sendWake(reason: String) {
        val chatName = _state.value.chat.name
        applicationScope.launchOnMain {
            val response = when (val result = userRepository.sendWake(targetId = chatId, isGroup = isGroup, reason = reason)) {
                is DataResult.Success -> result.data
                is DataResult.Error -> {
                    // The server rejects waking a non-friend outright (403) instead of reporting an outcome
                    val message = when (result.error) {
                        is DataError.Forbidden -> Res.string.wake_not_friends
                        is DataError.NoInternetConnection -> Res.string.network_error_no_internet
                        is DataError.NetworkTimeout -> Res.string.network_error_timeout
                        else -> Res.string.wake_failed
                    }
                    errorChannel.sendError(UiText.StringResourceText(message))
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
                        if (response.skippedCount > 0) getString(Res.string.wake_sent_skipped, base, response.skippedCount) else base
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
}

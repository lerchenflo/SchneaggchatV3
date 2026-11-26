package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.ui.text.input.TextFieldValue
import io.github.ismoy.imagepickerkmp.picker.GalleryPhotoResult
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentity
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.sharedcontent.SharedContentTab
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.ExtensionListAction
import org.lerchenflo.schneaggchatv3mp.utilities.UiText

data class GroupMemberWithUser(
    val groupMember: GroupMember,
    // Null when the member is not in the users table (not a friend or request)
    val user: User?,
    val identity: UserIdentity,
)

/** The chat the screen shows: the enriched user or group, or Loading until it is read. */
sealed interface ChatDetailsChat {
    val name: String
    val profilePictureUrl: String
    val description: String?
    val status: String?

    data object Loading : ChatDetailsChat {
        override val name: String = ""
        override val profilePictureUrl: String = ""
        override val description: String? = null
        override val status: String? = null
    }

    data class UserDetails(
        val user: User,
        val commonGroups: List<Group> = emptyList()
    ) : ChatDetailsChat {
        override val name: String get() = user.name
        override val profilePictureUrl: String get() = user.profilePictureUrl
        override val description: String? get() = user.description
        override val status: String? get() = user.status
    }

    data class GroupDetails(
        val group: Group,
        val members: List<GroupMemberWithUser> = emptyList()
    ) : ChatDetailsChat {
        override val name: String get() = group.name
        override val profilePictureUrl: String get() = group.profilePictureUrl
        override val description: String get() = group.description
        override val status: String? get() = null
    }
}

/** The one dialog the screen shows at a time. */
sealed interface ChatDetailsDialog {
    data object RenameGroup : ChatDetailsDialog
    data object Nickname : ChatDetailsDialog
    data object Description : ChatDetailsDialog
    data object WakeReason : ChatDetailsDialog
    data object PicturePicker : ChatDetailsDialog
    data object ExpiryPicker : ChatDetailsDialog
    data object RemoveExpiry : ChatDetailsDialog
    data object AddMembers : ChatDetailsDialog
    data class AddExtension(val type: GroupExtensionType) : ChatDetailsDialog
    data object LeaveGroup : ChatDetailsDialog
    data object DeleteGroup : ChatDetailsDialog
    data object DeleteEvent : ChatDetailsDialog
    data object RemoveFriend : ChatDetailsDialog
}

data class ChatDetailsState(
    val isGroup: Boolean = false,
    /** Null until the session is known (autologin may still run when the screen opens). */
    val ownId: String? = null,
    val chat: ChatDetailsChat = ChatDetailsChat.Loading,
    /** The event this group was created for, if any. */
    val connectedEvent: Event? = null,
    /** Null hides the map preview. */
    val mapPreview: ChatMapPreviewState? = null,
    val sharedImageCount: Int = 0,
    val sharedLinkCount: Int = 0,
    val messageCount: Int = 0,
    val groupExtensions: List<GroupExtension> = emptyList(),
    val dialog: ChatDetailsDialog? = null,
    val groupNameError: UiText? = null,
    val descriptionDraft: TextFieldValue = TextFieldValue(""),
    val newMemberSearchTerm: String = "",
    /** Friends and other groups that can be picked to add to the group; a group stands for its member friends. */
    val availableNewMembers: List<ChatListItem> = emptyList(),
    /** For each group in [availableNewMembers]: the ids of its member friends in that list. */
    val newMemberIdsByGroup: Map<String, Set<String>> = emptyMap(),
    val selectedNewMembers: List<ChatListItem> = emptyList(),
) {
    val groupDetails: ChatDetailsChat.GroupDetails? get() = chat as? ChatDetailsChat.GroupDetails
    val userDetails: ChatDetailsChat.UserDetails? get() = chat as? ChatDetailsChat.UserDetails

    val iAmAdmin: Boolean
        get() = groupDetails?.members.orEmpty().any { it.groupMember.userId == ownId && it.groupMember.admin }

    val groupExpiresAt: Long? get() = groupDetails?.group?.expiresAt

    val isEventCreator: Boolean get() = ownId != null && connectedEvent?.creatorId == ownId

    /** True for a picked friend, or a group whose member friends are all picked. */
    fun isNewMemberSelected(item: ChatListItem): Boolean {
        val selectedIds = selectedNewMembers.map { it.id }.toSet()
        if (!item.isGroup) return item.id in selectedIds
        val memberIds = newMemberIdsByGroup[item.id].orEmpty()
        return memberIds.isNotEmpty() && memberIds.all { it in selectedIds }
    }
}

sealed interface ChatDetailsAction {
    data object OnBackClick : ChatDetailsAction
    data object OnDialogDismiss : ChatDetailsAction

    // ⋮ menu
    data object OnRenameClick : ChatDetailsAction
    data class OnAddExtensionClick(val type: GroupExtensionType) : ChatDetailsAction
    data object OnSetGroupTimerClick : ChatDetailsAction
    data object OnRemoveGroupTimerClick : ChatDetailsAction
    data object OnWakeClick : ChatDetailsAction
    data object OnLeaveGroupClick : ChatDetailsAction
    data object OnDeleteGroupClick : ChatDetailsAction
    data object OnDeleteEventClick : ChatDetailsAction
    data object OnRemoveFriendClick : ChatDetailsAction

    // Page
    data object OnEditPictureClick : ChatDetailsAction
    data class OnPicturePicked(val photo: GalleryPhotoResult) : ChatDetailsAction
    data object OnBirthdayClick : ChatDetailsAction
    data object OnEditDescriptionClick : ChatDetailsAction
    data object OnMapPreviewClick : ChatDetailsAction
    data object OnConnectedEventClick : ChatDetailsAction
    data class OnExtensionAction(val action: ExtensionListAction) : ChatDetailsAction
    data class OnSharedContentClick(val tab: SharedContentTab) : ChatDetailsAction
    data class OnOpenChat(val chatId: String, val isGroup: Boolean) : ChatDetailsAction
    data class OnChangeAdminStatus(val member: GroupMember) : ChatDetailsAction
    data class OnRemoveMember(val memberId: String) : ChatDetailsAction
    data class OnOpenUnknownUser(val userId: String, val name: String) : ChatDetailsAction

    // Add members popup
    data object OnAddMembersClick : ChatDetailsAction
    data class OnNewMemberSearchChange(val term: String) : ChatDetailsAction
    data class OnNewMemberSelected(val item: ChatListItem) : ChatDetailsAction
    data class OnNewMemberDeselected(val item: ChatListItem) : ChatDetailsAction
    data object OnAddMembersConfirm : ChatDetailsAction

    // Dialog confirmations
    data class OnGroupNameConfirm(val name: String) : ChatDetailsAction
    /** An empty nickname removes it. */
    data class OnNicknameConfirm(val nickname: String) : ChatDetailsAction
    data class OnDescriptionDraftChange(val value: TextFieldValue) : ChatDetailsAction
    data object OnDescriptionConfirm : ChatDetailsAction
    data class OnWakeConfirm(val reason: String) : ChatDetailsAction
    data class OnGroupTimerConfirm(val expiresAt: Long) : ChatDetailsAction
    data object OnRemoveGroupTimerConfirm : ChatDetailsAction
    data object OnLeaveGroupConfirm : ChatDetailsAction
    data object OnDeleteGroupConfirm : ChatDetailsAction
    data class OnDeleteEventConfirm(val deleteGroup: Boolean, val deleteEvent: Boolean) : ChatDetailsAction
    data object OnRemoveFriendConfirm : ChatDetailsAction
}

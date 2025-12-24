package org.lerchenflo.schneaggchatv3mp.chat.domain

import androidx.collection.emptyObjectList
import io.ktor.util.reflect.instanceOf
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils


interface SelectedChatBase {
    val id: String
    val isGroup: Boolean
    val name: String
    val profilePictureUrl: String
    val status: String?
    val description: String?
    val friendshipStatus: NetworkUtils.FriendshipStatus?
    val requesterId: String? //Requester of the friendship

    val unreadMessageCount: Int
    val unsentMessageCount: Int
    val lastmessage: Message?
}

data class NotSelected(
    override val id: String = "",
    override val isGroup: Boolean = false,
    override val name: String = "",
    override val profilePictureUrl: String = "",
    override val status: String? = null,
    override val description: String? = null,
    override val unreadMessageCount: Int = 0,
    override val unsentMessageCount: Int = 0,
    override val lastmessage: Message? = null,
    override val friendshipStatus: NetworkUtils.FriendshipStatus? = null,
    override val requesterId: String? = null
) : SelectedChatBase

// Create wrapper for User that implements SelectedChat
data class UserChat(
    override val id: String,
    override val name: String,
    override val profilePictureUrl: String,
    override val status: String?,
    override val description: String?,
    override val unreadMessageCount: Int,
    override val unsentMessageCount: Int,
    override val lastmessage: Message?,
    override val friendshipStatus: NetworkUtils.FriendshipStatus?,
    override val requesterId: String?,

    var commonGroups : List<Group> = emptyList()
) : SelectedChatBase {
    override val isGroup: Boolean = false
}

// Create wrapper for GroupWithMembers that implements SelectedChat
data class GroupChat(
    override val id: String,
    override val name: String,
    override val profilePictureUrl: String,
    override val status: String?,
    override val description: String?,
    override val unreadMessageCount: Int,
    override val unsentMessageCount: Int,
    override val lastmessage: Message?,
    override val friendshipStatus: NetworkUtils.FriendshipStatus? = null,
    override val requesterId: String? = null,

    var groupMembers : List<GroupMember> = emptyList()
) : SelectedChatBase {
    override val isGroup: Boolean = true
}

// Type alias for convenience
typealias SelectedChat = SelectedChatBase

fun SelectedChat.isNotSelected(): Boolean = this is NotSelected


// Extension function to convert User to UserChat
fun User.toSelectedChat(
    unreadCount: Int,
    unsentCount: Int,
    lastMessage: Message?
): UserChat = UserChat(
    id = this.id,
    name = this.name,
    profilePictureUrl = this.profilePictureUrl,
    status = this.status,
    description = this.description,
    unreadMessageCount = unreadCount,
    unsentMessageCount = unsentCount,
    lastmessage = lastMessage,
    friendshipStatus = this.friendshipStatus,
    requesterId = this.requesterId
)

// Extension function to convert GroupWithMembers to GroupChat
fun Group.toSelectedChat(
    unreadCount: Int,
    unsentCount: Int,
    lastMessage: Message?
): GroupChat = GroupChat(
    id = this.id,
    name = this.name,
    profilePictureUrl = this.profilePictureUrl,
    status = null,
    description = this.description,
    unreadMessageCount = unreadCount,
    unsentMessageCount = unsentCount,
    lastmessage = lastMessage
)

fun SelectedChat.toGroup(): GroupChat? =
    this as? GroupChat

fun SelectedChat.toUser(): UserChat? =
    this as? UserChat

package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.relations.MessageWithReadersDto
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
    val lastmessage: MessageWithReadersDto?
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
    override val lastmessage: MessageWithReadersDto? = null,
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
    override val lastmessage: MessageWithReadersDto?,
    override val friendshipStatus: NetworkUtils.FriendshipStatus?,
    override val requesterId: String?
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
    override val lastmessage: MessageWithReadersDto?,
    override val friendshipStatus: NetworkUtils.FriendshipStatus? = null,
    override val requesterId: String? = null
) : SelectedChatBase {
    override val isGroup: Boolean = true
}

// Type alias for convenience
typealias SelectedChat = SelectedChatBase

// Extension function to convert User to UserChat
fun User.toSelectedChat(
    unreadCount: Int,
    unsentCount: Int,
    lastMessage: MessageWithReadersDto?
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
    lastMessage: MessageWithReadersDto?
): GroupChat = GroupChat(
    id = this.id,
    name = this.name,
    profilePictureUrl = this.profilePicture,
    status = null,
    description = this.description,
    unreadMessageCount = unreadCount,
    unsentMessageCount = unsentCount,
    lastmessage = lastMessage
)
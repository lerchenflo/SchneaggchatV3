package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils

/**
 * Flat presentation model for a chat row (chat selector, member pickers, chat header).
 * Works for both users and groups; user-only fields are null/false for groups.
 */
data class ChatListItem(
    val id: String,
    val isGroup: Boolean,
    val name: String,
    val nickName: String? = null,
    val profilePictureUrl: String = "",
    val status: String? = null,
    val description: String? = null,
    val friendshipStatus: NetworkUtils.FriendshipStatus? = null,
    val requesterId: String? = null, //Requester of the friendship

    val unreadMessageCount: Int = 0,
    val unsentMessageCount: Int = 0,
    val lastMessage: Message? = null,
    val pinned: Long = 0L,

    val birthDate: String? = null, //YYYY-MM-dd

    // Live "online right now" (never persisted, see UserRepository.onlineFriendIdsFlow) and the
    // persisted last-seen timestamp (epoch millis) - both null/false when unknown.
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
) {
    val displayName: String get() = nickName?.takeIf { it.isNotBlank() } ?: name
}

fun User.toChatListItem(
    unreadCount: Int = 0,
    unsentCount: Int = 0,
    lastMessage: Message? = null,
    pinned: Long = 0L,
    isOnline: Boolean = false,
): ChatListItem = ChatListItem(
    id = this.id,
    isGroup = false,
    name = this.name,
    nickName = this.nickName,
    profilePictureUrl = this.profilePictureUrl,
    status = this.status,
    description = this.description,
    friendshipStatus = this.friendshipStatus,
    requesterId = this.requesterId,
    unreadMessageCount = unreadCount,
    unsentMessageCount = unsentCount,
    lastMessage = lastMessage,
    pinned = pinned,
    birthDate = this.birthDate,
    isOnline = isOnline,
    lastSeen = this.lastSeen,
)

fun Group.toChatListItem(
    unreadCount: Int = 0,
    unsentCount: Int = 0,
    lastMessage: Message? = null,
    pinned: Long = 0L,
): ChatListItem = ChatListItem(
    id = this.id,
    isGroup = true,
    name = this.name,
    profilePictureUrl = this.profilePictureUrl,
    description = this.description,
    unreadMessageCount = unreadCount,
    unsentMessageCount = unsentCount,
    lastMessage = lastMessage,
    pinned = pinned,
)

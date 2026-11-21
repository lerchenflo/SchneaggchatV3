package org.lerchenflo.schneaggchatv3mp.chat.domain


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
    val friendshipStatus: FriendshipStatus? = null,
    val requesterId: String? = null, //Requester of the friendship

    val unreadMessageCount: Int = 0,
    val unsentMessageCount: Int = 0,
    /** A group with an unread message that mentions me or answers me: "@" badge in the chat selector. */
    val hasUnreadMention: Boolean = false,
    val lastMessage: Message? = null,
    // Names of the users the last message preview names (see Message.previewUserIds)
    val lastMessageNames: Map<String, UserIdentityName> = emptyMap(),
    val pinned: Long = 0L,

    val birthDate: String? = null, //YYYY-MM-dd

    // Optional phone number, shared with accepted friends. Null for groups.
    val phoneNumber: String? = null,

    // Live "online right now" (never persisted, see UserLocalDataSource.onlineFriendIdsFlow) and the
    // persisted last-seen timestamp (epoch millis) - both null/false when unknown.
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,

    // Group connected to an event, marked on its picture. Always false for users.
    val hasEvent: Boolean = false,
) {
    val displayName: String get() = nickName?.takeIf { it.isNotBlank() } ?: name
}

/**
 * First chat with an accepted friend whose last message is a real message, not a system line (e.g. for
 * the tour to open and point at that message), or null.
 */
fun List<ChatListItem>.firstFriendChatWithMessages(): ChatListItem? =
    firstOrNull { chat ->
        !chat.isGroup &&
            chat.friendshipStatus == FriendshipStatus.ACCEPTED &&
            chat.lastMessage.let { it != null && it.msgType != MessageType.SYSTEM }
    }

fun User.toChatListItem(
    unreadCount: Int = 0,
    unsentCount: Int = 0,
    lastMessage: Message? = null,
    lastMessageNames: Map<String, UserIdentityName> = emptyMap(),
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
    lastMessageNames = lastMessageNames,
    pinned = pinned,
    birthDate = this.birthDate,
    phoneNumber = this.phoneNumber,
    isOnline = isOnline,
    lastSeen = this.lastSeen,
)

fun Group.toChatListItem(
    unreadCount: Int = 0,
    unsentCount: Int = 0,
    lastMessage: Message? = null,
    lastMessageNames: Map<String, UserIdentityName> = emptyMap(),
    pinned: Long = 0L,
    hasEvent: Boolean = false,
    hasUnreadMention: Boolean = false,
): ChatListItem = ChatListItem(
    id = this.id,
    isGroup = true,
    hasUnreadMention = hasUnreadMention,
    name = this.name,
    profilePictureUrl = this.profilePictureUrl,
    description = this.description,
    unreadMessageCount = unreadCount,
    unsentMessageCount = unsentCount,
    lastMessage = lastMessage,
    lastMessageNames = lastMessageNames,
    pinned = pinned,
    hasEvent = hasEvent,
)

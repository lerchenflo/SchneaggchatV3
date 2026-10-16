package org.lerchenflo.schneaggchatv3mp.utilities.notifications

/** What a notification is about - picks its notification channel and system category on Android. */
enum class NotificationKind {
    /** A chat message */
    MESSAGE,
    /** Someone reacted to one of our messages */
    REACTION,
    /** Friend requests, events, birthdays */
    SOCIAL,
    /** Everything else, e.g. messages from the server */
    GENERAL,
}

data class NotificationContent(
    val id: Int,
    val title: String,
    val body: String,

    /** Chat this notification belongs to - tapping the notification opens it. Message notifications also offer reply / "mark as read" for it. */
    val chatId: String? = null,
    val groupChat: Boolean = false,

    /** Sender of this message - set only for message notifications, used to build MessagingStyle history. */
    val senderId: String? = null,
    val senderName: String? = null,
    val timestampMillis: Long = 0L,
    /** Group display name, for group chats only. */
    val groupName: String? = null,

    /** True for chat messages: shown as one updating conversation per chat (MessagingStyle on Android). */
    val conversation: Boolean = false,

    /** User whose profile picture is shown on a non-conversation notification (reactor, requester, event creator, ...). */
    val avatarUserId: String? = null,

    val kind: NotificationKind = NotificationKind.GENERAL,
)

package org.lerchenflo.schneaggchatv3mp.utilities.notifications

data class NotificationContent(
    val id: Int,
    val title: String,
    val body: String,

    /** Chat this notification belongs to. Set for message notifications so a "mark as read" action can be offered. */
    val chatId: String? = null,
    val groupChat: Boolean = false,

    /** Sender of this message - set only for message notifications, used to build MessagingStyle history. */
    val senderId: String? = null,
    val senderName: String? = null,
    val timestampMillis: Long = 0L,
    /** Group display name, for group chats only. */
    val groupName: String? = null,
)

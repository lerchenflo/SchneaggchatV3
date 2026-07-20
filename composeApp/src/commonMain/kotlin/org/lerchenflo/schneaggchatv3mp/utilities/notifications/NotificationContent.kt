package org.lerchenflo.schneaggchatv3mp.utilities.notifications

data class NotificationContent(
    val id: Int,
    val title: String,
    val body: String,

    /** Chat this notification belongs to. Set for message notifications so a "mark as read" action can be offered. */
    val chatId: String? = null,
    val groupChat: Boolean = false,
)

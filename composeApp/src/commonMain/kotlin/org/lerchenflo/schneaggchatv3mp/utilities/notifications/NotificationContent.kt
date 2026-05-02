package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import kotlin.math.absoluteValue

data class NotificationContent(
    val id: Int,
    val title: String,
    val body: String,
    val channelId: String,
)

object NotificationChannels {
    const val MESSAGES = "messages"
    const val FRIEND_REQUESTS = "friend_requests"
    const val SYSTEM = "system"
}

fun String.toNotificationId(): Int = hashCode().absoluteValue

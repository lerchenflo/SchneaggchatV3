package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType

@Serializable
sealed interface NotificationResponse {

    @Serializable
    @SerialName("message")
    data class MessageNotificationResponse(
        val msgId: String,
        val senderName: String,
        val groupMessage: Boolean,
        val messageType: MessageType,
        val groupName: String,
        val content: String
    ) : NotificationResponse

    //Response for a friend request notification
    @Serializable
    @SerialName("friend_request")
    data class FriendRequestNotificationResponse(
        val requesterId: String,
        val requesterName: String,
        val accepted: Boolean
    ) : NotificationResponse

    //Response for a system notification
    @Serializable
    @SerialName("system")
    data class SystemNotificationResponse(
        val title: String,
        val message: String,
        val priority: String = "normal"
    ) : NotificationResponse
}

package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType

sealed interface DecodedNotification {
    data class Message(
        val msgId: String,
        val senderName: String,
        val groupName: String,
        val messageType: MessageType,
        val groupMessage: Boolean,
        val encodedContent: String,
        val reaction: Boolean = false,
    ) : DecodedNotification

    data class FriendRequest(
        val requesterId: String,
        val requesterName: String,
        val accepted: Boolean,
    ) : DecodedNotification

    data class System(
        val title: String,
        val message: String,
    ) : DecodedNotification

    data class Birthday(
        val birthdayUserId: String,
        val birthdayUserName: String,
        val ownBirthday: Boolean,
    ) : DecodedNotification
}

object PayloadDecoder {
    fun decode(data: Map<String, String>): DecodedNotification? {
        return when (data["type"]) {
            "message" -> DecodedNotification.Message(
                msgId = data["msgId"] ?: return null,
                senderName = data["senderName"] ?: "",
                groupName = data["groupName"] ?: "",
                messageType = runCatching { MessageType.valueOf(data["messageType"] ?: "") }
                    .getOrDefault(MessageType.TEXT),
                groupMessage = data["groupMessage"]?.toBoolean() ?: false,
                encodedContent = data["encodedContent"] ?: "",
                reaction = data["reaction"]?.toBoolean() ?: false,
            )
            "friend_request" -> DecodedNotification.FriendRequest(
                requesterId = data["requesterId"] ?: "",
                requesterName = data["requesterName"] ?: "",
                accepted = data["accepted"]?.toBoolean() ?: false,
            )
            "system" -> DecodedNotification.System(
                title = data["title"] ?: "Schneaggchat",
                message = data["message"] ?: "",
            )
            "birthday" -> DecodedNotification.Birthday(
                birthdayUserId = data["birthdayUserId"] ?: "",
                birthdayUserName = data["birthdayUserName"] ?: "",
                ownBirthday = data["ownBirthday"]?.toBoolean() ?: false,
            )
            else -> null
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType

sealed interface DecodedNotification {
    data class Message(
        val msgId: String,
        val senderName: String,
        val groupMessage: Boolean,
        val messageType: MessageType,
        val groupName: String,
        val encodedContent: String,
        val senderId: String,
        val receiverId: String,
        val reaction: Boolean = false
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

    /** Someone wants to wake us. Android only - handled by the alarm service, not the Notifier. */
    data class Wake(
        val senderId: String,
        val senderName: String,
        val reason: String,
        val groupId: String,
        val groupName: String,
        //How many people were woken by the same request, us included
        val wokenUserCount: Int,
        val wokenDeviceCount: Int,
    ) : DecodedNotification {
        val isGroupWake: Boolean get() = groupId.isNotEmpty()
    }
}

object PayloadDecoder {
    fun decode(data: Map<String, String>): DecodedNotification? {
        return when (data["_class"]) {
            "message" -> DecodedNotification.Message(
                msgId = data["msgId"] ?: return null,
                senderName = data["senderName"] ?: "",
                groupName = data["groupName"] ?: "",
                messageType = runCatching { MessageType.valueOf(data["messageType"] ?: "") }
                    .getOrDefault(MessageType.TEXT),
                groupMessage = data["groupMessage"]?.toBoolean() ?: false,
                encodedContent = data["encodedContent"] ?: "",
                senderId = data["senderId"] ?: "",
                receiverId = data["receiverId"] ?: "",
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
            "wake" -> DecodedNotification.Wake(
                senderId = data["senderId"] ?: return null,
                senderName = data["senderName"] ?: "",
                reason = data["reason"] ?: "",
                groupId = data["groupId"] ?: "",
                groupName = data["groupName"] ?: "",
                wokenUserCount = data["wokenUserCount"]?.toIntOrNull() ?: 1,
                wokenDeviceCount = data["wokenDeviceCount"]?.toIntOrNull() ?: 1,
            )
            else -> null
        }
    }
}

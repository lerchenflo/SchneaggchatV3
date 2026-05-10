package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.utilities.CryptoUtil
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.audio
import schneaggchatv3mp.composeapp.generated.resources.friend_birthday_noti_body
import schneaggchatv3mp.composeapp.generated.resources.friend_birthday_noti_title
import schneaggchatv3mp.composeapp.generated.resources.image
import schneaggchatv3mp.composeapp.generated.resources.new_friend_accepted_noti
import schneaggchatv3mp.composeapp.generated.resources.new_friend_accepted_noti_body
import schneaggchatv3mp.composeapp.generated.resources.new_friend_request_noti
import schneaggchatv3mp.composeapp.generated.resources.new_friend_request_noti_body
import schneaggchatv3mp.composeapp.generated.resources.own_birthday_noti_body
import schneaggchatv3mp.composeapp.generated.resources.own_birthday_noti_title
import schneaggchatv3mp.composeapp.generated.resources.poll
import schneaggchatv3mp.composeapp.generated.resources.you_have_new_messages

suspend fun resolveLocalizedContent(
    decoded: DecodedNotification,
    encryptionKey: String?,
): NotificationContent? = when (decoded) {
    is DecodedNotification.Message       -> resolveMessage(decoded, encryptionKey)
    is DecodedNotification.FriendRequest -> resolveFriendRequest(decoded)
    is DecodedNotification.System        -> resolveSystem(decoded)
    is DecodedNotification.Birthday      -> resolveBirthday(decoded)
}

private suspend fun resolveMessage(
    decoded: DecodedNotification.Message,
    encryptionKey: String?,
): NotificationContent {
    val body = if (!encryptionKey.isNullOrEmpty()) {
        runCatching {
            when (decoded.messageType) {
                MessageType.TEXT  -> CryptoUtil.decrypt(decoded.encodedContent, encryptionKey)
                MessageType.IMAGE -> getString(Res.string.image)
                MessageType.AUDIO -> getString(Res.string.audio)
                MessageType.POLL  -> getString(Res.string.poll)
            }
        }.getOrElse { getString(Res.string.you_have_new_messages) }
    } else {
        getString(Res.string.you_have_new_messages)
    }

    val msg = Message(
        msgType = decoded.messageType,
        content = body,
        senderId = "",
        receiverId = "",
        groupMessage = decoded.groupMessage,
        senderAsString = decoded.senderName,
        myMessage = false,
        readByMe = false,
        readers = emptyList(),
        id = decoded.msgId,
    )
    return msg.toNotificationContent(
        fallbackGroupName = decoded.groupName.ifEmpty { null }
    )
}

private suspend fun resolveFriendRequest(
    decoded: DecodedNotification.FriendRequest,
): NotificationContent {
    val title: String
    val body: String
    if (decoded.accepted) {
        title = getString(Res.string.new_friend_accepted_noti)
        body = getString(Res.string.new_friend_accepted_noti_body, decoded.requesterName)
    } else {
        title = getString(Res.string.new_friend_request_noti, decoded.requesterName)
        body = getString(Res.string.new_friend_request_noti_body, decoded.requesterName)
    }
    return NotificationContent(
        id = NotificationManager.NotiIdType.FRIEND_REQUEST.baseId,
        title = title,
        body = body,
    )
}

private fun resolveSystem(
    decoded: DecodedNotification.System,
): NotificationContent = NotificationContent(
    id = NotificationManager.NotiIdType.SERVERMESSAGE.baseId,
    title = decoded.title,
    body = decoded.message,
)

private suspend fun resolveBirthday(
    decoded: DecodedNotification.Birthday,
): NotificationContent {
    val title: String
    val body: String
    if (decoded.ownBirthday) {
        title = getString(Res.string.own_birthday_noti_title)
        body = getString(Res.string.own_birthday_noti_body)
    } else {
        title = getString(Res.string.friend_birthday_noti_title, decoded.birthdayUserName)
        body = getString(Res.string.friend_birthday_noti_body)
    }
    return NotificationContent(
        id = NotificationManager.NotiIdType.BIRTHDAY.baseId,
        title = title,
        body = body,
    )
}

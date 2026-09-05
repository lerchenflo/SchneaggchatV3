package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.audio
import schneaggchatv3mp.composeapp.generated.resources.friend_birthday_noti_body
import schneaggchatv3mp.composeapp.generated.resources.friend_birthday_noti_title
import schneaggchatv3mp.composeapp.generated.resources.image
import schneaggchatv3mp.composeapp.generated.resources.message
import schneaggchatv3mp.composeapp.generated.resources.new_friend_accepted_noti
import schneaggchatv3mp.composeapp.generated.resources.new_friend_accepted_noti_body
import schneaggchatv3mp.composeapp.generated.resources.new_friend_request_noti
import schneaggchatv3mp.composeapp.generated.resources.new_friend_request_noti_body
import schneaggchatv3mp.composeapp.generated.resources.new_message_reaction
import schneaggchatv3mp.composeapp.generated.resources.new_event_noti_title
import schneaggchatv3mp.composeapp.generated.resources.own_birthday_noti_body
import schneaggchatv3mp.composeapp.generated.resources.own_birthday_noti_title
import schneaggchatv3mp.composeapp.generated.resources.poll
import schneaggchatv3mp.composeapp.generated.resources.you_have_new_messages
import kotlin.math.absoluteValue

suspend fun resolveLocalizedContent(
    decoded: DecodedNotification,
): NotificationContent? = when (decoded) {
    is DecodedNotification.Message       -> resolveMessage(decoded)
    is DecodedNotification.FriendRequest -> resolveFriendRequest(decoded)
    is DecodedNotification.System        -> resolveSystem(decoded)
    is DecodedNotification.Birthday      -> resolveBirthday(decoded)
    //Wakes never go through the normal notification path - the alarm service owns its own
    //foreground notification, so there is nothing for the Notifier to show.
    is DecodedNotification.Wake          -> null
    is DecodedNotification.Event         -> resolveEvent(decoded)
}

private suspend fun resolveMessage(
    decoded: DecodedNotification.Message,
): NotificationContent? {
    if (decoded.reaction) return resolveReaction(decoded)

    // Server-authored SYSTEM messages (group renamed, member added, wake sent, ...) never push -
    // this branch is currently unreachable, but keep it honest in case that ever changes.
    if (decoded.messageType == MessageType.SYSTEM) return null

    val body = when (decoded.messageType) {
        //Blank when the push came from a server that predates the plaintext `content` field.
        MessageType.TEXT  -> decoded.content.ifBlank { getString(Res.string.you_have_new_messages) }
        MessageType.IMAGE -> getString(Res.string.image)
        MessageType.AUDIO -> getString(Res.string.audio)
        MessageType.POLL  -> getString(Res.string.poll)
        MessageType.SYSTEM -> getString(Res.string.you_have_new_messages)
    }

    val msg = Message(
        msgType = decoded.messageType,
        content = body,
        senderId = decoded.senderId,
        //For group messages the chat is identified by groupId, not by the raw push receiverId
        //(which is the recipient user, not the group) - see Message.toNotificationContent().
        receiverId = if (decoded.groupMessage) decoded.groupId else decoded.receiverId,
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

private suspend fun resolveReaction(
    decoded: DecodedNotification.Message,
): NotificationContent {
    val reactionEmoji = decoded.content

    val typeWord = getString(
        when (decoded.messageType) {
            MessageType.TEXT  -> Res.string.message
            MessageType.IMAGE -> Res.string.image
            MessageType.AUDIO -> Res.string.audio
            MessageType.POLL  -> Res.string.poll
            MessageType.SYSTEM -> Res.string.message
        }
    )

    val title = getString(Res.string.new_message_reaction, decoded.senderName, typeWord)

    return NotificationContent(
        id = "reaction:${decoded.msgId}:${decoded.senderName}:$reactionEmoji".hashCode().absoluteValue,
        title = title,
        body = reactionEmoji,
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

private suspend fun resolveEvent(
    decoded: DecodedNotification.Event,
): NotificationContent {
    val title = getString(Res.string.new_event_noti_title, decoded.creatorName)
    return NotificationContent(
        id = NotificationManager.NotiIdType.EVENT.baseId,
        title = title,
        body = decoded.eventTitle,
    )
}

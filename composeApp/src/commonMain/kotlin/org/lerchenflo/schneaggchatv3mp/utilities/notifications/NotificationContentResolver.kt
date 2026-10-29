package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
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
import schneaggchatv3mp.composeapp.generated.resources.notification_fallback_body
import schneaggchatv3mp.composeapp.generated.resources.own_birthday_noti_body
import schneaggchatv3mp.composeapp.generated.resources.own_birthday_noti_title
import schneaggchatv3mp.composeapp.generated.resources.poll
import schneaggchatv3mp.composeapp.generated.resources.you_have_new_messages
import kotlin.math.absoluteValue

//Title of a last-resort notification when not even a sender name is known
internal const val NOTIFICATION_FALLBACK_TITLE = "Schneaggchat"

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
        sendDate = decoded.sendDate.takeIf { it > 0 }?.toString() ?: "",
    )
    return msg.toNotificationContent(
        fallbackGroupName = decoded.groupName.ifEmpty { null }
    )
}

private suspend fun resolveReaction(
    decoded: DecodedNotification.Message,
): NotificationContent = reactionNotificationContent(
    msgId = decoded.msgId,
    reactorId = decoded.senderId,
    reactorName = decoded.senderName,
    emoji = decoded.content,
    messageType = decoded.messageType,
    groupMessage = decoded.groupMessage,
    groupId = decoded.groupId,
)

/**
 * Someone reacted to one of our messages. Shows the reactor's profile picture and opens the chat
 * the message lives in: the group for group messages, the 1:1 chat with the reactor otherwise.
 * Used by the push path and by the socket path (which spots new reactions itself).
 */
suspend fun reactionNotificationContent(
    msgId: String,
    reactorId: String,
    reactorName: String,
    emoji: String,
    messageType: MessageType,
    groupMessage: Boolean,
    groupId: String,
): NotificationContent {
    val typeWord = getString(
        when (messageType) {
            MessageType.TEXT  -> Res.string.message
            MessageType.IMAGE -> Res.string.image
            MessageType.AUDIO -> Res.string.audio
            MessageType.POLL  -> Res.string.poll
            MessageType.SYSTEM -> Res.string.message
        }
    )

    val title = getString(Res.string.new_message_reaction, reactorName, typeWord)

    return NotificationContent(
        id = reactionNotificationId(msgId, reactorName, emoji),
        title = title,
        body = emoji,
        chatId = (if (groupMessage) groupId else reactorId).ifBlank { null },
        groupChat = groupMessage,
        avatarUserId = reactorId.ifBlank { null },
        kind = NotificationKind.REACTION,
    )
}

private fun reactionNotificationId(msgId: String, reactorName: String, emoji: String): Int =
    "reaction:$msgId:$reactorName:$emoji".hashCode().absoluteValue

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
    //Older servers put our own id into requesterId - never show our own picture on someone else's request
    val ownId = SessionCache.requireLoggedIn()?.userId
    return NotificationContent(
        id = NotificationManager.NotiIdType.FRIEND_REQUEST.baseId,
        title = title,
        body = body,
        avatarUserId = decoded.requesterId.takeIf { ownId != null && it.isNotBlank() && it != ownId },
        kind = NotificationKind.SOCIAL,
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
        avatarUserId = decoded.birthdayUserId.ifBlank { null },
        kind = NotificationKind.SOCIAL,
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
        avatarUserId = decoded.creatorId.ifBlank { null },
        kind = NotificationKind.SOCIAL,
    )
}

/**
 * Last-resort content for when [resolveLocalizedContent] failed or timed out: built from the raw
 * push fields only, no database lookups. Never throws. Null for wakes - the alarm service owns those.
 */
suspend fun resolveFallbackContent(decoded: DecodedNotification): NotificationContent? {
    val fallbackBody = stringOrEmpty(Res.string.notification_fallback_body)
    return when (decoded) {
        is DecodedNotification.Message -> {
            val chatId = decoded.chatTargetId
            val title = listOf(decoded.senderName, decoded.groupName)
                .filter { it.isNotBlank() }
                .joinToString(" - ")
                .ifBlank { NOTIFICATION_FALLBACK_TITLE }
            NotificationContent(
                //Same id as the chat's conversation notification, so it doesn't stack a second one -
                //except for reactions, which never replace the chat's messages
                id = if (decoded.reaction || chatId == null) {
                    reactionNotificationId(decoded.msgId, decoded.senderName, decoded.content)
                } else {
                    chatId.hashCode().absoluteValue
                },
                title = title,
                body = decoded.content.takeIf { decoded.reaction || decoded.messageType == MessageType.TEXT }
                    ?.ifBlank { null }
                    ?: fallbackBody,
                chatId = chatId,
                groupChat = decoded.groupMessage,
                kind = if (decoded.reaction) NotificationKind.REACTION else NotificationKind.MESSAGE,
            )
        }
        is DecodedNotification.FriendRequest -> NotificationContent(
            id = NotificationManager.NotiIdType.FRIEND_REQUEST.baseId,
            title = decoded.requesterName.ifBlank { NOTIFICATION_FALLBACK_TITLE },
            body = fallbackBody,
            kind = NotificationKind.SOCIAL,
        )
        is DecodedNotification.System -> resolveSystem(decoded)
        is DecodedNotification.Birthday -> NotificationContent(
            id = NotificationManager.NotiIdType.BIRTHDAY.baseId,
            title = decoded.birthdayUserName.ifBlank { NOTIFICATION_FALLBACK_TITLE },
            body = fallbackBody,
            kind = NotificationKind.SOCIAL,
        )
        is DecodedNotification.Wake -> null
        is DecodedNotification.Event -> NotificationContent(
            id = NotificationManager.NotiIdType.EVENT.baseId,
            title = decoded.creatorName.ifBlank { NOTIFICATION_FALLBACK_TITLE },
            body = decoded.eventTitle.ifBlank { fallbackBody },
            kind = NotificationKind.SOCIAL,
        )
    }
}

private suspend fun stringOrEmpty(resource: StringResource): String =
    runCatching { getString(resource) }.getOrDefault("")

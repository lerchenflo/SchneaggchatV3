package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.audio
import schneaggchatv3mp.composeapp.generated.resources.image
import schneaggchatv3mp.composeapp.generated.resources.new_message_noti_group_title
import schneaggchatv3mp.composeapp.generated.resources.new_message_noti_single_title
import schneaggchatv3mp.composeapp.generated.resources.poll
import schneaggchatv3mp.composeapp.generated.resources.you_have_new_messages
import kotlin.math.absoluteValue

suspend fun Message.toNotificationContent(fallbackGroupName: String? = null): NotificationContent {
    val title = if (groupMessage && fallbackGroupName != null)
        getString(Res.string.new_message_noti_group_title, senderAsString, fallbackGroupName)
    else
        getString(Res.string.new_message_noti_single_title, senderAsString)
    val body = when (msgType) {
        MessageType.TEXT  -> content.ifBlank { getString(Res.string.you_have_new_messages) }
        MessageType.IMAGE -> getString(Res.string.image)
        MessageType.AUDIO -> getString(Res.string.audio)
        MessageType.POLL  -> getString(Res.string.poll)
    }
    val notifId = id?.hashCode()?.absoluteValue ?: 0
    return NotificationContent(id = notifId, title = title, body = body)
}

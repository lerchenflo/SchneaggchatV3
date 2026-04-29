package org.lerchenflo.schneaggchatv3mp.chat.data.mappers

import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toPollMessage

fun NetworkUtils.MessageResponse.toDomainMessage(
    ownId: String,
    existing: Message?
): Message = Message(
    localPK = existing?.localPK ?: 0L,
    id = messageId,
    msgType = msgType,
    content = content,
    poll = if (msgType == MessageType.POLL) pollResponse?.toPollMessage(ownId) else null,
    pictureUrl = if (msgType == MessageType.IMAGE) existing?.pictureUrl else null,
    audioPath = if (msgType == MessageType.AUDIO) existing?.audioPath else null,
    senderId = senderId,
    receiverId = receiverId,
    sendDate = sendDate.toString(),
    changeDate = lastChanged.toString(),
    deleted = deleted,
    groupMessage = groupMessage,
    answerId = answerId,
    sent = true,
    myMessage = senderId == ownId,
    readByMe = readers.any { it.userId == ownId },
    readers = readers.map {
        MessageReader(
            readerEntryId = 0L,
            messageId = messageId,
            readerId = it.userId,
            readDate = it.readAt.toString()
        )
    }
)

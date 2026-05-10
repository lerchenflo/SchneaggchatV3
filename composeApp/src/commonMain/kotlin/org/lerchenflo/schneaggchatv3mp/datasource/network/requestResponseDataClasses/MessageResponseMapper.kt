package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.Reaction
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils

fun NetworkUtils.MessageResponse.toDomainMessage(
    ownId: String,
    existingLocalPK: Long = 0L,
    existingPictureUrl: String? = null,
    existingAudioPath: String? = null
): Message = Message(
    localPK = existingLocalPK,
    id = messageId,
    msgType = msgType,
    content = content,
    pictureUrl = if (msgType == MessageType.IMAGE) existingPictureUrl else null,
    audioPath = if (msgType == MessageType.AUDIO) existingAudioPath else null,
    poll = pollResponse?.toPollMessage(ownId),
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
            messageId = this.messageId,
            readerId = it.userId,
            readDate = it.readAt.toString()
        )
    },
    reactions = reactions.map { Reaction(userId = it.userId, content = it.content) }
)

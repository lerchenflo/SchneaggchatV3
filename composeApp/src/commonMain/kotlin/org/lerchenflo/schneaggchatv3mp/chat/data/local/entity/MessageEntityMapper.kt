package org.lerchenflo.schneaggchatv3mp.chat.data.local.entity

import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.relations.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader

fun MessageWithReaders.toMessage(): Message = this.messageDto.toMessage(
    readers = this.readers.map { readerDto ->
        readerDto.toMessageReader()
    }
)

/**
 * Entity -> domain. [readers] defaults to empty for callers that read a message without its relation,
 * e.g. the chat selector's last message preview.
 */
fun MessageEntity.toMessage(readers: List<MessageReader> = emptyList()): Message = Message(
    localPK = this.localPK,
    id = this.id,
    msgType = this.msgType,
    content = this.content,
    poll = this.poll,
    systemEvent = this.systemEvent,
    pictureUrl = this.pictureUrl,
    audioPath = this.audioPath,
    senderId = this.senderId,
    receiverId = this.receiverId,
    sendDate = if (this.sendDate == 0L) "" else this.sendDate.toString(),
    changeDate = this.updatedAt,
    deleted = this.deleted,
    edited = this.edited,
    groupMessage = this.groupMessage,
    answerId = this.answerId,
    sent = this.sent,
    senderAsString = this.senderAsString,
    minimizeMessage = this.minimizeMessage,
    senderColor = this.senderColor,
    myMessage = this.myMessage,
    readByMe = this.readByMe,
    readers = readers,
    reactions = this.reactions,
    version = this.version,
    clientMessageId = this.clientMessageId,
)

/** Domain -> entity */
fun Message.toEntity(): MessageWithReaders = MessageWithReaders(
    messageDto = MessageEntity(
        localPK = this.localPK,
        id = this.id,
        msgType = this.msgType,
        content = this.content,
        poll = this.poll,
        systemEvent = this.systemEvent,
        pictureUrl = this.pictureUrl,
        senderId = this.senderId,
        receiverId = this.receiverId,
        // Empty or malformed becomes 0, the same value the old CAST(sendDate AS INTEGER) gave it.
        sendDate = this.sendDate.toLongOrNull() ?: 0L,
        updatedAt = this.changeDate,
        deleted = this.deleted,
        edited = this.edited,
        groupMessage = this.groupMessage,
        answerId = this.answerId,
        sent = this.sent,
        myMessage = this.myMessage,
        readByMe = this.readByMe,
        reactions = this.reactions,
        version = this.version,
        clientMessageId = this.clientMessageId,
    ),
    readers = this.readers.map { reader ->
        reader.toEntity()
    }
)



fun MessageReaderEntity.toMessageReader(): MessageReader = MessageReader(
    readerEntryId = this.readerEntryId,
    messageId = this.messageId,
    readerId = this.readerID,
    readDate = this.readDate
)

/** Domain -> entity */
fun MessageReader.toEntity(): MessageReaderEntity = MessageReaderEntity(
    readerEntryId = this.readerEntryId,
    messageId = this.messageId,
    readerID = this.readerId,
    readDate = this.readDate
)

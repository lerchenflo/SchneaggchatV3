package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageReaderDto

data class MessageReader(
    val readerEntryId: Long = 0L,
    val messageId: Long = 0L,
    val readerId: Long = 0L,
    val readDate: String = ""
) {
    fun getReadDateAsLong(): Long = readDate.toLongOrNull() ?: 0L
}


fun MessageReaderDto.toMessageReader(): MessageReader = MessageReader(
    readerEntryId = this.readerEntryId,
    messageId = this.messageId,
    readerId = this.readerID,
    readDate = this.readDate
)

/** Domain -> DTO */
fun MessageReader.toDto(): MessageReaderDto = MessageReaderDto(
    readerEntryId = this.readerEntryId,
    messageId = this.messageId,
    readerID = this.readerId,
    readDate = this.readDate
)
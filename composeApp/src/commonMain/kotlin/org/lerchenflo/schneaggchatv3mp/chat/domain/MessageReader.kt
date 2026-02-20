package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageReaderDto

data class MessageReader(
    val readerEntryId: Long = 0L,
    val messageId: String,
    val readerId: String,
    val readDate: String = "", //TODO: Readdate as long

    //TODO: Pass reader names to show reader state in chat
    //val readerName: String? = n
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
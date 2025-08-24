package org.lerchenflo.schneaggchatv3mp.database.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.database.tables.Message
import org.lerchenflo.schneaggchatv3mp.database.tables.MessageReader
import org.lerchenflo.schneaggchatv3mp.database.tables.MessageWithReaders

@Serializable
data class ServerReaderDto(
    @SerialName("Id") val id: Long = 0,                   // server-assigned PK for the reader entry
    @SerialName("MessageId") val messageId: Long = 0,     // may be 0 or missing; converter will fallback to outer message id
    @SerialName("UserId") val userId: Long = 0,           // the user who read the message
    @SerialName("ReadTimestamp") val readTimestamp: String = "" // timestamp string
)

@Serializable
data class ServerMessageDto(
    @SerialName("id") val id: Long = 0,
    @SerialName("msgtype") val msgtype: String = "",
    @SerialName("inhalt") val inhalt: String = "",
    @SerialName("sender") val sender: Long = 0,
    @SerialName("empfaenger") val empfaenger: Long = 0,
    @SerialName("sendedatum") val sendedatum: String = "",
    @SerialName("geaendert") val geaendert: String = "",
    @SerialName("deleted") val deleted: Boolean = false,
    @SerialName("groupmessage") val groupmessage: Boolean = false,
    @SerialName("answerid") val answerid: Long = -1,
    @SerialName("Readers") val gelesenliste: List<ServerReaderDto> = emptyList()
)

fun convertServerMessageDtoToMessageWithReaders(serverList: List<ServerMessageDto>): List<MessageWithReaders> {
    return serverList.map { dto ->
        // create Message using the new property names/types
        val message = Message(
            id = dto.id,
            msgType = dto.msgtype,
            content = dto.inhalt,
            senderId = dto.sender,
            receiverId = dto.empfaenger,
            sendDate = dto.sendedatum,
            changeDate = dto.geaendert,
            deleted = dto.deleted,
            groupMessage = dto.groupmessage,
            answerId = dto.answerid,
            sent = true
        )

        // map readers; use server-assigned PK (r.id) as readerEntryId
        val readers: List<MessageReader> = dto.gelesenliste.map { r ->
            val resolvedMessageId = if (r.messageId.toInt() != 0) r.messageId.toLong() else dto.id.toLong()
            MessageReader(
                readerEntryId = 0/*r.id.toLong()*/,     // server PK for this reader record
                messageId = resolvedMessageId,     // FK -> message.id
                readerID = r.userId.toLong(),      // user who read the message
                readDate = r.readTimestamp
            )
        }

        MessageWithReaders(message = message, readers = readers)
    }
}

fun convertServerMessageDtoToMessageWithReaders(dto: ServerMessageDto): MessageWithReaders {
    val message = Message(
        id = dto.id,
        msgType = dto.msgtype,
        content = dto.inhalt,
        senderId = dto.sender,
        receiverId = dto.empfaenger,
        sendDate = dto.sendedatum,
        changeDate = dto.geaendert,
        deleted = dto.deleted,
        groupMessage = dto.groupmessage,
        answerId = dto.answerid,
        sent = true
    )

    val readers: List<MessageReader> = dto.gelesenliste.map { r ->
        val resolvedMessageId = if (r.messageId != 0L) r.messageId else dto.id
        MessageReader(
            readerEntryId = r.id,           // server PK for this reader record
            messageId = resolvedMessageId,  // FK -> message.id
            readerID = r.userId,            // user who read the message
            readDate = r.readTimestamp
        )
    }

    return MessageWithReaders(message = message, readers = readers)
}


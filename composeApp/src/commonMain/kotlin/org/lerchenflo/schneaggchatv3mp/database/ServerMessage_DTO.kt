package org.lerchenflo.schneaggchatv3mp.database

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//Data transaction objects die so direkt als json geparst werrand:

@kotlinx.serialization.Serializable
data class ServerReaderDto(
    @SerialName("id") val id: Long = 0L,
    @SerialName("changedate") val changedate: String? = null
)

@Serializable
data class ServerMessageDto(
    @SerialName("id") val id: Long = 0L,
    @SerialName("msgtype") val msgtype: String? = null,
    @SerialName("inhalt") val inhalt: String? = null,
    @SerialName("sender") val sender: Long = 0L,
    @SerialName("empfaenger") val empfaenger: Long = 0L,
    @SerialName("sendedatum") val sendedatum: String? = null,
    @SerialName("geaendert") val geaendert: String? = null,
    @SerialName("answerid") val answerid: Long = -1L,
    @SerialName("deleted") val deleted: Boolean = false,
    @SerialName("gelesenliste") val gelesenliste: List<ServerReaderDto> = emptyList()
)

fun convertServerMessageDtoToMessageWithReaders(serverList: List<ServerMessageDto>) : List<MessageWithReaders>{
    val batch: List<MessageWithReaders> = serverList.map { dto ->
        val message = Message(
            id = dto.id,
            msgType = dto.msgtype,
            content = dto.inhalt,
            sender = dto.sender,
            receiver = dto.empfaenger,
            changeDate = dto.geaendert,
            sendDate = dto.sendedatum,
            answerId = dto.answerid,
            deleted = dto.deleted
        )

        // map readers. Ensure messageId is set to message.id so FK is correct.
        val readers: List<MessageReader> = dto.gelesenliste.map { r ->
            // NOTE: adjust mapping below according to what the server's "id" actually means
            MessageReader(
                readerEntryId = 0,       // if server provides a unique entry id
                messageId = dto.id,         // FK -> message.id
                readerID = r.id,            // set if server supplies reader user id as different field
                readDate = r.changedate
            )
        }

        MessageWithReaders(message = message, readers = readers)
    }

    return batch
}
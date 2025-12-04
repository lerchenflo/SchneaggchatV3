package org.lerchenflo.schneaggchatv3mp.chat.domain

import androidx.compose.material3.rememberTimePickerState
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.relations.MessageWithReadersDto
import org.lerchenflo.schneaggchatv3mp.datasource.network.PICTUREMESSAGE

enum class MessageType {
    TEXT,
    IMAGE
}


data class Message(
    var localPK: Long = 0L,
    var id: String? = null,
    var msgType: MessageType,
    var content: String = "",
    var senderId: String,
    var receiverId: String,
    var sendDate: String = "",
    var changeDate: String = "",
    var deleted: Boolean = false,
    var groupMessage: Boolean = false,
    var answerId: String? = null,
    var sent: Boolean = false,
    var senderAsString: String = "",
    var senderColor: Int = 0,
    var readers : List<MessageReader>
) {
    fun isPicture(): Boolean = msgType == MessageType.IMAGE

    fun getSendDateAsLong(): Long = sendDate.toLongOrNull() ?: 0L

    fun isMyMessage(): Boolean {
        return try {
            senderId.toString() == SessionCache.getOwnIdValue().toString()
        } catch (_: Exception) {
            false
        }
    }

    fun isReadByMe(): Boolean =
        readers.any { it.readerId == SessionCache.getOwnIdValue() }

    fun isReadById(id: String): Boolean =
        readers.any { it.readerId == id }

    fun isGroupMessage(): Boolean = groupMessage

    /**
     * Was this message sent into this chat?
     * @param chatID id of the chat (group id or user id)
     * @param gruppe true if chatID refers to a group
     */
    fun isThisChatMessage(chatID: String, gruppe: Boolean): Boolean {
        return if (gruppe) {
            receiverId == chatID && groupMessage
        } else {
            if (isMyMessage() && receiverId == chatID && !groupMessage) {
                true
            } else if (senderId == chatID &&
                receiverId == SessionCache.getOwnIdValue() &&
                !groupMessage
            ) {
                true
            } else {
                false
            }
        }
    }
}


fun MessageWithReadersDto.toMessage(): Message = Message(
    localPK = this.messageDto.localPK,
    id = this.messageDto.id,
    msgType = this.messageDto.msgType,
    content = this.messageDto.content,
    senderId = this.messageDto.senderId,
    receiverId = this.messageDto.receiverId,
    sendDate = this.messageDto.sendDate,
    changeDate = this.messageDto.changedate,
    deleted = this.messageDto.deleted,
    groupMessage = this.messageDto.groupMessage,
    answerId = this.messageDto.answerId,
    sent = this.messageDto.sent,
    senderAsString = this.messageDto.senderAsString,
    senderColor = this.messageDto.senderColor,
    readers = this.readers.map { readerDto ->
        readerDto.toMessageReader()
    },
)

/** Domain -> DTO */
fun Message.toDto(): MessageWithReadersDto = MessageWithReadersDto(
    messageDto = MessageDto(
        localPK = this.localPK,
        id = this.id,
        msgType = this.msgType,
        content = this.content,
        senderId = this.senderId,
        receiverId = this.receiverId,
        sendDate = this.sendDate,
        changedate = this.changeDate,
        deleted = this.deleted,
        groupMessage = this.groupMessage,
        answerId = this.answerId,
        sent = this.sent
    ),
    readers = this.readers.map { reader ->
        reader.toDto()
    }
)
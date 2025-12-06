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
    var myMessage: Boolean,
    var readByMe: Boolean,
    var senderAsString: String = "",
    var senderColor: Int = 0,
    var readers : List<MessageReader>
) {
    fun isPicture(): Boolean = msgType == MessageType.IMAGE

    fun getSendDateAsLong(): Long = sendDate.toLongOrNull() ?: 0L

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
            if (myMessage && receiverId == chatID && !groupMessage) {
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

    override fun toString(): String {
        val contentPreview = if (content.length > 50) {
            content.take(50) + "..."
        } else {
            content
        }

        return buildString {
            appendLine("Message {")
            appendLine("  id: $id")
            appendLine("  type: $msgType")
            appendLine("  from: ${senderAsString.ifEmpty { senderId }}")
            appendLine("  to: $receiverId")
            appendLine("  content: '$contentPreview'")
            appendLine("  myMessage: $myMessage")
            appendLine("  readByMe: $readByMe")
            appendLine("  sent: $sent")
            appendLine("  groupMessage: $groupMessage")
            appendLine("  deleted: $deleted")
            appendLine("  readers: ${readers.size}")
            appendLine("  sendDate:\t\t $sendDate")
            appendLine("  lastChanged:\t $changeDate")

            if (answerId != null) {
                appendLine("  answerId: $answerId")
            }
            append("}")
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
    myMessage = this.messageDto.myMessage,
    readByMe = this.messageDto.readByMe,
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
        sent = this.sent,
        myMessage = this.myMessage,
        readByMe = this.readByMe
    ),
    readers = this.readers.map { reader ->
        reader.toDto()
    }
)
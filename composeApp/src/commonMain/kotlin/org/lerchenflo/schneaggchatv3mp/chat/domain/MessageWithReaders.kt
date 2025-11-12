package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageWithReadersDto

data class MessageWithReaders(
    val message: Message,
    val readers: List<MessageReader>
) {
    fun isReadByMe(): Boolean =
        readers.any { it.readerId == SessionCache.getOwnIdValue() }

    fun isReadById(id: String): Boolean =
        readers.any { it.readerId == id }

    fun isPicture(): Boolean =
        message.isPicture() // delegates to Message.isPicture()

    fun getSendDateAsLong(): Long =
        message.getSendDateAsLong() // delegates to Message.getSendDateAsLong()

    fun isMyMessage(): Boolean =
        message.isMyMessage() // delegates to Message.isMyMessage()

    fun isGroupMessage(): Boolean =
        message.groupMessage

    /**
     * Was this message sent into this chat?
     * @param chatID id of the chat (group id or user id)
     * @param gruppe true if chatID refers to a group
     */
    fun isThisChatMessage(chatID: String, gruppe: Boolean): Boolean {
        return if (gruppe) {
            message.receiverId == chatID && message.groupMessage
        } else {
            if (isMyMessage() && message.receiverId == chatID && !message.groupMessage) {
                true
            } else if (message.senderId == chatID &&
                message.receiverId == SessionCache.getOwnIdValue() &&
                !message.groupMessage
            ) {
                true
            } else {
                false
            }
        }
    }
}


fun MessageWithReadersDto.toMessageWithReaders(): MessageWithReaders =
    MessageWithReaders(
        message = this.messageDto.toMessage(),
        readers = this.readers.map { it.toMessageReader() }
    )

/** Domain -> DTO */
fun MessageWithReaders.toDto(): MessageWithReadersDto =
    MessageWithReadersDto(
        messageDto = this.message.toDto(),
        readers = this.readers.map { it.toDto() }
    )
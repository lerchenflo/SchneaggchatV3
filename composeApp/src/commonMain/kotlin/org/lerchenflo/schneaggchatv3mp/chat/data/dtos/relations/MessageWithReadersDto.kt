package org.lerchenflo.schneaggchatv3mp.chat.data.dtos.relations

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageReaderDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType

@Serializable
data class MessageWithReadersDto(
    @Embedded val messageDto: MessageDto,
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId"
    )
    val readers: List<MessageReaderDto>
){

    fun isReadById(id: String) : Boolean{
        return readers.any {it.readerID == id}
    }

    fun isPicture() : Boolean{
        return messageDto.msgType == MessageType.IMAGE
    }

    fun getSendDateAsLong(): Long {
        return messageDto.sendDate.toLong()
    }

    fun isGroupMessage(): Boolean {
        return messageDto.groupMessage
    }

    /**
     * Was this message sent into this chat?
     */
    fun isThisChatMessage(chatID: String, gruppe : Boolean) : Boolean {
        if (gruppe){
            if (messageDto.receiverId == chatID && messageDto.groupMessage){
                return true
            }
        }else {
            if (messageDto.myMessage && messageDto.receiverId == chatID && !messageDto.groupMessage){
                return true
            }else if (messageDto.senderId == chatID && messageDto.receiverId == SessionCache.getOwnIdValue() && !messageDto.groupMessage){
                return true
            }
        }
        return false
    }
}
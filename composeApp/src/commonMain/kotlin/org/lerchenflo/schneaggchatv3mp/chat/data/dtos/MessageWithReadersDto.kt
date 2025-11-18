package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.datasource.network.PICTUREMESSAGE

@Serializable
data class MessageWithReadersDto(
    @Embedded val messageDto: MessageDto,
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId"
    )
    val readers: List<MessageReaderDto>
){
    fun isReadbyMe() : Boolean{
        return readers.any{ it.readerID == SessionCache.getOwnIdValue() }
    }

    fun isReadById(id: String) : Boolean{
        return readers.any {it.readerID == id}
    }

    fun isPicture() : Boolean{
        return messageDto.msgType == PICTUREMESSAGE
    }

    fun getSendDateAsLong(): Long {
        return messageDto.sendDate.toLong()
    }

    fun isMyMessage(): Boolean {
        return messageDto.senderId == SessionCache.getOwnIdValue()
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
            if (isMyMessage() && messageDto.receiverId == chatID && !messageDto.groupMessage){
                return true
            }else if (messageDto.senderId == chatID && messageDto.receiverId == SessionCache.getOwnIdValue() && !messageDto.groupMessage){
                return true
            }
        }
        return false
    }
}
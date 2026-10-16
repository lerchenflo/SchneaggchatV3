package org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.relations

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.MessageEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.MessageReaderEntity
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType

@Serializable
data class MessageWithReaders(
    @Embedded val messageDto: MessageEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId"
    )
    val readers: List<MessageReaderEntity>
){

    fun isReadById(id: String) : Boolean{
        return readers.any {it.readerID == id}
    }

    fun isPicture() : Boolean{
        return messageDto.msgType == MessageType.IMAGE
    }

    fun getSendDateAsLong(): Long {
        return messageDto.sendDate
    }

    fun isGroupMessage(): Boolean {
        return messageDto.groupMessage
    }

    /**
     * Was this message sent into this chat?
     */
    fun isThisChatMessage(ownId: String, chatID: String, gruppe : Boolean) : Boolean {
        if (gruppe){
            if (messageDto.receiverId == chatID && messageDto.groupMessage){
                return true
            }
        }else {
            if (messageDto.myMessage && messageDto.receiverId == chatID && !messageDto.groupMessage){
                return true
            }else if (messageDto.senderId == chatID && messageDto.receiverId == ownId && !messageDto.groupMessage){
                return true
            }
        }
        return false
    }
}
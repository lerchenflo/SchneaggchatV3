package org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.relations

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.MessageEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.MessageReaderEntity
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType

@Serializable
data class MessageWithReaders(
    @Embedded val messageEntity: MessageEntity,
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
        return messageEntity.msgType == MessageType.IMAGE
    }

    fun getSendDateAsLong(): Long {
        return messageEntity.sendDate
    }

    fun isGroupMessage(): Boolean {
        return messageEntity.groupMessage
    }

    /**
     * Was this message sent into this chat?
     */
    fun isThisChatMessage(ownId: String, chatID: String, gruppe : Boolean) : Boolean {
        if (gruppe){
            if (messageEntity.receiverId == chatID && messageEntity.groupMessage){
                return true
            }
        }else {
            if (messageEntity.myMessage && messageEntity.receiverId == chatID && !messageEntity.groupMessage){
                return true
            }else if (messageEntity.senderId == chatID && messageEntity.receiverId == ownId && !messageEntity.groupMessage){
                return true
            }
        }
        return false
    }
}
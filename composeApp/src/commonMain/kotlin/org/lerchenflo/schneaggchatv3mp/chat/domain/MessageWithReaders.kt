package org.lerchenflo.schneaggchatv3mp.chat.domain

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.network.PICTUREMESSAGE

@Serializable
data class MessageWithReaders(
    @Embedded val message: Message,
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId"
    )
    val readers: List<MessageReader>
){
    fun isReadbyMe() : Boolean{
        return readers.any{ it.readerID == SessionCache.getOwnIdValue() }
    }

    fun isReadById(id: Long) : Boolean{
        return readers.any {it.readerID == id}
    }

    fun isPicture() : Boolean{
        return message.msgType == PICTUREMESSAGE
    }

    fun getSendDateAsLong(): Long {
        return message.sendDate?.toLongOrNull() ?: 0L
    }

    fun isMyMessage(): Boolean {
        return message.senderId == SessionCache.getOwnIdValue()
    }

    fun isGroupMessage(): Boolean {
        return message.groupMessage
    }
}
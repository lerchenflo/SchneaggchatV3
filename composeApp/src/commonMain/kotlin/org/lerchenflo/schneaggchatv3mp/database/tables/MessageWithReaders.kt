package org.lerchenflo.schneaggchatv3mp.database.tables

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.OWNID
import org.lerchenflo.schneaggchatv3mp.network.GROUPPICTUREMESSAGE
import org.lerchenflo.schneaggchatv3mp.network.SINGLEPICTUREMESSAGE

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
        return readers.any{ it.readerID == OWNID }
    }

    fun isReadById(id: Long) : Boolean{
        return readers.any {it.readerID == id}
    }

    fun isPicture() : Boolean{
        return message.msgType == SINGLEPICTUREMESSAGE || message.msgType == GROUPPICTUREMESSAGE
    }

    fun getSendDateAsLong(): Long {
        return message.sendDate?.toLongOrNull() ?: 0L
    }

    fun isMyMessage(): Boolean {
        return message.sender == OWNID
    }
}
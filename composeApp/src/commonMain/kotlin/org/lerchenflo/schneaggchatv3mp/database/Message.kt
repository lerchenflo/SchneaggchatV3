package org.lerchenflo.schneaggchatv3mp.database


import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
)



@Serializable
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey()
    var id: Long = 0L,

    @SerialName("msgtype")
    var msgType: String? = null,

    @SerialName("inhalt")
    var content: String? = null,

    @SerialName("sender")
    var sender: Long = 0L,

    @SerialName("empfaenger")
    var receiver: Long = 0L,

    @SerialName("geaendert")
    @ColumnInfo(name = "changedate")
    var changeDate: String? = null,

    @SerialName("sendedatum")
    var sendDate: String? = null,

    @SerialName("answerid")
    var answerId: Long = -1,
    var deleted: Boolean = false,

    @Ignore
    var senderAsString: String = "",

    @Ignore
    var senderColor: Int = 0,



){
    fun isPicture() : Boolean{
        return msgType == SINGLEPICTUREMESSAGE || msgType == GROUPPICTUREMESSAGE
    }

    fun getSendDateAsLong(): Long {
        return sendDate?.toLongOrNull() ?: 0L
    }
}
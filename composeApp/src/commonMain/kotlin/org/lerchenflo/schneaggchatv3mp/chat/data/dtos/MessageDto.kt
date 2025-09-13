package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.network.PICTUREMESSAGE

@Serializable
@Entity(
    tableName = "messages",
    indices = [Index(value = ["id"], unique = true)]
)
data class MessageDto(

    @PrimaryKey(autoGenerate = true)
    var localPK : Long = 0L,

    @SerialName("id")
    var id: Long? = null,

    @SerialName("msgtype")
    var msgType: String = "",

    @SerialName("inhalt")
    var content: String = "",

    @SerialName("sender")
    var senderId: Long = 0,

    @SerialName("empfaenger")
    var receiverId: Long = 0,

    @SerialName("sendedatum")
    var sendDate: String = "",

    @SerialName("geaendert")
    @ColumnInfo(name = "changedate")
    var changeDate: String = "",

    @SerialName("deleted")
    var deleted: Boolean = false,

    @SerialName("groupmessage")
    var groupMessage: Boolean = false,

    @SerialName("answerid")
    var answerId: Long = -1,

    @ColumnInfo(name = "sent")
    var sent: Boolean = false,

    @Ignore
    var senderAsString: String = "",

    @Ignore
    var senderColor: Int = 0,
) {


    fun isPicture(): Boolean {
        return msgType == PICTUREMESSAGE
    }

    fun getSendDateAsLong(): Long {
        return sendDate.toLongOrNull() ?: 0L
    }

    fun isMyMessage(): Boolean {
        // compare as strings to be robust to OWNID being Int/Long/String
        return try {
            senderId.toString() == SessionCache.getOwnIdValue().toString()
        } catch (_: Exception) {
            false
        }
    }
}

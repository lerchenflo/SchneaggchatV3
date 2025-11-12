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
    var id: String? = null,

    @SerialName("msgtype")
    var msgType: String = "",

    @SerialName("inhalt")
    var content: String = "",

    @SerialName("sender")
    var senderId: String,

    @SerialName("empfaenger")
    var receiverId: String,

    @SerialName("sendedatum")
    var sendDate: String = "",

    @ColumnInfo(name = "changedate")
    var changeDate: String = "",

    @SerialName("deleted")
    var deleted: Boolean = false,

    @SerialName("groupmessage")
    var groupMessage: Boolean = false,

    @SerialName("answerid")
    var answerId: String? = null,

    @ColumnInfo(name = "sent")
    var sent: Boolean = false,

) {
    @Ignore
    var senderAsString: String = ""

    @Ignore
    var senderColor: Int = 0

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

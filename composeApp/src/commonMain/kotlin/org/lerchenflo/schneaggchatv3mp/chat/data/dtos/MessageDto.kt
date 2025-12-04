package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.network.PICTUREMESSAGE

@Serializable
@Entity(
    tableName = "messages",
    indices = [Index(value = ["id"], unique = true)]
)
data class MessageDto(

    @PrimaryKey(autoGenerate = true)
    var localPK : Long = 0L,

    var id: String? = null,

    var msgType: MessageType,

    var content: String = "",

    var senderId: String,

    var receiverId: String,

    var sendDate: String = "",

    var changedate: String = "",

    var deleted: Boolean = false,

    var groupMessage: Boolean = false,

    var answerId: String? = null,

    var sent: Boolean = false,

    ) {
    @Ignore
    var senderAsString: String = ""

    @Ignore
    var senderColor: Int = 0

    fun isMyMessage(): Boolean {
        // compare as strings to be robust to OWNID being Int/Long/String
        return try {
            senderId.toString() == SessionCache.getOwnIdValue().toString()
        } catch (_: Exception) {
            false
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage

@Serializable
@Entity(
    tableName = "messages",
    indices = [Index(value = ["id"], unique = true), Index(value = ["myMessage", "groupMessage", "sent", "readByMe"])]
)
data class MessageDto(

    @PrimaryKey(autoGenerate = true)
    var localPK : Long = 0L,

    var id: String? = null,

    var msgType: MessageType,

    var content: String = "",
    var poll: PollMessage? = null,

    var senderId: String,

    var receiverId: String,

    var sendDate: String = "",

    var updatedAt: String = "",

    var deleted: Boolean = false,

    var myMessage: Boolean, //Keep track if this is a message sent by me or not
    var readByMe: Boolean,


    var groupMessage: Boolean = false,

    var answerId: String? = null,

    var sent: Boolean = false,

    ) {
    @Ignore
    var senderAsString: String = ""

    @Ignore
    var senderColor: Int = 0

}

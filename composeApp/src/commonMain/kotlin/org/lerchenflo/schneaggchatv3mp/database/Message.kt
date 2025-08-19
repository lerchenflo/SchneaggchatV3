package org.lerchenflo.schneaggchatv3mp.database


import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.database.userdatabase.MessageReader


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

    var msgType: String? = null,
    var content: String? = null,
    var sender: Long = 0L,
    var receiver: Long = 0L,
    var changeDate: String? = null,
    var sendDate: String? = null,
    var answerId: Long = -1,
    var groupMessage: Boolean = false,
    var deleted: Boolean = false,

    // not persisted in DB if you don’t want them stored → mark with @Ignore
    @Ignore
    var senderAsString: String = "",

    @Ignore
    var senderColor: Int = 0
)
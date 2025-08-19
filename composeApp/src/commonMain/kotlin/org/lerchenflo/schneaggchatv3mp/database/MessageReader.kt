package org.lerchenflo.schneaggchatv3mp.database.userdatabase

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.database.Message

@Entity(
    tableName = "message_readers",
    foreignKeys = [
        ForeignKey(
            entity = Message::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ]
)

@Serializable
data class MessageReader(
    @PrimaryKey()
    val readerEntryId: Long = 0L,

    val messageId: Long,     // FK → Message.id
    val readerID: Long?,
    val readDate: String?
) {
    fun getReadDateAsLong(): Long = readDate?.toLongOrNull() ?: 0L
}
package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "message_readers",
    foreignKeys = [
        ForeignKey(
            entity = Message::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [Index(value = ["messageId"])]
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
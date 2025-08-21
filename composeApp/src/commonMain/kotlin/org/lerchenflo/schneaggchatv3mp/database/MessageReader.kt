package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/*
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

 */

@Serializable
@Entity(tableName = "message_readers")
data class MessageReader(
    @PrimaryKey(autoGenerate = true)
    val readerEntryId: Long = 0L,
    val messageId: Long,
    val readerID: Long,
    val readDate: String?
) {
    fun getReadDateAsLong(): Long = readDate?.toLongOrNull() ?: 0L
}
package org.lerchenflo.schneaggchatv3mp.chat.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "message_readers",
    indices = [Index(value = ["messageId"])]
)
data class MessageReaderEntity(
    @PrimaryKey(autoGenerate = true)
    val readerEntryId: Long = 0L,
    val messageId: String,
    val readerID: String,
    val readDate: String
) {
    fun getReadDateAsLong(): Long = readDate.toLongOrNull() ?: 0L
}
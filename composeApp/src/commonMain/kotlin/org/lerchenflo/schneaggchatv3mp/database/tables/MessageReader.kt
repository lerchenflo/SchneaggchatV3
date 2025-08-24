package org.lerchenflo.schneaggchatv3mp.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "message_readers")
data class MessageReader(
    @PrimaryKey(autoGenerate = true)
    val readerEntryId: Long = 0L,
    val messageId: Long,
    val readerID: Long,
    val readDate: String
) {
    fun getReadDateAsLong(): Long = readDate.toLongOrNull() ?: 0L
}
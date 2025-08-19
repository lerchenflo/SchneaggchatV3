package org.lerchenflo.schneaggchatv3mp.database.userdatabase

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.database.Message
import org.lerchenflo.schneaggchatv3mp.database.MessageWithReaders

@Dao
interface MessageDao {

    @Upsert()
    suspend fun updateMessage(message: Message): Long


    @Transaction
    @Query("SELECT * FROM messages WHERE id = :id")
    fun getMessageWithReaders(id: Long): Flow<MessageWithReaders>

    @Transaction
    @Query("SELECT * FROM messages")
    fun getAllMessagesWithReaders(): Flow<List<MessageWithReaders>>
}
package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Dao
interface UserDao {

    @Upsert()
    suspend fun upsert(user: User) //Suspend: Async mit warten

    @Query("DELETE FROM users WHERE id = :userid")
    suspend fun delete(userid: Long)

    @Query("SELECT * FROM users WHERE name LIKE '%' || :searchterm || '%'")
    fun getallusers(searchterm: String = ""): Flow<List<User>>


    @Query("SELECT id, changedate FROM users")
    suspend fun getUserIdsWithChangeDates(): List<IdChangeDate>

}


@Dao
interface MessageDao {

    @Upsert()
    suspend fun updateMessage(message: Message): Long

    @Upsert
    suspend fun updateMessages(messages: List<Message>)


    @Transaction
    @Query("SELECT * FROM messages WHERE id = :id")
    fun getMessageWithReaders(id: Long): Flow<MessageWithReaders>


    @Transaction
    @Query("SELECT * FROM messages")
    fun getAllMessagesWithReaders(): Flow<List<MessageWithReaders>>

    @Query("SELECT id, changedate FROM messages")
    suspend fun getMessageIdsWithChangeDates(): List<IdChangeDate>
}

@Dao
interface MessageReaderDao {

    @Upsert()
    suspend fun upsertReader(reader: MessageReader): Long

    @Upsert
    suspend fun upsertReaders(readers: List<MessageReader>): List<Long>

    @Query("DELETE FROM message_readers WHERE messageId = :messageId")
    suspend fun deleteReadersForMessage(messageId: Long)
}

@Serializable
data class IdChangeDate(
    val id: Long,
    val changedate: String
)

@Serializable
data class IdOperation(val Status: String = "", val Id: Long = 0L)


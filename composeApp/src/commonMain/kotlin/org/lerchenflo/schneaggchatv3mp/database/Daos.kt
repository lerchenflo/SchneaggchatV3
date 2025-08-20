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

    @Upsert
    suspend fun upsert(user: User) //Suspend: Async mit warten

    @Query("DELETE FROM users WHERE id = :userid")
    suspend fun delete(userid: Long)

    @Query("SELECT * FROM users WHERE name LIKE '%' || :searchterm || '%'")
    fun getallusers(searchterm: String = ""): Flow<List<User>>


    @Query("SELECT id, changedate FROM users")
    fun getUserIdsWithChangeDates(): List<IdChangeDate?>?
}


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

@Serializable
data class IdChangeDate(
    val id: Long,
    val changedate: String
)

@Serializable
data class IdOperation(val Status: String = "", val Id: Long = 0L)


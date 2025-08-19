package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Upsert
    suspend fun upsert(user: User) //Suspend: Async mit warten

    @Query("SELECT * FROM users WHERE name LIKE '%' || :searchterm || '%'")
    fun getallusers(searchterm: String = ""): Flow<List<User>>


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

data class UserWithLastMessage(
    val user: User,
    val lastMessage: Message?
)
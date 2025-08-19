package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Upsert
    suspend fun upsert(user: User) //Suspend: Async mit warten

    @Query("SELECT * FROM users WHERE name LIKE '%' || :searchterm || '%'")
    fun getallusers(searchterm: String = ""): Flow<List<User>>


}

data class UserWithLastMessage(
    val user: User,
    val lastMessage: Message?
)
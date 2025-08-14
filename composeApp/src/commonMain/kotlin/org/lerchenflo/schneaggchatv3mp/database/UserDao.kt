package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Upsert
    suspend fun upsert(user: User) //SUspend: Async

    @Query("SELECT * FROM users")
    fun getallusers(): Flow<List<User>>


}
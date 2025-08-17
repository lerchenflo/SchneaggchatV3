package org.lerchenflo.schneaggchatv3mp.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

class UserDatabaseRepository(
    private val database: UserDatabase
) {
    private val dispatcher = Dispatchers.IO

    suspend fun upsertUser(user: User){
        with(dispatcher){
            database.userDao().upsert(user)
        }
    }

    fun getallusers(): Flow<List<User>>{
        return database.userDao().getallusers()
    }

}
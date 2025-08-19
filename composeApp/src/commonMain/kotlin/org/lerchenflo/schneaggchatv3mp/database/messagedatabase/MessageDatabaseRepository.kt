package org.lerchenflo.schneaggchatv3mp.database.userdatabase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.database.messagedatabase.UserDatabase

class UserDatabaseRepository(
    private val database: UserDatabase
) {
    private val dispatcher = Dispatchers.IO

    suspend fun upsertUser(user: User){
        with(dispatcher){
            database.userDao().upsert(user)
        }
    }

    fun getallusers(searchterm: String = ""): Flow<List<User>>{
        return database.userDao().getallusers(searchterm)
    }

}
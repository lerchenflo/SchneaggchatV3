package org.lerchenflo.schneaggchatv3mp.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

class AppDatabaseRepository(
    private val database: AppDatabase
) {
    private val dispatcher = Dispatchers.IO

    suspend fun upsertUser(user: User){
        with(dispatcher){
            database.userDao().upsert(user)
        }
    }

    suspend fun deleteUser(userid: Long){
        with(dispatcher){
            database.userDao().delete(userid)
        }
    }

    fun getallusers(searchterm: String = ""): Flow<List<User>>{
        return database.userDao().getallusers(searchterm)
    }

    fun getchangeid(): List<IdChangeDate?>?{
        return database.userDao().getUserIdsWithChangeDates()
    }



}
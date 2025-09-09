package org.lerchenflo.schneaggchatv3mp.chat.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager

class UserRepository(
    private val database: AppDatabase,
    private val networkUtils: NetworkUtils,
    private val pictureManager: PictureManager
) {

    @Transaction
    suspend fun upsertUser(user: User){
        val savefilename = user.id.toString() + USERPROFILEPICTURE_FILE_NAME
        pictureManager.savePictureToStorage(user.profilePicture, savefilename)
        user.profilePicture = savefilename
        database.userDao().upsert(user)
    }


    suspend fun deleteUser(userid: Long){
        database.userDao().delete(userid)
    }

    fun getallusers(searchterm: String = ""): Flow<List<User>>{
        return database.userDao().getallusers(searchterm)
    }

    @Transaction
    suspend fun getuserchangeid(): List<IdChangeDate>{
        return database.userDao().getUserIdsWithChangeDates()
    }

}
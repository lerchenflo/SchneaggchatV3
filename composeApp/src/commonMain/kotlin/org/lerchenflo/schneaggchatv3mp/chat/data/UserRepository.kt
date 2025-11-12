package org.lerchenflo.schneaggchatv3mp.chat.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.toUser
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
    suspend fun upsertUser(userDto: UserDto){
        val savefilename = userDto.id.toString() + USERPROFILEPICTURE_FILE_NAME
        val path = pictureManager.savePictureToStorage(userDto.profilePicture, savefilename)
        userDto.profilePicture = path
        database.userDao().upsert(userDto)
    }


    suspend fun deleteUser(userid: String){
        database.userDao().delete(userid)
    }

    fun getallusers(searchterm: String = ""): Flow<List<User>>{
        return database.userDao().getallusers(searchterm).map { users ->
            users.map { user ->
                user.toUser()
            }
        }
    }

    @Transaction
    suspend fun getuserchangeid(): List<IdChangeDate>{
        return database.userDao().getUserIdsWithChangeDates()
    }

}
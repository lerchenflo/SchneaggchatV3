package org.lerchenflo.schneaggchatv3mp.chat.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.toUser
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager

class UserRepository(
    private val database: AppDatabase,
    private val pictureManager: PictureManager
) {

    @Transaction
    suspend fun upsertUser(userDto: UserDto){
        database.userDao().upsert(userDto)
    }


    suspend fun deleteUser(userid: String){
        database.userDao().delete(userid)
    }

    fun getAllUsersFlow(searchTerm: String = ""): Flow<List<User>>{
        return database.userDao().getAllUsersFlow(searchTerm).map { users ->
            users.map { user ->
                user.toUser()
            }
        }
    }

    suspend fun getAllUsers() : List<User> {
        return database.userDao().getAllUsers().map {
            it.toUser()
        }
    }

    @Transaction
    suspend fun getuserchangeid(): List<IdChangeDate>{
        return database.userDao().getUserIdsWithChangeDates()
    }

    suspend fun getUserById(id: String) : User? {
        return database.userDao().getUserbyId(id)?.toUser()
    }

    fun getUserFlow(id: String): Flow<User?> {
        return database.userDao().getUserbyIdFlow(id).map { it?.toUser() }
    }


    suspend fun updateUserProfilePicUrl(userId: String, newUrl: String) {
        val dbUser = database.userDao().getUserbyId(userId)
        if (dbUser != null){
            database.userDao().upsert(dbUser.copy(
                profilePictureUrl = newUrl
            ))
        }
    }

    suspend fun updateUserLocations(locations: List<NetworkUtils.UserLocationResponse>, ownLocation: LatLong) {
        locations.forEach { location ->
            val dbUser = database.userDao().getUserbyId(location.userId)
            if (dbUser != null) {
                println("SCHNEAGGMAP: Updating location for user ${dbUser.name}: $location")
                database.userDao().upsert(dbUser.copy(
                    locationLat = location.coordinates.lat,
                    locationLong = location.coordinates.long,
                    locationDate = location.locationTime
                ))
            }
        }
    }

}
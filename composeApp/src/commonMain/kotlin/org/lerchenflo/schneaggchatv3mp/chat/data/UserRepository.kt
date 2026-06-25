package org.lerchenflo.schneaggchatv3mp.chat.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.toUser
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.FriendLocationPayload
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

    /** Applies a single friend's live location push (`FriendLocationChange`) to the local DB. */
    suspend fun updateFriendLocation(payload: FriendLocationPayload) {
        val dbUser = database.userDao().getUserbyId(payload.userId)
        if (dbUser != null) {
            database.userDao().upsert(dbUser.copy(
                locationLat = payload.coordinates.lat,
                locationLong = payload.coordinates.long,
                locationDate = payload.locationTime,
                locationSpeed = payload.speed,
                locationHeading = payload.heading,
                locationAltitude = payload.altitude,
                locationBattery = payload.batteryLevel,
                locationDistance24h = payload.distanceTraveled24h,
            ))
        }
    }

    /** Applies the initial `FriendLocationsSnapshot` (pushed once on socket connect). */
    suspend fun updateFriendLocations(payloads: List<FriendLocationPayload>) {
        payloads.forEach { updateFriendLocation(it) }
    }

}
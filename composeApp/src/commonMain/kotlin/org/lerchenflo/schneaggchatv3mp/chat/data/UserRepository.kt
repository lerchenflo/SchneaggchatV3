package org.lerchenflo.schneaggchatv3mp.chat.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.SnailTrailPointDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.SnailTrailPoint
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
        database.snailTrailDao().deletePointsForUser(userid) // Child table first
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
    @Transaction
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

        // The server sends its currently-retained trail window every time - so we just replace
        // ours wholesale rather than trying to merge/dedupe incrementally.
        database.snailTrailDao().deletePointsForUser(payload.userId)
        if (payload.snailTrail.isNotEmpty()) {
            database.snailTrailDao().upsertPoints(
                payload.snailTrail.map {
                    SnailTrailPointDto(
                        userId = payload.userId,
                        lat = it.coordinates.lat,
                        long = it.coordinates.long,
                        locationTime = it.locationTime,
                        speed = it.speed,
                        heading = it.heading,
                    )
                }
            )
        }
    }

    /** Applies the initial `FriendLocationsSnapshot` (pushed once on socket connect). */
    suspend fun updateFriendLocations(payloads: List<FriendLocationPayload>) {
        payloads.forEach { updateFriendLocation(it) }
    }

    fun getSnailTrailFlow(userId: String): Flow<List<SnailTrailPoint>> {
        return database.snailTrailDao().getPointsForUserFlow(userId).map { points ->
            points.map { it.toSnailTrailPoint() }
        }
    }

    suspend fun getSnailTrail(userId: String): List<SnailTrailPoint> {
        return database.snailTrailDao().getPointsForUser(userId).map { it.toSnailTrailPoint() }
    }

}

private fun SnailTrailPointDto.toSnailTrailPoint() = SnailTrailPoint(
    lat = lat,
    long = long,
    locationTime = locationTime,
    speed = speed,
    heading = heading,
)
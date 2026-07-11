package org.lerchenflo.schneaggchatv3mp.chat.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.SnailTrailPointDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.SnailTrailPoint
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.toUser
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.FriendLocationPayload
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.FriendLocationSnapshotEntry
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SnailTrailPointPayload
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisLong

class UserRepository(
    private val database: AppDatabase,
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

    /** Applies a single friend's live location push (`FriendLocationChange`) to the local DB.
     * Position only - the trail is never touched here, it only grows via [appendSnailTrailPoint]. */
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

    /**
     * Applies the initial `FriendLocationsSnapshot` (pushed once on socket connect): seeds each
     * friend's position and replaces their local trail wholesale with the server's
     * currently-retained window. This is the only place the trail is replaced rather than
     * appended - further points arrive one at a time via [appendSnailTrailPoint].
     */
    @Transaction
    suspend fun updateFriendLocations(entries: List<FriendLocationSnapshotEntry>) {
        entries.forEach { entry ->
            updateFriendLocation(entry.position)

            val userId = entry.position.userId
            database.snailTrailDao().deletePointsForUser(userId)
            if (entry.snailTrail.isNotEmpty()) {
                database.snailTrailDao().upsertPoints(
                    entry.snailTrail.map {
                        SnailTrailPointDto(
                            userId = userId,
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
    }

    /** Appends one new snail-trail point (`SnailTrailPointAdded`, ~once/minute) without touching
     * any existing points. */
    suspend fun appendSnailTrailPoint(userId: String, point: SnailTrailPointPayload) {
        database.snailTrailDao().upsertPoints(listOf(
            SnailTrailPointDto(
                userId = userId,
                lat = point.coordinates.lat,
                long = point.coordinates.long,
                locationTime = point.locationTime,
                speed = point.speed,
                heading = point.heading,
            )
        ))
    }

    fun getSnailTrailFlow(userId: String): Flow<List<SnailTrailPoint>> {
        return database.snailTrailDao().getPointsForUserFlow(userId).map { points ->
            points.map { it.toSnailTrailPoint() }
        }
    }

    suspend fun getSnailTrail(userId: String): List<SnailTrailPoint> {
        return database.snailTrailDao().getPointsForUser(userId).map { it.toSnailTrailPoint() }
    }

    // --------------------- live online/offline presence (in-memory only, never persisted) ---------------------
    // A friend's "online right now" flag is transient runtime state: it always starts unknown/offline on
    // cold start and only becomes meaningful once the socket (re)connects and the server re-announces who's
    // online. Persisting it to Room would let a stale "online" survive an app kill or crash.
    private val _onlineFriendIds = MutableStateFlow<Set<String>>(emptySet())
    val onlineFriendIdsFlow: StateFlow<Set<String>> = _onlineFriendIds.asStateFlow()

    fun isFriendOnlineFlow(userId: String): Flow<Boolean> =
        onlineFriendIdsFlow.map { userId in it }

    fun setFriendOnline(userId: String, online: Boolean) {
        _onlineFriendIds.update { current -> if (online) current + userId else current - userId }
    }

    /** Best-effort local timestamp for the moment a friend went offline. The next [dataSync][org.lerchenflo.schneaggchatv3mp.datasource.AppRepository.dataSync]
     * will overwrite this with the server's authoritative `lastSeen`. */
    suspend fun updateUserLastSeenNow(userId: String) {
        val dbUser = database.userDao().getUserbyId(userId)
        if (dbUser != null) {
            database.userDao().upsert(dbUser.copy(
                lastSeen = getCurrentTimeMillisLong()
            ))
        }
    }

    fun setOnlineFriendIds(ids: Set<String>) {
        _onlineFriendIds.value = ids
    }

    fun clearOnlineFriends() {
        _onlineFriendIds.value = emptySet()
    }

}

private fun SnailTrailPointDto.toSnailTrailPoint() = SnailTrailPoint(
    lat = lat,
    long = long,
    locationTime = locationTime,
    speed = speed,
    heading = heading,
)
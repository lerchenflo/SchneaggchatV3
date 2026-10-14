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
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.toUser
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.FriendLocationPayload
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.FriendLocationSnapshotEntry
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SnailTrailPointPayload
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisLong

class RoomUserDataSource(
    private val database: AppDatabase,
) : UserLocalDataSource {

    @Transaction
    override suspend fun upsertUser(userDto: UserDto){
        database.userDao().upsert(userDto)
    }


    override suspend fun deleteUser(userid: String){
        database.snailTrailDao().deletePointsForUser(userid) // Child table first
        database.userDao().delete(userid)
    }

    override fun getAllUsersFlow(searchTerm: String): Flow<List<User>>{
        return database.userDao().getAllUsersFlow(searchTerm).map { users ->
            users.map { user ->
                user.toUser()
            }
        }
    }

    override suspend fun getAllUsers() : List<User> {
        return database.userDao().getAllUsers().map {
            it.toUser()
        }
    }

    @Transaction
    override suspend fun getuserchangeid(): List<IdChangeDate>{
        return database.userDao().getUserIdsWithChangeDates()
    }

    override suspend fun getUserById(id: String) : User? {
        return database.userDao().getUserbyId(id)?.toUser()
    }

    override fun getUserFlow(id: String): Flow<User?> {
        return database.userDao().getUserbyIdFlow(id).map { it?.toUser() }
    }


    override suspend fun updateUserProfilePicUrl(userId: String, newUrl: String) {
        val dbUser = database.userDao().getUserbyId(userId)
        if (dbUser != null){
            database.userDao().upsert(dbUser.copy(
                profilePictureUrl = newUrl
            ))
        }
    }

    override suspend fun updateFriendLocation(payload: FriendLocationPayload) {
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

    @Transaction
    override suspend fun updateFriendLocations(entries: List<FriendLocationSnapshotEntry>) {
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

    override suspend fun appendSnailTrailPoint(userId: String, point: SnailTrailPointPayload) {
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

    override fun getSnailTrailFlow(userId: String): Flow<List<SnailTrailPoint>> {
        return database.snailTrailDao().getPointsForUserFlow(userId).map { points ->
            points.map { it.toSnailTrailPoint() }
        }
    }

    override suspend fun getSnailTrail(userId: String): List<SnailTrailPoint> {
        return database.snailTrailDao().getPointsForUser(userId).map { it.toSnailTrailPoint() }
    }

    // --------------------- live online/offline presence (in-memory only, never persisted) ---------------------
    // A friend's "online right now" flag is transient runtime state: it always starts unknown/offline on
    // cold start and only becomes meaningful once the socket (re)connects and the server re-announces who's
    // online. Persisting it to Room would let a stale "online" survive an app kill or crash.
    private val _onlineFriendIds = MutableStateFlow<Set<String>>(emptySet())
    override val onlineFriendIdsFlow: StateFlow<Set<String>> = _onlineFriendIds.asStateFlow()

    override fun isFriendOnlineFlow(userId: String): Flow<Boolean> =
        onlineFriendIdsFlow.map { userId in it }

    override fun setFriendOnline(userId: String, online: Boolean) {
        _onlineFriendIds.update { current -> if (online) current + userId else current - userId }
    }

    override suspend fun updateUserLastSeenNow(userId: String) {
        val dbUser = database.userDao().getUserbyId(userId)
        if (dbUser != null) {
            database.userDao().upsert(dbUser.copy(
                lastSeen = getCurrentTimeMillisLong()
            ))
        }
    }

    override fun setOnlineFriendIds(ids: Set<String>) {
        _onlineFriendIds.value = ids
    }

    override fun clearOnlineFriends() {
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
package org.lerchenflo.schneaggchatv3mp.chat.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.UserEntity
import org.lerchenflo.schneaggchatv3mp.chat.domain.SnailTrailPoint
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.FriendLocationPayload
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.FriendLocationSnapshotEntry
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SnailTrailPointPayload

/**
 * Local (Room) storage for users, their snail trails and the in-memory online presence of friends.
 */
interface UserLocalDataSource {

    suspend fun upsertUser(userDto: UserEntity)

    suspend fun deleteUser(userid: String)

    fun getAllUsersFlow(searchTerm: String = ""): Flow<List<User>>

    suspend fun getAllUsers(): List<User>

    suspend fun getuserchangeid(): List<IdChangeDate>

    suspend fun getUserById(id: String): User?

    fun getUserFlow(id: String): Flow<User?>

    suspend fun updateUserProfilePicUrl(userId: String, newUrl: String)

    /** Applies a single friend's live location push (`FriendLocationChange`) to the local DB.
     * Position only - the trail is never touched here, it only grows via [appendSnailTrailPoint]. */
    suspend fun updateFriendLocation(payload: FriendLocationPayload)

    /**
     * Applies the initial `FriendLocationsSnapshot` (pushed once on socket connect): seeds each
     * friend's position and replaces their local trail wholesale with the server's
     * currently-retained window. This is the only place the trail is replaced rather than
     * appended - further points arrive one at a time via [appendSnailTrailPoint].
     */
    suspend fun updateFriendLocations(entries: List<FriendLocationSnapshotEntry>)

    /** Appends one new snail-trail point (`SnailTrailPointAdded`, ~once/minute) without touching
     * any existing points. */
    suspend fun appendSnailTrailPoint(userId: String, point: SnailTrailPointPayload)

    fun getSnailTrailFlow(userId: String): Flow<List<SnailTrailPoint>>

    suspend fun getSnailTrail(userId: String): List<SnailTrailPoint>

    /** Ids of the friends that are online right now. In-memory only, never persisted. */
    val onlineFriendIdsFlow: StateFlow<Set<String>>

    fun isFriendOnlineFlow(userId: String): Flow<Boolean>

    fun setFriendOnline(userId: String, online: Boolean)

    /** Best-effort local timestamp for the moment a friend went offline. The next [dataSync][org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncCoordinator.dataSync]
     * will overwrite this with the server's authoritative `lastSeen`. */
    suspend fun updateUserLastSeenNow(userId: String)

    fun setOnlineFriendIds(ids: Set<String>)

    fun clearOnlineFriends()
}

package org.lerchenflo.schneaggchatv3mp.datasource.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.AvailableUser
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.SnailTrailPoint
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.User
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.WakeResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

/**
 * Users and friends: reads come from the local database, writes go to the server first.
 * Errors of the write calls are sent to the [ErrorChannel] (except [sendWake], which returns them).
 */
interface UserRepository {

    // ─── Reads (local database) ───────────────────────────────────────────────

    fun getUserByIdFlow(userId: String): Flow<User?>

    suspend fun getUserById(userId: String): User?

    /** Every stored user (friends, pending requests and the own user), filtered by name. */
    fun getAllUsersFlow(searchTerm: String = ""): Flow<List<User>>

    /** Accepted friends whose name contains [searchTerm]. */
    suspend fun getFriends(searchTerm: String): List<User>

    /** Accepted friends whose name matches [searchTerm]. */
    fun getFriendsFlow(searchTerm: String): Flow<List<User>>

    fun getSnailTrailFlow(userId: String): Flow<List<SnailTrailPoint>>

    /** Ids of the friends that are online right now. In-memory only, never persisted. */
    val onlineFriendIdsFlow: StateFlow<Set<String>>

    // ─── Sync / pictures ──────────────────────────────────────────────────────

    /** Pulls changed and deleted users from the server into the local database (incl. the own user's settings). */
    suspend fun syncUsers()

    /** Downloads the profile pictures of [userIds] and stores their local paths. */
    suspend fun getProfilePicturesForUserIds(userIds: List<String>)

    /** Downloads every stored user's profile picture whose file is missing locally. */
    suspend fun fetchMissingProfilePictures()

    // ─── Friends ──────────────────────────────────────────────────────────────

    /** Users the own user can send a friend request to. Empty on error. */
    suspend fun getAvailableUsers(searchTerm: String): List<AvailableUser>

    /** Sends a friend request to [friendId], or accepts theirs. @return true on success */
    suspend fun sendFriendRequest(friendId: String): Boolean

    /** Denies a friend request from [friendId], or cancels an outgoing one. @return true on success */
    suspend fun denyFriendRequest(friendId: String): Boolean

    /** @return true on success */
    suspend fun removeFriend(friendId: String): Boolean

    // ─── Profile ──────────────────────────────────────────────────────────────

    /** @return true on success */
    suspend fun changeUsername(newUsername: String): Boolean

    /** @return true on success */
    suspend fun changeProfilePic(newPic: ByteArray): Boolean

    /**
     * Changes profile details. For the own [userId] the own fields (status, email, ...), for a
     * friend the per-friend fields (description, nickname). @return true on success
     */
    suspend fun changeUserDetails(
        userId: String,
        newStatus: String? = null,
        newDescription: String? = null,
        newEmail: String? = null,
        newBirthDate: String? = null,
        newNickName: String? = null,
        newPhoneNumber: String? = null,
    ): Boolean

    // ─── Location sharing ─────────────────────────────────────────────────────

    /** Per-friend toggle: what we share with this specific friend. @return true on success */
    suspend fun setLocationSharing(
        friendId: String,
        share: Boolean,
        shareSpeedHeading: Boolean = false,
        snailTrail: Boolean = false,
    ): Boolean

    /** Disables location sharing towards every friend. @return true if every call succeeded */
    suspend fun disableLocationSharingForAllFriends(): Boolean

    // ─── Wake ─────────────────────────────────────────────────────────────────

    /** Per-friend toggle: may this friend wake us. @return true on success */
    suspend fun setWakePermission(friendId: String, allowWake: Boolean): Boolean

    /** Master switch: while off nobody can wake us. @return true on success */
    suspend fun setWakeGlobal(enabled: Boolean): Boolean

    /**
     * Asks the server to wake a friend or a group. A successful result can still report that
     * nobody was reachable (see [WakeResult.outcome]). Errors are returned, not sent to the
     * [ErrorChannel], because the caller needs to tell "not friends" (403) apart from a plain
     * network failure.
     */
    suspend fun sendWake(
        targetId: String,
        isGroup: Boolean,
        reason: String,
    ): NetworkResult<WakeResult, NetworkingError>
}

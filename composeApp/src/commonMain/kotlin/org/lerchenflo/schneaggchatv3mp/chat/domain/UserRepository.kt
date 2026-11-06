package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult

/**
 * Users and friends: reads come from the local database, writes go to the server first.
 * Write calls return their error, the caller maps it to UI.
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

    /** Users the own user can send a friend request to. */
    suspend fun getAvailableUsers(searchTerm: String): DataResult<List<AvailableUser>, DataError>

    /** Sends a friend request to [friendId], or accepts theirs. */
    suspend fun sendFriendRequest(friendId: String): EmptyDataResult<DataError>

    /** Denies a friend request from [friendId], or cancels an outgoing one. */
    suspend fun denyFriendRequest(friendId: String): EmptyDataResult<DataError>
    suspend fun removeFriend(friendId: String): EmptyDataResult<DataError>

    // ─── Profile ──────────────────────────────────────────────────────────────
    suspend fun changeUsername(newUsername: String): EmptyDataResult<DataError>
    suspend fun changeProfilePic(newPic: ByteArray): EmptyDataResult<DataError>

    /**
     * Changes profile details. For the own [userId] the own fields (status, email, ...), for a
     * friend the per-friend fields (description, nickname).
     */
    suspend fun changeUserDetails(
        userId: String,
        newStatus: String? = null,
        newDescription: String? = null,
        newEmail: String? = null,
        newBirthDate: String? = null,
        newNickName: String? = null,
        newPhoneNumber: String? = null,
    ): EmptyDataResult<DataError>

    // ─── Location sharing ─────────────────────────────────────────────────────

    /** Per-friend toggle: what we share with this specific friend. */
    suspend fun setLocationSharing(
        friendId: String,
        share: Boolean,
        shareSpeedHeading: Boolean = false,
        snailTrail: Boolean = false,
    ): EmptyDataResult<DataError>

    /** Disables location sharing towards every friend. Stops at the first failed call. */
    suspend fun disableLocationSharingForAllFriends(): EmptyDataResult<DataError>

    // ─── Wake ─────────────────────────────────────────────────────────────────

    /** Per-friend toggle: may this friend wake us. */
    suspend fun setWakePermission(friendId: String, allowWake: Boolean): EmptyDataResult<DataError>

    /** Master switch: while off nobody can wake us. */
    suspend fun setWakeGlobal(enabled: Boolean): EmptyDataResult<DataError>

    /**
     * Asks the server to wake a friend or a group. A successful result can still report that
     * nobody was reachable (see [WakeResult.outcome]). The caller needs to tell "not friends"
     * (403) apart from a plain network failure.
     */
    suspend fun sendWake(
        targetId: String,
        isGroup: Boolean,
        reason: String,
    ): DataResult<WakeResult, DataError>
}

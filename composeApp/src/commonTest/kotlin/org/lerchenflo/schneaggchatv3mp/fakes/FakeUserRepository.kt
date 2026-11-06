package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.AvailableUser
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.chat.domain.SnailTrailPoint
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.WakeOutcome
import org.lerchenflo.schneaggchatv3mp.chat.domain.WakeResult

/**
 * In-memory [UserRepository]. The reads serve [users]; the writes are recorded and answer with
 * [writeResult] (they don't change [users]).
 */
class FakeUserRepository : UserRepository {

    data class DetailsCall(
        val userId: String,
        val newStatus: String?,
        val newDescription: String?,
        val newEmail: String?,
        val newBirthDate: String?,
        val newNickName: String?,
        val newPhoneNumber: String?,
    )

    data class LocationSharingCall(
        val friendId: String,
        val share: Boolean,
        val shareSpeedHeading: Boolean,
        val snailTrail: Boolean,
    )

    data class WakeCall(val targetId: String, val isGroup: Boolean, val reason: String)

    val users = MutableStateFlow<List<User>>(emptyList())

    /** Backs [onlineFriendIdsFlow]. */
    val onlineFriendIds = MutableStateFlow<Set<String>>(emptySet())

    /** What [getAvailableUsers] answers, whatever the search term. */
    var availableUsers: List<AvailableUser> = emptyList()

    /** Whether every write succeeds. */
    var writeResult: Boolean = true

    var wakeResult: DataResult<WakeResult, DataError> =
        DataResult.Success(WakeResult(outcome = WakeOutcome.WOKEN, userCount = 1, deviceCount = 1))

    var onSyncUsers: suspend () -> Unit = {}
    var onFetchMissingProfilePictures: suspend () -> Unit = {}

    val syncCalls = CallLog<Unit>()
    val fetchMissingProfilePictureCalls = CallLog<Unit>()
    val profilePictureRequests = CallLog<List<String>>()
    val availableUserSearches = CallLog<String>()
    val friendRequestsSent = CallLog<String>()
    val friendRequestsDenied = CallLog<String>()
    val friendsRemoved = CallLog<String>()
    val usernameChanges = CallLog<String>()
    val profilePicChanges = CallLog<ByteArray>()
    val detailsChanges = CallLog<DetailsCall>()
    val locationSharingChanges = CallLog<LocationSharingCall>()
    val disableLocationSharingCalls = CallLog<Unit>()
    val wakePermissionChanges = CallLog<Pair<String, Boolean>>()
    val wakeGlobalChanges = CallLog<Boolean>()
    val wakeCalls = CallLog<WakeCall>()

    // ─── Reads ────────────────────────────────────────────────────────────────

    override fun getUserByIdFlow(userId: String): Flow<User?> = users.map { list -> list.firstOrNull { it.id == userId } }

    override suspend fun getUserById(userId: String): User? = users.value.firstOrNull { it.id == userId }

    override fun getAllUsersFlow(searchTerm: String): Flow<List<User>> =
        users.map { list -> list.filter { it.name.contains(searchTerm, ignoreCase = true) } }

    override suspend fun getFriends(searchTerm: String): List<User> = users.value.friendsMatching(searchTerm)

    override fun getFriendsFlow(searchTerm: String): Flow<List<User>> = users.map { it.friendsMatching(searchTerm) }

    override fun getSnailTrailFlow(userId: String): Flow<List<SnailTrailPoint>> = flowOf(emptyList())

    override val onlineFriendIdsFlow: StateFlow<Set<String>> = onlineFriendIds

    // ─── Sync / pictures ──────────────────────────────────────────────────────

    override suspend fun syncUsers() {
        syncCalls.record(Unit)
        onSyncUsers()
    }

    override suspend fun getProfilePicturesForUserIds(userIds: List<String>) = profilePictureRequests.record(userIds)

    override suspend fun fetchMissingProfilePictures() {
        fetchMissingProfilePictureCalls.record(Unit)
        onFetchMissingProfilePictures()
    }

    // ─── Friends ──────────────────────────────────────────────────────────────

    override suspend fun getAvailableUsers(searchTerm: String): DataResult<List<AvailableUser>, DataError> {
        availableUserSearches.record(searchTerm)
        return DataResult.Success(availableUsers)
    }

    override suspend fun sendFriendRequest(friendId: String): EmptyDataResult<DataError> {
        friendRequestsSent.record(friendId)
        return writeResult.toFakeResult()
    }

    override suspend fun denyFriendRequest(friendId: String): EmptyDataResult<DataError> {
        friendRequestsDenied.record(friendId)
        return writeResult.toFakeResult()
    }

    override suspend fun removeFriend(friendId: String): EmptyDataResult<DataError> {
        friendsRemoved.record(friendId)
        return writeResult.toFakeResult()
    }

    // ─── Profile ──────────────────────────────────────────────────────────────

    override suspend fun changeUsername(newUsername: String): EmptyDataResult<DataError> {
        usernameChanges.record(newUsername)
        return writeResult.toFakeResult()
    }

    override suspend fun changeProfilePic(newPic: ByteArray): EmptyDataResult<DataError> {
        profilePicChanges.record(newPic)
        return writeResult.toFakeResult()
    }

    override suspend fun changeUserDetails(
        userId: String,
        newStatus: String?,
        newDescription: String?,
        newEmail: String?,
        newBirthDate: String?,
        newNickName: String?,
        newPhoneNumber: String?,
    ): EmptyDataResult<DataError> {
        detailsChanges.record(
            DetailsCall(userId, newStatus, newDescription, newEmail, newBirthDate, newNickName, newPhoneNumber)
        )
        return writeResult.toFakeResult()
    }

    // ─── Location sharing / wake ──────────────────────────────────────────────

    override suspend fun setLocationSharing(
        friendId: String,
        share: Boolean,
        shareSpeedHeading: Boolean,
        snailTrail: Boolean,
    ): EmptyDataResult<DataError> {
        locationSharingChanges.record(LocationSharingCall(friendId, share, shareSpeedHeading, snailTrail))
        return writeResult.toFakeResult()
    }

    override suspend fun disableLocationSharingForAllFriends(): EmptyDataResult<DataError> {
        disableLocationSharingCalls.record(Unit)
        return writeResult.toFakeResult()
    }

    override suspend fun setWakePermission(friendId: String, allowWake: Boolean): EmptyDataResult<DataError> {
        wakePermissionChanges.record(friendId to allowWake)
        return writeResult.toFakeResult()
    }

    override suspend fun setWakeGlobal(enabled: Boolean): EmptyDataResult<DataError> {
        wakeGlobalChanges.record(enabled)
        return writeResult.toFakeResult()
    }

    override suspend fun sendWake(
        targetId: String,
        isGroup: Boolean,
        reason: String,
    ): DataResult<WakeResult, DataError> {
        wakeCalls.record(WakeCall(targetId, isGroup, reason))
        return wakeResult
    }

    private fun List<User>.friendsMatching(searchTerm: String): List<User> =
        filter { it.friendshipStatus == FriendshipStatus.ACCEPTED && it.name.contains(searchTerm, ignoreCase = true) }
}

package org.lerchenflo.schneaggchatv3mp.chat.data.remote

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.NewFriendsUserResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.UserSyncResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.WakeResponse

/** Server calls for users: sync, profile, friendships, location sharing and wake. */
interface UserRemoteDataSource {

    suspend fun getProfilePicForUserId(userId: String) : DataResult<ByteArray, DataError>

    suspend fun userIdSync(userIds: List<IdTimeStamp>) : DataResult<UserSyncResponse, DataError>

    suspend fun getAvailableUsers(searchterm: String) : DataResult<List<NewFriendsUserResponse>, DataError>

    suspend fun sendFriendRequest(friendId: String) : DataResult<Any, DataError>

    suspend fun denyFriendRequest(friendId: String) : DataResult<Any, DataError>

    suspend fun removeFriend(friendId: String) : DataResult<Any, DataError>

    suspend fun changeUsername(newUsername: String) : DataResult<Any, DataError>

    suspend fun changeProfilePic(newProfilePic: ByteArray, fileName: String = "profile.jpg"): DataResult<Unit, DataError>

    suspend fun changeProfile(userId: String, newStatus: String?, newDescription: String?, newEmail: String?, newBirthDate: String?, newNickName: String?, newPhoneNumber: String? = null): DataResult<Any, DataError>

    /** Per-friend toggle: what we share with this specific friend (location, speed/heading, snail trail). */
    suspend fun shareLocation(
        friendId: String,
        share: Boolean,
        shareSpeedHeading: Boolean = false,
        shareSnailTrail: Boolean = false,
    ): DataResult<Unit, DataError>

    /** Per-friend toggle: may this friend wake us (play an alarm on our devices). */
    suspend fun setWakePermission(friendId: String, allowWake: Boolean): DataResult<Unit, DataError>

    /** Master switch: while off nobody can wake us, whatever the per-friend toggles say. */
    suspend fun setWakeGlobal(enabled: Boolean): DataResult<Unit, DataError>

    /** Wake a friend or a group - plays an alarm on every consenting Android device. */
    suspend fun sendWake(targetId: String, isGroup: Boolean, reason: String): DataResult<WakeResponse, DataError>
}

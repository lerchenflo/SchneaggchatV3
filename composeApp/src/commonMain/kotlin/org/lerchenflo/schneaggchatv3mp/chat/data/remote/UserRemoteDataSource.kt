package org.lerchenflo.schneaggchatv3mp.chat.data.remote

import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.NewFriendsUserResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.UserSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.WakeResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

/** Server calls for users: sync, profile, friendships, location sharing and wake. */
interface UserRemoteDataSource {

    suspend fun getProfilePicForUserId(userId: String) : NetworkResult<ByteArray, NetworkingError>

    suspend fun userIdSync(userIds: List<IdTimeStamp>) : NetworkResult<UserSyncResponse, NetworkingError>

    suspend fun getAvailableUsers(searchterm: String) : NetworkResult<List<NewFriendsUserResponse>, NetworkingError>

    suspend fun sendFriendRequest(friendId: String) : NetworkResult<Any, NetworkingError>

    suspend fun denyFriendRequest(friendId: String) : NetworkResult<Any, NetworkingError>

    suspend fun removeFriend(friendId: String) : NetworkResult<Any, NetworkingError>

    suspend fun changeUsername(newUsername: String) : NetworkResult<Any, NetworkingError>

    suspend fun changeProfilePic(newProfilePic: ByteArray, fileName: String = "profile.jpg"): NetworkResult<Unit, NetworkingError>

    suspend fun changeProfile(userId: String, newStatus: String?, newDescription: String?, newEmail: String?, newBirthDate: String?, newNickName: String?, newPhoneNumber: String? = null): NetworkResult<Any, NetworkingError>

    /** Per-friend toggle: what we share with this specific friend (location, speed/heading, snail trail). */
    suspend fun shareLocation(
        friendId: String,
        share: Boolean,
        shareSpeedHeading: Boolean = false,
        shareSnailTrail: Boolean = false,
    ): NetworkResult<Unit, NetworkingError>

    /** Per-friend toggle: may this friend wake us (play an alarm on our devices). */
    suspend fun setWakePermission(friendId: String, allowWake: Boolean): NetworkResult<Unit, NetworkingError>

    /** Master switch: while off nobody can wake us, whatever the per-friend toggles say. */
    suspend fun setWakeGlobal(enabled: Boolean): NetworkResult<Unit, NetworkingError>

    /** Wake a friend or a group - plays an alarm on every consenting Android device. */
    suspend fun sendWake(targetId: String, isGroup: Boolean, reason: String): NetworkResult<WakeResponse, NetworkingError>
}

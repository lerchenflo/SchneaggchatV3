package org.lerchenflo.schneaggchatv3mp.chat.data.remote

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.appendProfilePic
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.LocationShareRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.NewFriendsUserResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.UserRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.UserSyncResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.WakeGlobalRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.WakePermissionRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.WakeRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.WakeResponse
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager

/** [UserRemoteDataSource] over Ktor, against the server URL from [Preferencemanager]. */
class KtorUserRemoteDataSource(
    httpClient: HttpClient,
    preferenceManager: Preferencemanager,
    loggingRepository: Logger,
) : UserRemoteDataSource {

    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    override suspend fun getProfilePicForUserId(userId: String) : DataResult<ByteArray, DataError> {
        return server.safeGet(
            endpoint = "/users/profilepic/$userId"
        )
    }

    override suspend fun userIdSync(userIds: List<IdTimeStamp>) : DataResult<UserSyncResponse, DataError> {
        return server.safePost(
            endpoint = "/users/sync",
            body = userIds
        )
    }

    override suspend fun getAvailableUsers(searchterm: String) : DataResult<List<NewFriendsUserResponse>, DataError> {
        return server.safeGet(
            endpoint = "/users/availableusers?searchterm=$searchterm"
        )
    }

    override suspend fun sendFriendRequest(friendId: String) : DataResult<Any, DataError> {
        return server.safePost(
            endpoint = "/users/addfriend/$friendId",
            body = ""
        )
    }

    override suspend fun denyFriendRequest(friendId: String) : DataResult<Any, DataError> {
        return server.safePost(
            endpoint = "/users/denyfriend/$friendId",
            body = ""
        )
    }

    override suspend fun removeFriend(friendId: String) : DataResult<Any, DataError> {
        return server.safePost(
            endpoint = "/users/removefriend/$friendId",
            body = ""
        )
    }

    override suspend fun changeUsername(newUsername: String) : DataResult<Any, DataError> {
        return server.safePost(
            endpoint = "/users/changeusername",
            body = newUsername
        )
    }

    override suspend fun changeProfilePic(newProfilePic: ByteArray, fileName: String): DataResult<Unit, DataError> {
        return server.safePostMultipart(
            endpoint = "/users/setprofilepic",
            parts = { appendProfilePic(newProfilePic, fileName) }
        )
    }

    override suspend fun changeProfile(userId: String, newStatus: String?, newDescription: String?, newEmail: String?, newBirthDate: String?, newNickName: String?, newPhoneNumber: String?): DataResult<Any, DataError> {
        return server.safePost(
            endpoint = "/users/changeprofile",
            body = UserRequest(
                userId = userId,
                newDescription = newDescription,
                newStatus = newStatus,
                newEmail = newEmail,
                newBirthDate = newBirthDate,
                newNickName = newNickName,
                newPhoneNumber = newPhoneNumber,
            )
        )
    }

    override suspend fun shareLocation(
        friendId: String,
        share: Boolean,
        shareSpeedHeading: Boolean,
        shareSnailTrail: Boolean,
    ): DataResult<Unit, DataError> {
        return server.safePost(
            endpoint = "/users/sharelocation",
            body = LocationShareRequest(friendId, share, shareSpeedHeading, shareSnailTrail)
        )
    }

    override suspend fun setWakePermission(friendId: String, allowWake: Boolean): DataResult<Unit, DataError> {
        return server.safePost(
            endpoint = "/users/setwakepermission",
            body = WakePermissionRequest(friendId, allowWake)
        )
    }

    override suspend fun setWakeGlobal(enabled: Boolean): DataResult<Unit, DataError> {
        return server.safePost(
            endpoint = "/users/setwakeglobal",
            body = WakeGlobalRequest(enabled)
        )
    }

    override suspend fun sendWake(targetId: String, isGroup: Boolean, reason: String): DataResult<WakeResponse, DataError> {
        return server.safePost(
            endpoint = "/wake/send",
            body = WakeRequest(targetId, isGroup, reason)
        )
    }
}

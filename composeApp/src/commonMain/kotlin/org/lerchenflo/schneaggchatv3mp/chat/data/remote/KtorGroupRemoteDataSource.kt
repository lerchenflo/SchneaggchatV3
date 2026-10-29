package org.lerchenflo.schneaggchatv3mp.chat.data.remote

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.appendProfilePic
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.GroupActionRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.GroupResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.GroupSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.NetworkGroupMemberAction
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.SetGroupExpiryRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager

/** [GroupRemoteDataSource] over Ktor, against the server URL from [Preferencemanager]. */
class KtorGroupRemoteDataSource(
    httpClient: HttpClient,
    preferenceManager: Preferencemanager,
    loggingRepository: Logger,
) : GroupRemoteDataSource {

    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    override suspend fun getProfilePicForGroupId(groupId: String) : NetworkResult<ByteArray, NetworkingError> {
        return server.safeGet(
            endpoint = "/groups/profilepic/$groupId"
        )
    }

    override suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>, //Userids
        profilePicBytes: ByteArray,
    ): NetworkResult<GroupResponse, NetworkingError> {
        return server.safePostMultipart(
            endpoint = "/groups/create",
            parts = {
                append("name", name)
                append("description", description)
                append("memberlist", memberIds.joinToString(","))
                appendProfilePic(profilePicBytes, fileName = "image.png")
            }
        )
    }

    override suspend fun groupIdSync(groupIds: List<IdTimeStamp>) : NetworkResult<GroupSyncResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/groups/sync",
            body = groupIds
        )
    }

    override suspend fun changeGroupProfilePic(newProfilePic: ByteArray, groupId: String, fileName: String): NetworkResult<Unit, NetworkingError> {
        return server.safePostMultipart(
            endpoint = "/groups/setprofilepic?groupid=$groupId",
            parts = { appendProfilePic(newProfilePic, fileName) }
        )
    }

    override suspend fun changeGroupDescription(newDescription: String, groupId: String) : NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/groups/setdescription?groupid=$groupId",
            body = newDescription
        )
    }

    override suspend fun changeGroupName(newName: String, groupId: String) : NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/groups/setGroupName?groupid=$groupId",
            body = newName
        )
    }

    override suspend fun changeGroupMembers(action: NetworkGroupMemberAction, memberId: String, groupId: String): NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/groups/changemembers",
            body = GroupActionRequest(
                action = action,
                groupMemberId = memberId,
                groupId = groupId
            )
        )
    }

    override suspend fun changeGroupExpiry(groupId: String, expiresAt: Long?): NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/groups/setexpiry",
            body = SetGroupExpiryRequest(
                groupId = groupId,
                expiresAt = expiresAt
            )
        )
    }

    override suspend fun deleteGroup(groupId: String): NetworkResult<Unit, NetworkingError> {
        return server.safeDelete(endpoint = "/groups/delete?groupid=$groupId")
    }
}

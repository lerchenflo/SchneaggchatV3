package org.lerchenflo.schneaggchatv3mp.chat.data.remote

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.appendProfilePic
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.GroupActionRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.GroupResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.GroupSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.NetworkGroupMemberAction
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.GroupSettingsRequest
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager

/** [GroupRemoteDataSource] over Ktor, against the server URL from [Preferencemanager]. */
class KtorGroupRemoteDataSource(
    httpClient: HttpClient,
    preferenceManager: Preferencemanager,
    loggingRepository: Logger,
) : GroupRemoteDataSource {

    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    override suspend fun getProfilePicForGroupId(groupId: String) : DataResult<ByteArray, DataError> {
        return server.safeGet(
            endpoint = "/groups/profilepic/$groupId"
        )
    }

    override suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>, //Userids
        profilePicBytes: ByteArray,
        membersCanAdd: Boolean,
    ): DataResult<GroupResponse, DataError> {
        return server.safePostMultipart(
            endpoint = "/groups/create",
            parts = {
                append("name", name)
                append("description", description)
                append("memberlist", memberIds.joinToString(","))
                append("membersCanAdd", membersCanAdd.toString())
                appendProfilePic(profilePicBytes, fileName = "image.png")
            }
        )
    }

    override suspend fun groupIdSync(groupIds: List<IdTimeStamp>) : DataResult<GroupSyncResponse, DataError> {
        return server.safePost(
            endpoint = "/groups/sync",
            body = groupIds
        )
    }

    override suspend fun changeGroupProfilePic(newProfilePic: ByteArray, groupId: String, fileName: String): DataResult<Unit, DataError> {
        return server.safePostMultipart(
            endpoint = "/groups/setprofilepic?groupid=$groupId",
            parts = { appendProfilePic(newProfilePic, fileName) }
        )
    }

    override suspend fun changeGroupMembers(action: NetworkGroupMemberAction, memberId: String, groupId: String): DataResult<Any, DataError> {
        return server.safePost(
            endpoint = "/groups/changemembers",
            body = GroupActionRequest(
                action = action,
                groupMemberId = memberId,
                groupId = groupId
            )
        )
    }

    override suspend fun updateGroupSettings(request: GroupSettingsRequest): DataResult<Any, DataError> {
        return server.safePost(
            endpoint = "/groups/settings",
            body = request
        )
    }

    override suspend fun deleteGroup(groupId: String): DataResult<Unit, DataError> {
        return server.safeDelete(endpoint = "/groups/delete?groupid=$groupId")
    }
}

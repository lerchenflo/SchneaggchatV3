package org.lerchenflo.schneaggchatv3mp.chat.data.remote

import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.GroupResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.GroupSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.NetworkGroupMemberAction
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

/** Server calls for groups: sync, creation, profile and members. */
interface GroupRemoteDataSource {

    suspend fun getProfilePicForGroupId(groupId: String) : NetworkResult<ByteArray, NetworkingError>

    suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>, //Userids
        profilePicBytes: ByteArray,
    ): NetworkResult<GroupResponse, NetworkingError>

    suspend fun groupIdSync(groupIds: List<IdTimeStamp>) : NetworkResult<GroupSyncResponse, NetworkingError>

    suspend fun changeGroupProfilePic(newProfilePic: ByteArray, groupId: String, fileName: String = "profile.jpg"): NetworkResult<Unit, NetworkingError>

    suspend fun changeGroupDescription(newDescription: String, groupId: String) : NetworkResult<Any, NetworkingError>

    suspend fun changeGroupName(newName: String, groupId: String) : NetworkResult<Any, NetworkingError>

    suspend fun changeGroupMembers(action: NetworkGroupMemberAction, memberId: String, groupId: String): NetworkResult<Any, NetworkingError>

    //Pass null to clear the expiry (group never auto-expires)
    suspend fun changeGroupExpiry(groupId: String, expiresAt: Long?): NetworkResult<Any, NetworkingError>
}

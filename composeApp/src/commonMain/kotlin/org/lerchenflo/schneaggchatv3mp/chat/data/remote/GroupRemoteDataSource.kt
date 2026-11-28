package org.lerchenflo.schneaggchatv3mp.chat.data.remote

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.GroupSettingsRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.GroupSyncResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.NetworkGroupMemberAction
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.GroupResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp

/** Server calls for groups: sync, creation, profile and members. */
interface GroupRemoteDataSource {

    suspend fun getProfilePicForGroupId(groupId: String) : DataResult<ByteArray, DataError>

    suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>, //Userids
        profilePicBytes: ByteArray,
        membersCanAdd: Boolean,
    ): DataResult<GroupResponse, DataError>

    suspend fun groupIdSync(groupIds: List<IdTimeStamp>) : DataResult<GroupSyncResponse, DataError>

    suspend fun changeGroupProfilePic(newProfilePic: ByteArray, groupId: String, fileName: String = "profile.jpg"): DataResult<Unit, DataError>

    suspend fun changeGroupMembers(action: NetworkGroupMemberAction, memberId: String, groupId: String): DataResult<Any, DataError>


    //Name, description, delete timer and members-can-add in one partial update; only the changed fields are set
    suspend fun updateGroupSettings(request: GroupSettingsRequest): DataResult<Any, DataError>

    //Deletes the whole group for every member (admins only)
    suspend fun deleteGroup(groupId: String): DataResult<Unit, DataError>
}

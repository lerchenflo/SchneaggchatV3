package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMemberAction
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupRepository

/**
 * In-memory [GroupRepository]. The reads serve [groups]; the writes are recorded and answer with
 * [writeResult] / [createdGroupId] (they don't change [groups]).
 */
class FakeGroupRepository : GroupRepository {

    data class CreateCall(
        val name: String,
        val description: String,
        val memberIds: List<String>,
        val profilePic: ByteArray,
    )

    data class MemberCall(val action: GroupMemberAction, val memberId: String, val groupId: String)

    val groups = MutableStateFlow<List<Group>>(emptyList())

    /** Whether every write succeeds. */
    var writeResult: Boolean = true

    /** What [createGroup] answers; null means the creation failed. */
    var createdGroupId: String? = "created-group"

    /** Runs on every [syncGroups]; its result is what the sync reports as removed group ids. */
    var onSyncGroups: suspend () -> List<String> = { emptyList() }
    var onFetchMissingGroupPictures: suspend () -> Unit = {}

    val syncCalls = CallLog<Unit>()
    val fetchMissingGroupPictureCalls = CallLog<Unit>()
    val profilePictureRequests = CallLog<List<String>>()
    val createCalls = CallLog<CreateCall>()
    val profilePicChanges = CallLog<Pair<String, ByteArray>>()
    val descriptionChanges = CallLog<Pair<String, String>>()
    val nameChanges = CallLog<Pair<String, String>>()
    val membersCanAddChanges = CallLog<Pair<String, Boolean>>()
    val memberChanges = CallLog<MemberCall>()
    val expiryChanges = CallLog<Pair<String, Long?>>()
    val deleteCalls = CallLog<String>()

    // ─── Reads ────────────────────────────────────────────────────────────────

    override fun getAllGroupsFlow(): Flow<List<Group>> = groups

    override fun getGroupByIdFlow(groupId: String): Flow<Group?> = groups.map { list -> list.firstOrNull { it.id == groupId } }

    override suspend fun getGroupById(groupId: String): Group? = groups.value.firstOrNull { it.id == groupId }

    override suspend fun getGroupMembers(groupId: String): List<GroupMember> =
        getGroupById(groupId)?.members ?: emptyList()

    override suspend fun getCommonGroups(ownId: String, otherUserId: String): List<Group> =
        groups.value.filter { group ->
            val memberIds = group.members.map { it.userId }
            ownId in memberIds && otherUserId in memberIds
        }

    // ─── Sync / pictures ──────────────────────────────────────────────────────

    override suspend fun syncGroups(): List<String> {
        syncCalls.record(Unit)
        return onSyncGroups()
    }

    override suspend fun getProfilePicturesForGroupIds(groupIds: List<String>) = profilePictureRequests.record(groupIds)

    override suspend fun fetchMissingGroupPictures() {
        fetchMissingGroupPictureCalls.record(Unit)
        onFetchMissingGroupPictures()
    }

    // ─── Writes ───────────────────────────────────────────────────────────────

    override suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>,
        profilePic: ByteArray,
        membersCanAdd: Boolean,
    ): DataResult<String, DataError> {
        createCalls.record(CreateCall(name, description, memberIds, profilePic))
        return createdGroupId.toFakeDataResult()
    }

    override suspend fun changeGroupProfilePic(groupId: String, newPic: ByteArray): EmptyDataResult<DataError> {
        profilePicChanges.record(groupId to newPic)
        return writeResult.toFakeResult()
    }

    override suspend fun changeGroupSettings(
        groupId: String,
        name: String?,
        description: String?,
        membersCanAdd: Boolean?,
    ): EmptyDataResult<DataError> {
        name?.let { nameChanges.record(groupId to it) }
        description?.let { descriptionChanges.record(groupId to it) }
        membersCanAdd?.let { membersCanAddChanges.record(groupId to it) }
        return writeResult.toFakeResult()
    }

    override suspend fun changeGroupMembers(action: GroupMemberAction, memberId: String, groupId: String): EmptyDataResult<DataError> {
        memberChanges.record(MemberCall(action, memberId, groupId))
        return writeResult.toFakeResult()
    }

    override suspend fun changeGroupExpiry(groupId: String, expiresAt: Long?): EmptyDataResult<DataError> {
        expiryChanges.record(groupId to expiresAt)
        return writeResult.toFakeResult()
    }

    override suspend fun deleteGroup(groupId: String): EmptyDataResult<DataError> {
        deleteCalls.record(groupId)
        return writeResult.toFakeResult()
    }
}

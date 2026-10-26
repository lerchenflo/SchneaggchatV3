package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMemberAction

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

    /** What every Boolean write answers. */
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
    ): String? {
        createCalls.record(CreateCall(name, description, memberIds, profilePic))
        return createdGroupId
    }

    override suspend fun changeGroupProfilePic(groupId: String, newPic: ByteArray): Boolean {
        profilePicChanges.record(groupId to newPic)
        return writeResult
    }

    override suspend fun changeGroupDescription(groupId: String, newDescription: String): Boolean {
        descriptionChanges.record(groupId to newDescription)
        return writeResult
    }

    override suspend fun changeGroupName(groupId: String, newName: String): Boolean {
        nameChanges.record(groupId to newName)
        return writeResult
    }

    override suspend fun changeGroupMembers(action: GroupMemberAction, memberId: String, groupId: String): Boolean {
        memberChanges.record(MemberCall(action, memberId, groupId))
        return writeResult
    }

    override suspend fun changeGroupExpiry(groupId: String, expiresAt: Long?): Boolean {
        expiryChanges.record(groupId to expiresAt)
        return writeResult
    }

    override suspend fun deleteGroup(groupId: String): Boolean {
        deleteCalls.record(groupId)
        return writeResult
    }
}

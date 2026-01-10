package org.lerchenflo.schneaggchatv3mp.chat.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.relations.GroupWithMembersDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.toDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.toGroup
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate

class GroupRepository(
    private val database: AppDatabase
) {

    /**
     * Upsert group and fully replace its members (same pattern as Message + Readers)
     */
    @Transaction
    suspend fun upsertGroup(group: Group) {
        val groupWithMembersDto = group.toDto()

        // 1️⃣ Upsert group first
        database.groupDao().upsertGroup(groupWithMembersDto.group)

        // 2️⃣ Delete all existing members for this group
        database.groupDao().deleteMembersForGroup(groupWithMembersDto.group.id)

        // 3️⃣ Re-insert members (if any)
        if (groupWithMembersDto.members.isNotEmpty()) {
            database.groupDao().upsertMembers(
                groupWithMembersDto.members.map {
                    it.copy(groupId = groupWithMembersDto.group.id)
                }
            )
        }
    }

    @Transaction
    suspend fun getgroupchangeid(): List<IdChangeDate> {
        return database.groupDao().getGroupIdsWithChangeDates()
    }

    @Transaction
    fun getAllGroupswithMembersFlow(): Flow<List<Group>> {
        return database.groupDao().getAllGroupsWithMembersFlow().map { list ->
            list.map { it.toGroup() }
        }
    }

    suspend fun getAllGroups() : List<Group> {
        return database.groupDao().getAllGroupsWithMembers().map {
            it.toGroup()
        }
    }

    suspend fun deleteGroup(groupid: String) {
        database.groupDao().deleteMembersForGroup(groupid)
        database.groupDao().deleteGroup(groupid)
    }

    suspend fun updateGroupProfilePicUrl(groupId: String, newUrl: String) {
        val dbGroup = database.groupDao().getGroupById(groupId)
        if (dbGroup != null) {
            database.groupDao().upsertGroup(
                dbGroup.copy(profilePictureUrl = newUrl)
            )
        }
    }


    /**
     * Return the members of a group as domain objects (empty list when not found).
     * Caller example:
     *   chat.toGroup()?.groupMembers = groupRepository.getGroupMembers(chat.id)
     */
    @Transaction
    suspend fun getGroupMembers(groupId: String): List<GroupMember> {
        val gwm: GroupWithMembersDto? = database.groupDao().getGroupWithMembersById(groupId)
        // toGroup() maps the DTO relation to a domain Group which carries members
        return gwm?.toGroup()?.members ?: emptyList()
    }

    fun getGroupFlow(id: String): Flow<Group?> {
        return database.groupDao().getGroupWithMembersByIdFlow(id).map { it?.toGroup() }
    }

    /**
     * Return all groups (domain objects) that are common between the current user and the provided userId.
     * Caller example:
     *   chat.toUser()?.commonGroups = groupRepository.getCommonGroups(chat.id) // Together with own id
     */
    @Transaction
    suspend fun getCommonGroups(otherUserId: String): List<Group> {
        val ownId = SessionCache.getOwnIdValue()
        if (ownId.isNullOrBlank()) return emptyList()

        val commonDtos = database.groupDao().getCommonGroupsWithMembers(ownId, otherUserId)
        return commonDtos.map { it.toGroup() }
    }



}

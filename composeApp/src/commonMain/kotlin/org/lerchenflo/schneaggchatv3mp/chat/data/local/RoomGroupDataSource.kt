package org.lerchenflo.schneaggchatv3mp.chat.data.local

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.relations.GroupWithMembers
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.toEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.toGroup
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate

class RoomGroupDataSource(
    private val database: AppDatabase
) : GroupLocalDataSource {

    override suspend fun upsertGroup(group: Group) {
        val groupWithMembersDto = group.toEntity()

        database.groupDao().upsertGroupWithMembers(
            groupWithMembersDto.group,
            groupWithMembersDto.members.map {
                it.copy(groupId = groupWithMembersDto.group.id)
            }
        )
    }

    @Transaction
    override suspend fun getgroupchangeid(): List<IdChangeDate> {
        return database.groupDao().getGroupIdsWithChangeDates()
    }

    @Transaction
    override fun getAllGroupswithMembersFlow(): Flow<List<Group>> {
        return database.groupDao().getAllGroupsWithMembersFlow().map { list ->
            list.map { it.toGroup() }
        }
    }

    override suspend fun getAllGroups() : List<Group> {
        return database.groupDao().getAllGroupsWithMembers().map {
            it.toGroup()
        }
    }

    override suspend fun deleteGroup(groupid: String) {
        // A left/removed group no longer produces a message-sync signal - purge its
        // messages here so both /groups/sync and the websocket groupchange path cover it.
        database.messageReaderDao().deleteReadersForGroupMessages(groupid)
        database.messageDao().deleteMessagesForGroup(groupid)
        database.groupDao().deleteMembersForGroup(groupid)
        database.groupDao().deleteGroup(groupid)
    }

    override suspend fun updateGroupProfilePicUrl(groupId: String, newUrl: String) {
        val dbGroup = database.groupDao().getGroupById(groupId)
        if (dbGroup != null) {
            database.groupDao().upsertGroup(
                dbGroup.copy(profilePictureUrl = newUrl)
            )
        }
    }


    @Transaction
    override suspend fun getGroupMembers(groupId: String): List<GroupMember> {
        val gwm: GroupWithMembers? = database.groupDao().getGroupWithMembersById(groupId)
        // toGroup() maps the DTO relation to a domain Group which carries members
        return gwm?.toGroup()?.members ?: emptyList()
    }

    override fun getGroupFlow(id: String): Flow<Group?> {
        return database.groupDao().getGroupWithMembersByIdFlow(id).map { it?.toGroup() }
    }

    override suspend fun getGroupById(id: String) : Group? {
        return database.groupDao().getGroupWithMembersById(id)?.toGroup()
    }

    @Transaction
    override suspend fun getCommonGroups(ownId: String, otherUserId: String): List<Group> {

        val commonDtos = database.groupDao().getCommonGroupsWithMembers(ownId, otherUserId)
        return commonDtos.map { it.toGroup() }
    }


}

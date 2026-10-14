package org.lerchenflo.schneaggchatv3mp.chat.data

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate

/**
 * Local (Room) storage for groups and their members.
 */
interface GroupLocalDataSource {

    /**
     * Upsert group and fully replace its members (same pattern as Message + Readers)
     */
    suspend fun upsertGroup(group: Group)

    suspend fun getgroupchangeid(): List<IdChangeDate>

    fun getAllGroupswithMembersFlow(): Flow<List<Group>>

    suspend fun getAllGroups(): List<Group>

    suspend fun deleteGroup(groupid: String)

    suspend fun updateGroupProfilePicUrl(groupId: String, newUrl: String)

    /**
     * Return the members of a group as domain objects (empty list when not found).
     * Caller example:
     *   chat.toGroup()?.groupMembers = groupLocalDataSource.getGroupMembers(chat.id)
     */
    suspend fun getGroupMembers(groupId: String): List<GroupMember>

    fun getGroupFlow(id: String): Flow<Group?>

    suspend fun getGroupById(id: String): Group?

    /**
     * Return all groups (domain objects) that are common between the current user and the provided userId.
     * Caller example:
     *   chat.toUser()?.commonGroups = groupLocalDataSource.getCommonGroups(chat.id) // Together with own id
     */
    suspend fun getCommonGroups(ownId: String, otherUserId: String): List<Group>
}

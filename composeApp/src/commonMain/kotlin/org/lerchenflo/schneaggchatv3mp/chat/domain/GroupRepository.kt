package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult

/**
 * Groups and their members: reads come from the local database, writes go to the server first.
 * Write calls return their error, the caller maps it to UI.
 */
interface GroupRepository {

    // ─── Reads (local database) ───────────────────────────────────────────────

    /** Every stored group with its members. */
    fun getAllGroupsFlow(): Flow<List<Group>>

    fun getGroupByIdFlow(groupId: String): Flow<Group?>

    suspend fun getGroupById(groupId: String): Group?

    /** The members of a group (empty list when the group is not stored). */
    suspend fun getGroupMembers(groupId: String): List<GroupMember>

    /** Every stored group that both [ownId] and [otherUserId] are members of. */
    suspend fun getCommonGroups(ownId: String, otherUserId: String): List<Group>

    // ─── Sync / pictures ──────────────────────────────────────────────────────

    /**
     * Pulls changed and deleted groups from the server into the local database.
     * @return the ids of the groups deleted during this sync (left, removed, or the group itself
     * got deleted), so the caller can purge their messages once a parallel message sync settled.
     */
    suspend fun syncGroups(): List<String>

    /** Downloads the profile pictures of [groupIds] and stores their local paths. */
    suspend fun getProfilePicturesForGroupIds(groupIds: List<String>)

    /** Downloads every stored group's profile picture whose file is missing locally. */
    suspend fun fetchMissingGroupPictures()

    // ─── Writes ───────────────────────────────────────────────────────────────

    /** @return the id of the created group to directly open the chat */
    suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>,
        profilePic: ByteArray,
    ): DataResult<String, DataError>
    suspend fun changeGroupProfilePic(groupId: String, newPic: ByteArray): EmptyDataResult<DataError>
    suspend fun changeGroupDescription(groupId: String, newDescription: String): EmptyDataResult<DataError>
    suspend fun changeGroupName(groupId: String, newName: String): EmptyDataResult<DataError>

    /** Adds or removes a member, or changes their admin flag. */
    suspend fun changeGroupMembers(action: GroupMemberAction, memberId: String, groupId: String): EmptyDataResult<DataError>

    /** Pass null for [expiresAt] to clear the expiry (group never auto-expires). */
    suspend fun changeGroupExpiry(groupId: String, expiresAt: Long?): EmptyDataResult<DataError>

    /**
     * Deletes the whole group for every member (admins only). A connected event is detached and
     * survives.
     */
    suspend fun deleteGroup(groupId: String): EmptyDataResult<DataError>
}

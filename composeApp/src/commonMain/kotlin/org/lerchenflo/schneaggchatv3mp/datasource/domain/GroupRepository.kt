package org.lerchenflo.schneaggchatv3mp.datasource.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.Group
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.GroupMember
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.GroupMemberAction

/**
 * Groups and their members: reads come from the local database, writes go to the server first.
 * Errors of the write calls are sent to the [ErrorChannel].
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

    /** @return the id of the created group to directly open the chat, null on error */
    suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>,
        profilePic: ByteArray,
    ): String?

    /** @return true on success */
    suspend fun changeGroupProfilePic(groupId: String, newPic: ByteArray): Boolean

    /** @return true on success */
    suspend fun changeGroupDescription(groupId: String, newDescription: String): Boolean

    /** @return true on success */
    suspend fun changeGroupName(groupId: String, newName: String): Boolean

    /** Adds or removes a member, or changes their admin flag. @return true on success */
    suspend fun changeGroupMembers(action: GroupMemberAction, memberId: String, groupId: String): Boolean

    /** Pass null for [expiresAt] to clear the expiry (group never auto-expires). @return true on success */
    suspend fun changeGroupExpiry(groupId: String, expiresAt: Long?): Boolean
}

package org.lerchenflo.schneaggchatv3mp.chat.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.GROUPPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupWithMembers
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager

class GroupRepository(
    private val database: AppDatabase,
    private val networkUtils: NetworkUtils,
    private val pictureManager: PictureManager
) {
    suspend fun upsertGroup(group: Group){
        database.groupDao().upsertGroup(group)
    }

    @Transaction
    suspend fun getgroupchangeid(): List<IdChangeDate>{
        return database.groupDao().getGroupIdsWithChangeDates()
    }

    @Transaction
    fun getallgroupswithmembers(): Flow<List<GroupWithMembers>> {
        return database.groupDao().getAllGroupsWithMembers()
    }

    suspend fun deleteGroup(groupid: Long){
        database.groupDao().deleteGroup(groupid)
    }

    @Transaction
    suspend fun upsertGroupWithMembers(gwm: GroupWithMembers) {
        // 1) upsert the group
        val savefilename = gwm.group.id.toString() + GROUPPROFILEPICTURE_FILE_NAME
        pictureManager.savePictureToStorage(gwm.group.profilePicture, savefilename)
        gwm.group.profilePicture = savefilename

        database.groupDao().upsertGroup(gwm.group)

        // 3) replace membership rows for this group
        // delete old members for the group
        database.groupDao().deleteMembersForGroup(gwm.group.id)

        // create new join rows and insert
        val joinRows = gwm.members.map { member ->
            GroupMember(0, gwm.group.id, member.id, member.color, member.joinDate, member.isAdmin)
        }

        if (joinRows.isNotEmpty()) {
            database.groupDao().upsertMembers(joinRows)
        }
    }
}
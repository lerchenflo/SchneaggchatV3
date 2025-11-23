package org.lerchenflo.schneaggchatv3mp.chat.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.toDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.toGroup
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager

class GroupRepository(
    private val database: AppDatabase,
    private val networkUtils: NetworkUtils,
    private val pictureManager: PictureManager
) {
    suspend fun upsertGroup(group: Group){
        database.groupDao().upsertGroup(group.toDto().group)
    }

    @Transaction
    suspend fun getgroupchangeid(): List<IdChangeDate>{
        return database.groupDao().getGroupIdsWithChangeDates()
    }

    @Transaction
    fun getallgroupswithmembers(): Flow<List<Group>> {
        return database.groupDao().getAllGroupsWithMembers().map { list ->
            list.map {
                it.toGroup()
            }
        }
    }

    suspend fun deleteGroup(groupid: String){
        database.groupDao().deleteGroup(groupid)
    }


    /*
    //TODO: Rebuild + Rebuild the dtos, all server synced
    @Transaction
    suspend fun upsertGroupWithMembers(gwm: GroupWithMembersDto) {
        // 1) upsert the group
        val savefilename = gwm.group.id.toString() + GROUPPROFILEPICTURE_FILE_NAME
        val path = pictureManager.savePictureToStorage(gwm.group.profilePicture, savefilename)
        gwm.group.profilePicture = path

        database.groupDao().upsertGroup(gwm.group)

        // 3) replace membership rows for this group
        // delete old members for the group
        database.groupDao().deleteMembersForGroup(gwm.group.id)

        // create new join rows and insert
        val joinRows = gwm.members.map { member ->
            //GroupMemberDto(0, gwm.group.id, member.id, member.color, member.joinDate, member.isAdmin)
        }

        if (joinRows.isNotEmpty()) {
            database.groupDao().upsertMembers(joinRows)
        }
    }

     */
}
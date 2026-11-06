package org.lerchenflo.schneaggchatv3mp.chat.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.lerchenflo.schneaggchatv3mp.GROUPPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.asEmptyDataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.map
import org.lerchenflo.schneaggchatv3mp.app.domain.onSuccess
import org.lerchenflo.schneaggchatv3mp.chat.data.local.GroupLocalDataSource
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMemberAction
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.toNetworkGroupMemberAction
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.GroupSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.GroupRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.toGroup

/**
 * [GroupRepository] on top of the local [GroupLocalDataSource] and the server via [GroupRemoteDataSource].
 * The sync, picture and write code is moved verbatim from AppRepository (groupIdSync,
 * getProfilePicturesForGroupIds, createGroup, changeGroup*, group part of getMissingPics).
 */
class OfflineFirstGroupRepository(
    private val groupLocalDataSource: GroupLocalDataSource,
    private val groupRemoteDataSource: GroupRemoteDataSource,
    private val pictureManager: PictureManager,
    private val loggingRepository: Logger,
) : GroupRepository {

    // ─── Reads ────────────────────────────────────────────────────────────────

    override fun getAllGroupsFlow(): Flow<List<Group>> =
        groupLocalDataSource.getAllGroupswithMembersFlow()

    override fun getGroupByIdFlow(groupId: String): Flow<Group?> =
        groupLocalDataSource.getGroupFlow(groupId)

    override suspend fun getGroupById(groupId: String): Group? =
        groupLocalDataSource.getGroupById(groupId)

    override suspend fun getGroupMembers(groupId: String): List<GroupMember> =
        groupLocalDataSource.getGroupMembers(groupId)

    override suspend fun getCommonGroups(ownId: String, otherUserId: String): List<Group> =
        groupLocalDataSource.getCommonGroups(ownId, otherUserId)

    // ─── Writes ───────────────────────────────────────────────────────────────

    /**
     * Create a group
     * @return the id of the created group to directly open the chat
     */
    override suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>,
        profilePic: ByteArray
    ): DataResult<String, DataError> =
        groupRemoteDataSource.createGroup(
            name = name,
            description = description,
            memberIds = memberIds,
            profilePicBytes = profilePic
        ).trackConnectivity()
            .onSuccess { groupLocalDataSource.upsertGroup(it.toGroup()) }
            .map { it.id }


    // ─── Group sync ───────────────────────────────────────────────────────────

    /**
     * The group mutations below run their own [syncGroups] outside DataSyncCoordinator's dataSyncLock
     * (D16/R1), so a full sync and a mutation's sync could otherwise run /groups/sync at the same
     * time. This lock makes the second one wait. Lock order is always dataSyncLock -> this lock,
     * and nothing inside [syncGroups] calls [syncGroups] again (a Mutex is not reentrant).
     */
    private val groupSyncLock = Mutex()

    /**
     * @return the ids of groups deleted during this sync (left, removed, or the group itself
     * got deleted) - used by DataSyncCoordinator.dataSync to purge that group's messages once the
     * parallel message job has settled.
     */
    override suspend fun syncGroups(): List<String> {
        groupSyncLock.withLock {

            SessionCache.requireLoggedIn() ?: return emptyList()


            val localgroups = groupLocalDataSource.getgroupchangeid()
            val profilepicsToGet = mutableListOf<String>()

            val groupSyncResponse = groupRemoteDataSource.groupIdSync(
                groupIds = localgroups.map {
                    IdTimeStamp(it.id, it.updatedAt)
                }
            ).trackConnectivity()

            var deletedGroupIds: List<String> = emptyList()

            when (groupSyncResponse) {
                is DataResult.Error<*> -> {
                    loggingRepository.logWarning("groupid sync error: ${groupSyncResponse.error}")
                }

                is DataResult.Success<GroupSyncResponse> -> {
                    //println("GroupIdSync sync response: ${groupSyncResponse.data.toString()}")

                    groupSyncResponse.data.updatedGroups.forEach { groupResponse ->
                        val existing = groupLocalDataSource.getGroupById(groupResponse.id)
                        groupLocalDataSource.upsertGroup(groupResponse.toGroup(existing))

                        //Update profile picture if group is new or the profile pic got updated
                        if (existing == null || existing.profilePicUpdatedAt < groupResponse.profilePicUpdatedAt) {
                            profilepicsToGet += groupResponse.id
                        }
                    }


                    deletedGroupIds = groupSyncResponse.data.deletedGroups
                    deletedGroupIds.forEach { id ->
                        groupLocalDataSource.deleteGroup(id)
                    }
                }
            }

            if (profilepicsToGet.isNotEmpty()) {
                getProfilePicturesForGroupIds(profilepicsToGet)
            }

            return deletedGroupIds
        }
    }

    override suspend fun getProfilePicturesForGroupIds(groupIds: List<String>){
        groupIds.forEach { groupId ->
            val savefilename = groupId + GROUPPROFILEPICTURE_FILE_NAME
            when (val picture = groupRemoteDataSource.getProfilePicForGroupId(groupId).trackConnectivity()) {
                is DataResult.Error<*> -> {loggingRepository.logWarning("Profilepic error for groupid $groupId")}
                is DataResult.Success<ByteArray> -> {
                    val filepath = pictureManager.savePictureToStorage(picture.data, savefilename)

                    groupLocalDataSource.updateGroupProfilePicUrl(groupId, filepath)
                }
            }
        }
    }

    /**
     * Get missing group profile pics (On ios after update installation, there are no more pictures).
     * Group part of the MEDIA job in DataSyncCoordinator.dataSync (was AppRepository.getMissingPics).
     */
    override suspend fun fetchMissingGroupPictures() {
        // Check group profile pics
        val groups = groupLocalDataSource.getAllGroups()
        val missingPpGroupIds = groups.filter { group ->
            !pictureManager.checkImageExists(group.profilePictureUrl)
        }.map { it.id }

        if (missingPpGroupIds.isNotEmpty()) {
            getProfilePicturesForGroupIds(missingPpGroupIds)
        }
    }


    // ─── Group changes ────────────────────────────────────────────────────────

    override suspend fun changeGroupProfilePic(groupId: String, newPic: ByteArray): EmptyDataResult<DataError> =
        groupRemoteDataSource.changeGroupProfilePic(newPic, groupId).trackConnectivity()
            // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
            .onSuccess {
                syncGroups()
            }
            .asEmptyDataResult()

    override suspend fun changeGroupDescription(groupId: String, newDescription: String): EmptyDataResult<DataError> =
        groupRemoteDataSource.changeGroupDescription(newDescription, groupId).trackConnectivity()
            // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
            .onSuccess {
                syncGroups()
            }
            .asEmptyDataResult()

    override suspend fun changeGroupName(groupId: String, newName: String): EmptyDataResult<DataError> =
        groupRemoteDataSource.changeGroupName(newName, groupId).trackConnectivity()
            // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
            .onSuccess {
                syncGroups()
            }
            .asEmptyDataResult()

    override suspend fun changeGroupMembers(action: GroupMemberAction, memberId: String, groupId: String): EmptyDataResult<DataError> =
        groupRemoteDataSource.changeGroupMembers(
            action = action.toNetworkGroupMemberAction(),
            memberId = memberId,
            groupId = groupId
        ).trackConnectivity()
            // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
            .onSuccess {
                syncGroups()
            }
            .asEmptyDataResult()

    /** Pass null for [expiresAt] to clear the expiry (group never auto-expires). */
    override suspend fun changeGroupExpiry(groupId: String, expiresAt: Long?): EmptyDataResult<DataError> =
        groupRemoteDataSource.changeGroupExpiry(groupId, expiresAt).trackConnectivity()
            // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
            .onSuccess {
                syncGroups()
            }
            .asEmptyDataResult()

    /**
     * The local delete is only an optimistic fast path (it also purges the group's messages);
     * the server's GroupChange push and the next group sync converge on the same result.
     */
    override suspend fun deleteGroup(groupId: String): EmptyDataResult<DataError> =
        groupRemoteDataSource.deleteGroup(groupId).trackConnectivity()
            .onSuccess {
                groupLocalDataSource.deleteGroup(groupId)
            }
            .asEmptyDataResult()
}

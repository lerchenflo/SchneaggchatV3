package org.lerchenflo.schneaggchatv3mp.chat.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.lerchenflo.schneaggchatv3mp.GROUPPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorEvent
import org.lerchenflo.schneaggchatv3mp.chat.data.local.GroupLocalDataSource
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMemberAction
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.toNetworkGroupMemberAction
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.GroupResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.GroupSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.GroupRemoteDataSource

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
    private val errorChannel: ErrorChannel,
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
    ) : String? {
        val response = groupRemoteDataSource.createGroup(
            name = name,
            description = description,
            memberIds = memberIds,
            profilePicBytes = profilePic
        ).trackConnectivity()

        return when (response){
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = response.error))
                null
            }
            is NetworkResult.Success<GroupResponse> -> {

                val groupResponse = response.data

                groupLocalDataSource.upsertGroup(Group(
                    id = groupResponse.id,
                    name = groupResponse.name,
                    profilePictureUrl = "",
                    description = groupResponse.description,
                    createDate = groupResponse.createdAt,
                    updatedAt = groupResponse.updatedAt,
                    profilePicUpdatedAt = groupResponse.profilePicUpdatedAt,
                    expiresAt = groupResponse.expiresAt,
                    notisMuted = false,
                    members = groupResponse.members.map { groupMemberresp ->
                        GroupMember(
                            groupId = groupResponse.id,
                            userId = groupMemberresp.userid,
                            joinDate = groupMemberresp.joinedAt,
                            admin = groupMemberresp.admin,
                            color = groupMemberresp.color,
                            memberName = groupMemberresp.memberName
                        )
                    }
                ))

                response.data.id
            }
        }
    }


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

            val userId = SessionCache.requireLoggedIn() ?: return emptyList()


            val localgroups = groupLocalDataSource.getgroupchangeid()
            val profilepicsToGet = mutableListOf<String>()

            val groupSyncResponse = groupRemoteDataSource.groupIdSync(
                groupIds = localgroups.map {
                    IdTimeStamp(it.id, it.updatedAt)
                }
            ).trackConnectivity()

            var deletedGroupIds: List<String> = emptyList()

            when (groupSyncResponse) {
                is NetworkResult.Error<*> -> {
                    loggingRepository.logWarning("groupid sync error: ${groupSyncResponse.error}")
                }

                is NetworkResult.Success<GroupSyncResponse> -> {
                    //println("GroupIdSync sync response: ${groupSyncResponse.data.toString()}")

                    groupSyncResponse.data.updatedGroups.forEach { groupResponse ->
                        val existing = groupLocalDataSource.getGroupById(groupResponse.id)
                        groupLocalDataSource.upsertGroup(Group(
                            id = groupResponse.id,
                            name = groupResponse.name,
                            profilePictureUrl = existing?.profilePictureUrl ?: "",
                            description = groupResponse.description,
                            createDate = groupResponse.createdAt,
                            updatedAt = groupResponse.updatedAt,
                            profilePicUpdatedAt = groupResponse.profilePicUpdatedAt,
                            expiresAt = groupResponse.expiresAt,
                            notisMuted = existing?.notisMuted ?: false,
                            members = groupResponse.members.map { groupMemberresp ->
                                GroupMember(
                                    groupId = groupResponse.id,
                                    userId = groupMemberresp.userid,
                                    joinDate = groupMemberresp.joinedAt,
                                    admin = groupMemberresp.admin,
                                    color = groupMemberresp.color,
                                    memberName = groupMemberresp.memberName
                                    )
                            }
                        ))

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
                is NetworkResult.Error<*> -> {loggingRepository.logWarning("Profilepic error for groupid $groupId")}
                is NetworkResult.Success<ByteArray> -> {
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

    override suspend fun changeGroupProfilePic(groupId: String, newPic: ByteArray) : Boolean {
        when (val success = groupRemoteDataSource.changeGroupProfilePic(newPic, groupId).trackConnectivity()){
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = success.error))
                return false
            }
            is NetworkResult.Success<*> -> {
                // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
                syncGroups()
                return true
            }
        }
    }

    override suspend fun changeGroupDescription(groupId: String, newDescription: String) : Boolean {
        when (val success = groupRemoteDataSource.changeGroupDescription(newDescription, groupId).trackConnectivity()){
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = success.error))
                return false
            }
            is NetworkResult.Success<*> -> {
                // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
                syncGroups()
                return true
            }
        }
    }

    override suspend fun changeGroupName(groupId: String, newName: String) : Boolean {
        when (val success = groupRemoteDataSource.changeGroupName(newName, groupId).trackConnectivity()){
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = success.error))
                return false
            }
            is NetworkResult.Success<*> -> {
                // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
                syncGroups()
                return true
            }
        }
    }

    override suspend fun changeGroupMembers(action: GroupMemberAction, memberId: String, groupId: String) : Boolean {
        when (val success = groupRemoteDataSource.changeGroupMembers(
            action = action.toNetworkGroupMemberAction(),
            memberId = memberId,
            groupId = groupId
        ).trackConnectivity()){
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = success.error))
                return false
            }
            is NetworkResult.Success<*> -> {
                // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
                syncGroups()
                return true
            }
        }
    }

    /** Pass null for [expiresAt] to clear the expiry (group never auto-expires). */
    override suspend fun changeGroupExpiry(groupId: String, expiresAt: Long?) : Boolean {
        when (val success = groupRemoteDataSource.changeGroupExpiry(groupId, expiresAt).trackConnectivity()){
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = success.error))
                return false
            }
            is NetworkResult.Success<*> -> {
                // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
                syncGroups()
                return true
            }
        }
    }
}

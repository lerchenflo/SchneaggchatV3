package org.lerchenflo.schneaggchatv3mp.chat.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.local.UserLocalDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.UserEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.toEntity
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorEvent
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.AvailableUser
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.chat.domain.SnailTrailPoint
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.WakeResult
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.toAvailableUser
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.toSyncedSettings
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.toWakeResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.map
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.NetworkFriendshipStatus
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.NewFriendsUserResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.UserResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.UserSyncResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.UserRemoteDataSource

/**
 * [UserRepository] on top of the local [UserLocalDataSource] and the server via [UserRemoteDataSource].
 * [syncUsers] writes the user rows through [AppDatabase.userDao] directly (moved verbatim from
 * AppRepository.userIdSync) and hands the own user's synced settings to [SettingsRepository].
 */
class OfflineFirstUserRepository(
    private val database: AppDatabase,
    private val userLocalDataSource: UserLocalDataSource,
    private val userRemoteDataSource: UserRemoteDataSource,
    private val pictureManager: PictureManager,
    private val settingsRepository: SettingsRepository,
    private val loggingRepository: Logger,
    private val errorChannel: ErrorChannel,
) : UserRepository {

    // ─── Reads ────────────────────────────────────────────────────────────────

    override fun getUserByIdFlow(userId: String): Flow<User?> =
        userLocalDataSource.getUserFlow(userId)

    override suspend fun getUserById(userId: String): User? =
        userLocalDataSource.getUserById(userId)

    override fun getAllUsersFlow(searchTerm: String): Flow<List<User>> =
        userLocalDataSource.getAllUsersFlow(searchTerm)

    override suspend fun getFriends(searchTerm: String): List<User> {
        return userLocalDataSource.getAllUsers().filter {
            it.name.contains(searchTerm, ignoreCase = true) // like getFriendsFlow's SQL LIKE
                    && it.friendshipStatus == FriendshipStatus.ACCEPTED
        }
    }

    override fun getFriendsFlow(searchTerm: String): Flow<List<User>> {
        return userLocalDataSource.getAllUsersFlow(searchTerm).map { users ->
            users.filter {
                it.friendshipStatus == FriendshipStatus.ACCEPTED
            }
        }
    }

    override fun getSnailTrailFlow(userId: String): Flow<List<SnailTrailPoint>> {
        return userLocalDataSource.getSnailTrailFlow(userId)
    }

    override val onlineFriendIdsFlow: StateFlow<Set<String>>
        get() = userLocalDataSource.onlineFriendIdsFlow

    // ─── User sync ────────────────────────────────────────────────────────────

    /**
     * The user mutations below run their own [syncUsers] outside DataSyncCoordinator's dataSyncLock
     * (D16/R1), so a full sync and a mutation's sync could otherwise run /users/sync at the same
     * time. This lock makes the second one wait. Lock order is always dataSyncLock -> this lock,
     * and nothing inside [syncUsers] calls [syncUsers] again (a Mutex is not reentrant).
     */
    private val userSyncLock = Mutex()

    /**
     * Execute a useridsync
     */
    override suspend fun syncUsers() {
        userSyncLock.withLock {

            SessionCache.requireLoggedIn() ?: return

            val localusers = userLocalDataSource.getuserchangeid()

            val userSyncResponse = userRemoteDataSource.userIdSync(localusers.map { (id, changedate) -> IdTimeStamp(id, changedate) }).trackConnectivity()

            val profilePicsToGet = emptyList<String>().toMutableList()

            when (userSyncResponse) {
                is NetworkResult.Error<*> -> {
                    loggingRepository.logWarning("userid sync error: ${userSyncResponse.error}")
                }
                is NetworkResult.Success<UserSyncResponse> -> {
                    //println("Userid sync response: ${userSyncResponse.data.toString()}")

                    val updatedUsers = userSyncResponse.data.updatedUsers
                    val deletedUsers = userSyncResponse.data.deletedUsers

                    updatedUsers.forEach { newUser ->
                        when (newUser) {
                            is UserResponse.FriendUserResponse -> {
                                val existing = database.userDao().getUserbyId(newUser.id)
                                database.userDao().upsert(UserEntity(
                                    id = newUser.id,
                                    updatedAt = newUser.updatedAt,
                                    name = newUser.username,
                                    nickName = newUser.nickName,
                                    description = newUser.userDescription,
                                    status = newUser.userStatus,
                                    birthDate = newUser.birthDate,
                                    phoneNumber = newUser.phoneNumber,
                                    frienshipStatus = NetworkFriendshipStatus.ACCEPTED,
                                    requesterId = newUser.requesterId,

                                    // Preserve existing values:
                                    locationLat = existing?.locationLat,
                                    locationLong = existing?.locationLong,
                                    locationDate = existing?.locationDate,
                                    locationSpeed = existing?.locationSpeed,
                                    locationHeading = existing?.locationHeading,
                                    locationAltitude = existing?.locationAltitude,
                                    locationBattery = existing?.locationBattery,
                                    locationDistance24h = existing?.locationDistance24h,
                                    locationShared = newUser.shareLocation,
                                    shareSpeedHeading = newUser.shareSpeedHeading,
                                    snailTrail = newUser.shareSnailTrail,
                                    //On a friend's row this means "this friend may wake me"
                                    wakeupEnabled = newUser.allowWake,
                                    notisMuted = existing?.notisMuted ?: false,
                                    lastSeen = newUser.lastSeen,
                                    email = null,
                                    emailVerifiedAt = null,
                                    createdAt = null,
                                    //Local file path, the server never sends it. Clearing it here would
                                    //blank the picture on every unrelated user change (e.g. lastSeen)
                                    profilePictureUrl = existing?.profilePictureUrl ?: "",
                                    profilePicUpdatedAt = newUser.profilePicUpdatedAt,
                                ))
                                if (existing == null || existing.profilePicUpdatedAt < newUser.profilePicUpdatedAt) {
                                    profilePicsToGet += newUser.id
                                }
                            }
                            is UserResponse.SelfUserResponse -> {
                                val existing = database.userDao().getUserbyId(newUser.id)
                                database.userDao().upsert(UserEntity(
                                    id = newUser.id,
                                    updatedAt = newUser.updatedAt,
                                    name = newUser.username,
                                    description = newUser.userDescription,
                                    status = newUser.userStatus,
                                    birthDate = newUser.birthDate,
                                    phoneNumber = newUser.phoneNumber,

                                    // Preserve existing values:
                                    locationLat = existing?.locationLat,
                                    locationLong = existing?.locationLong,
                                    locationDate = existing?.locationDate,
                                    locationSpeed = existing?.locationSpeed,
                                    locationHeading = existing?.locationHeading,
                                    locationAltitude = existing?.locationAltitude,
                                    locationBattery = existing?.locationBattery,
                                    locationDistance24h = existing?.locationDistance24h,
                                    locationShared = newUser.locationShared,
                                    //On my own row this is the master wake switch
                                    wakeupEnabled = newUser.allowWakeGlobal,
                                    frienshipStatus = null,
                                    requesterId = null,
                                    notisMuted = false,
                                    email = newUser.email,
                                    emailVerifiedAt = newUser.emailVerifiedAt,
                                    createdAt = newUser.createdAt,
                                    profilePictureUrl = existing?.profilePictureUrl ?: "",
                                    profilePicUpdatedAt = newUser.profilePicUpdatedAt,
                                    ))
                                if (existing == null || existing.profilePicUpdatedAt < newUser.profilePicUpdatedAt) {
                                    profilePicsToGet += newUser.id
                                }

                                settingsRepository.applyServerSettings(newUser.settings.toSyncedSettings())
                            }
                            is UserResponse.SimpleUserResponse -> {
                                val existing = database.userDao().getUserbyId(newUser.id)
                                database.userDao().upsert(UserEntity(
                                    id = newUser.id,
                                    updatedAt = newUser.updatedAt,
                                    name = newUser.username,
                                    description = null,
                                    status = null,

                                    locationLat = null,
                                    locationLong = null,
                                    locationDate = null,
                                    locationShared = false,
                                    wakeupEnabled = false,
                                    frienshipStatus = newUser.friendShipStatus,
                                    requesterId = newUser.requesterId,
                                    notisMuted = false,
                                    birthDate = null,
                                    phoneNumber = null,
                                    email = null,
                                    emailVerifiedAt = null,
                                    createdAt = null,
                                    profilePictureUrl = existing?.profilePictureUrl ?: "",
                                    profilePicUpdatedAt = newUser.profilePicUpdatedAt
                                    ))

                                if (existing == null || existing.profilePicUpdatedAt < newUser.profilePicUpdatedAt) {
                                    profilePicsToGet += newUser.id
                                }

                            }
                        }

                    }

                    //Delete all non existing users
                    deletedUsers.forEach { userId ->
                        database.userDao().delete(userId)
                    }
                }
            }
            //println("profilepics to get count: ${profilePicsToGet.size}")

            if (profilePicsToGet.isNotEmpty()) {
                getProfilePicturesForUserIds(profilePicsToGet)
            }
        }
    }

    /**
     * Get the profile pics for all passed user ids from the server
     */
    override suspend fun getProfilePicturesForUserIds(userIds: List<String>){
        userIds.forEach { userId ->
            val savefilename = userId + USERPROFILEPICTURE_FILE_NAME
            when (val picture = userRemoteDataSource.getProfilePicForUserId(userId).trackConnectivity()) {
                is NetworkResult.Error<*> -> {loggingRepository.logWarning("Profilepic error for userid $userId")}
                is NetworkResult.Success<ByteArray> -> {
                    val filepath = pictureManager.savePictureToStorage(picture.data, savefilename)

                    userLocalDataSource.updateUserProfilePicUrl(userId, filepath)
                }
            }
        }
    }

    /**
     * Get missing profile pics (On ios after update installation, there are no more pictures).
     * User part of the MEDIA job in DataSyncCoordinator.dataSync (was AppRepository.getMissingPics).
     */
    override suspend fun fetchMissingProfilePictures() {
        // Check user profile pics
        val users = userLocalDataSource.getAllUsers()
        val missingPpUserIds = users.filter { user ->
            !pictureManager.checkImageExists(user.profilePictureUrl)
        }.map { it.id }

        if (missingPpUserIds.isNotEmpty()) {
            getProfilePicturesForUserIds(missingPpUserIds)
        }
    }


    // ─── Friends / profile ────────────────────────────────────────────────────

    /**
     * Get all availavble users from the server
     * @param searchTerm the searchterm which the user entered
     */
    override suspend fun getAvailableUsers(searchTerm: String) : List<AvailableUser> {
        return when (val response = userRemoteDataSource.getAvailableUsers(searchTerm).trackConnectivity()) {
            is NetworkResult.Error<NetworkingError> -> {
                errorChannel.sendErrorSuspend(
                    ErrorEvent(
                        error = response.error
                    )
                )
                emptyList()
            }
            is NetworkResult.Success<List<NewFriendsUserResponse>> -> {
                response.data.map { it.toAvailableUser() }
            }
        }

    }

    /**
     * Send or accept friend request from / to an userid
     */
    override suspend fun sendFriendRequest(friendId: String) : Boolean {
        when (val success = userRemoteDataSource.sendFriendRequest(friendId).trackConnectivity()){
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = success.error))
                return false
            }
            is NetworkResult.Success<*> -> {
                // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
                syncUsers()
                return true
            }
        }
    }

    /**
     * deny a friendrequest from an userid / cancel an outgoing friend request to an userid
     */
    override suspend fun denyFriendRequest(friendId: String) : Boolean {
        when (val success = userRemoteDataSource.denyFriendRequest(friendId).trackConnectivity()){
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = success.error))
                return false
            }
            is NetworkResult.Success<*> -> {
                // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
                syncUsers()
                return true
            }
        }
    }


    override suspend fun removeFriend(friendId: String) : Boolean {
        when (val success = userRemoteDataSource.removeFriend(friendId).trackConnectivity()){
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = success.error))
                return false
            }
            is NetworkResult.Success<*> -> {
                // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
                syncUsers()
                return true
            }
        }
    }


    override suspend fun changeUsername(newUsername: String) : Boolean {
        when (val success = userRemoteDataSource.changeUsername(newUsername).trackConnectivity()){
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = success.error))
                return false
            }
            is NetworkResult.Success<*> -> {
                // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
                syncUsers()
                return true
            }
        }
    }

    override suspend fun changeProfilePic(newPic: ByteArray) : Boolean {
        when (val success = userRemoteDataSource.changeProfilePic(newPic).trackConnectivity()){
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = success.error))
                return false
            }
            is NetworkResult.Success<*> -> {
                // D16/R1: own sync instead of the full dataSync; can go once SchneaggchatV3server/Todo.md confirms the socket push.
                syncUsers()
                return true
            }
        }
    }

    /**
     * Change status and description. if userid = self then only status, else only description
     */
    override suspend fun changeUserDetails(
        userId: String,
        newStatus: String?,
        newDescription: String?,
        newEmail: String?,
        newBirthDate: String?,
        newNickName: String?,
        newPhoneNumber: String?,
        ) : Boolean {
        return when (val success = userRemoteDataSource.changeProfile(
            userId = userId,
            newStatus = newStatus,
            newDescription = newDescription,
            newEmail = newEmail,
            newBirthDate = newBirthDate,
            newNickName = newNickName,
            newPhoneNumber = newPhoneNumber,
        ).trackConnectivity()){
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = success.error))
                false
            }
            is NetworkResult.Success<*> -> {
                //dataSync()
                true
            }
        }
    }


    // ─── Location sharing ─────────────────────────────────────────────────────

    // Location data itself is now pushed/pulled over the WebSocket (see GlobalViewModel's
    // location-tracking job and SocketConnectionMessage's LocationUpdate/FriendLocationChange/
    // FriendLocationsSnapshot handling), not via HTTP.

    /** Per-friend toggle: what we share with this specific friend (location, speed/heading, snail trail). */
    override suspend fun setLocationSharing(
        friendId: String,
        share: Boolean,
        shareSpeedHeading: Boolean,
        snailTrail: Boolean,
    ): Boolean {
        return when (val result = userRemoteDataSource.shareLocation(friendId, share, shareSpeedHeading, snailTrail).trackConnectivity()) {
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = result.error))
                false
            }
            is NetworkResult.Success<*> -> {
                // Optimistically reflect the new value locally so UI updates immediately;
                // the next /users/sync or UserChange socket push reconciles the authoritative value.
                userLocalDataSource.getUserById(friendId)?.let { friend ->
                    userLocalDataSource.upsertUser(friend.copy(
                        locationShared = share,
                        shareSpeedHeading = shareSpeedHeading,
                        snailTrail = snailTrail,
                    ).toEntity())
                }
                true
            }
        }
    }

    /**
     * Disables location sharing towards every friend. User.locationShared is derived
     * server-side from whether any friend has share=true, so this is the real way to turn
     * sharing off entirely (there's no standalone global switch to write to anymore).
     */
    override suspend fun disableLocationSharingForAllFriends(): Boolean {
        return getFriends("").all { friend ->
            setLocationSharing(friend.id, share = false, friend.shareSpeedHeading, friend.snailTrail)
        }
    }

    // ─── Wake ─────────────────────────────────────────────────────────────────

    /**
     * Per-friend toggle: may this friend wake us. Stored on the friend's local row as
     * [UserEntity.wakeupEnabled] - on our own row that same column is the master switch instead.
     */
    override suspend fun setWakePermission(friendId: String, allowWake: Boolean): Boolean {
        return when (val result = userRemoteDataSource.setWakePermission(friendId, allowWake).trackConnectivity()) {
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = result.error))
                false
            }
            is NetworkResult.Success<*> -> {
                // Optimistic local write, reconciled by the next /users/sync or UserChange push.
                userLocalDataSource.getUserById(friendId)?.let { friend ->
                    userLocalDataSource.upsertUser(friend.copy(wakeupEnabled = allowWake).toEntity())
                }
                true
            }
        }
    }

    /** Master switch: while off nobody can wake us, whatever the per-friend toggles say. */
    override suspend fun setWakeGlobal(enabled: Boolean): Boolean {
        return when (val result = userRemoteDataSource.setWakeGlobal(enabled).trackConnectivity()) {
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = result.error))
                false
            }
            is NetworkResult.Success<*> -> {
                SessionCache.requireLoggedIn()?.userId?.let { ownId ->
                    userLocalDataSource.getUserById(ownId)?.let { own ->
                        userLocalDataSource.upsertUser(own.copy(wakeupEnabled = enabled).toEntity())
                    }
                }
                true
            }
        }
    }


    /**
     * Ask the server to wake a friend or a group. A successful response can still report that
     * nobody was reachable - see WakeOutcome. Errors are passed through rather than flattened,
     * because the caller needs to tell "not friends" (403) apart from a plain network failure.
     */
    override suspend fun sendWake(
        targetId: String,
        isGroup: Boolean,
        reason: String,
    ): NetworkResult<WakeResult, NetworkingError> {
        return userRemoteDataSource.sendWake(targetId, isGroup, reason).trackConnectivity().map { it.toWakeResult() }
    }
}

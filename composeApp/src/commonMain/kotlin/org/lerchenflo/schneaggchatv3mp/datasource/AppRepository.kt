@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package org.lerchenflo.schneaggchatv3mp.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageSearchResult
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatFilter
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MessageContent
import org.lerchenflo.schneaggchatv3mp.datasource.domain.AppInfoRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.AuthRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.DataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.datasource.domain.EventRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.MapRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.MessageRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.SessionTerminator
import org.lerchenflo.schneaggchatv3mp.datasource.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.PersonalUserSettings
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toSyncedSettings
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.ThemeSetting
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.AvailableUser
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DataSyncState
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DeviceSession
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.EventType
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.EventVisibility
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.GroupMemberAction
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.GroupDeleteDelay
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.LatLong
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.LocationData
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.PollVote
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.SavedCredsResult
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.SnailTrailPoint
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.User
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.WakeResult
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.ChangelogEntry
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.DecodedNotification
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

class AppRepository(
    val appVersion: AppVersion,
    private val mapRepository: MapRepository,
    private val eventRepository: EventRepository,
    private val appInfoRepository: AppInfoRepository,
    private val authRepository: AuthRepository,
    private val sessionTerminator: SessionTerminator,
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val chatListRepository: ChatListRepository,
    private val messageRepository: MessageRepository,
    private val dataSyncCoordinator: DataSyncCoordinator,
) {
    /*
    **************************************************************************

    Common

    **************************************************************************
     */

    // ─── Auth / teardown: moved to AuthRepository and SessionTerminator ────────
    // Thin delegates until every caller uses the interfaces directly (removed with this class).

    suspend fun setNotificationToken(token: String) = authRepository.setNotificationToken(token)

    suspend fun sendEmailVerify() : Boolean = authRepository.sendEmailVerify()

    suspend fun deleteAllAppData() = sessionTerminator.deleteAllAppData()

    suspend fun logout(endSessionOnServer: Boolean = true) = sessionTerminator.logout(endSessionOnServer)

    // ─── App info: moved to AppInfoRepository ──────────────────────────────────
    // Thin delegates until every caller uses AppInfoRepository directly (removed with this class).

    suspend fun testServer(serverUrl: String = BASE_SERVER_URL) : Boolean =
        appInfoRepository.testServer(serverUrl)

    suspend fun getChangeLog(version: String) : ChangelogEntry? = appInfoRepository.getChangeLog(version)

    suspend fun getLatestGitHubVersionAsString(): String? = appInfoRepository.getLatestGitHubVersionAsString()


    // ─── Data sync: moved to DataSyncCoordinator ───────────────────────────────
    // Thin delegates until every caller uses DataSyncCoordinator directly (removed with this class).

    val dataSyncState: StateFlow<DataSyncState> = dataSyncCoordinator.dataSyncState

    suspend fun dataSync(reason: String) = dataSyncCoordinator.dataSync(reason)


    // ─── Map / Event: moved to MapRepository / EventRepository ─────────────────
    // Thin delegates until every caller uses the repositories directly (removed with this class).

    suspend fun upsertMapEntry(
        entryId: String?,
        name: String,
        description: String,
        lat: Double,
        lon: Double,
        locationData: List<LocationData>,
    ) = mapRepository.upsertMapEntry(entryId, name, description, lat, lon, locationData)

    suspend fun deleteMapEntry(entryId: String) : Boolean = mapRepository.deleteMapEntry(entryId)

    suspend fun upsertEvent(
        eventId: String?,
        type: EventType,
        title: String,
        description: String,
        createGroup: Boolean,
        location: LatLong?,
        startDate: Long,
        closeDate: Long?,
        invitedUsers: List<String>,
        visibility: EventVisibility,
        maxUsers: Int?,
        groupDeleteDelay: GroupDeleteDelay,
        profilePic: ByteArray? = null,
    ): Boolean = eventRepository.upsertEvent(
        eventId = eventId,
        type = type,
        title = title,
        description = description,
        createGroup = createGroup,
        location = location,
        startDate = startDate,
        closeDate = closeDate,
        invitedUsers = invitedUsers,
        visibility = visibility,
        maxUsers = maxUsers,
        groupDeleteDelay = groupDeleteDelay,
        profilePic = profilePic,
    )

    suspend fun setEventParticipation(eventId: String, status: EventParticipationStatus): Boolean =
        eventRepository.setEventParticipation(eventId, status)

    suspend fun joinEvent(eventId: String): String? = eventRepository.joinEvent(eventId)

    suspend fun detachEvent(eventId: String, groupId: String? = null, deleteGroup: Boolean = false, deleteEvent: Boolean = false): Boolean =
        eventRepository.detachEvent(eventId, groupId, deleteGroup, deleteEvent)


    // ─── Chat list / search: moved to ChatListRepository ───────────────────────
    // Thin delegates until every caller uses ChatListRepository directly (removed with this class).

    fun getPendingFriends(searchTerm: String): Flow<List<ChatListItem>> =
        chatListRepository.getPendingFriends(searchTerm)

    fun getUnreadChatCountFlow(): Flow<Int> = chatListRepository.getUnreadChatCountFlow()

    fun getChatListFlow(userId: String): Flow<List<ChatListItem>> = chatListRepository.getChatListFlow(userId)

    fun getChatSelectorFlow(
        searchTerm: String,
        userId: String,
        filter: ChatFilter = ChatFilter.NONE
    ): Flow<List<ChatListItem>> = chatListRepository.getChatSelectorFlow(searchTerm, userId, filter)

    fun getMessageSearchFlow(searchTerm: String, userId: String): Flow<List<MessageSearchResult>> =
        chatListRepository.getMessageSearchFlow(searchTerm, userId)

    suspend fun setPinnedChat(chatId: String, group: Boolean, pinned: Boolean) =
        chatListRepository.setPinnedChat(chatId, group, pinned)


    // ─── Users / friends: moved to UserRepository ──────────────────────────────
    // Thin delegates until every caller uses UserRepository directly (removed with this class).

    fun getUserByIdFlow(userId: String): Flow<User?> = userRepository.getUserByIdFlow(userId)

    suspend fun getUserById(userId: String): User? = userRepository.getUserById(userId)

    suspend fun getFriends(searchTerm: String): List<User> = userRepository.getFriends(searchTerm)

    fun getFriendsFlow(searchTerm: String): Flow<List<User>> = userRepository.getFriendsFlow(searchTerm)

    fun getSnailTrailFlow(userId: String): Flow<List<SnailTrailPoint>> = userRepository.getSnailTrailFlow(userId)


    /*
    **************************************************************************

    Auth

    **************************************************************************
     */


    // ─── Login / registration: moved to AuthRepository ─────────────────────────
    // Thin delegates until every caller uses AuthRepository directly (removed with this class).
    // onNewTokenPair is private to RemoteAuthRepository now (it had no caller outside login).

    suspend fun loadSavedLoginConfig(): SavedCredsResult = authRepository.loadSavedLoginConfig()

    suspend fun login(
        username: String,
        password: String,
        onResult: (Boolean) -> Unit
    ) = authRepository.login(username, password, onResult)

    suspend fun createAccount(
        username: String,
        email: String,
        password: String,
        birthdate: String,
        profilePic: ByteArray,
        phoneNumber: String? = null,
        language: String? = null,
        onResult: (Boolean) -> Unit
    ) = authRepository.createAccount(username, email, password, birthdate, profilePic, phoneNumber, language, onResult)


    /*
    **************************************************************************

    User

    **************************************************************************
     */

    // ─── Users / friends: moved to UserRepository ──────────────────────────────
    // Thin delegates until every caller uses UserRepository directly (removed with this class).

    suspend fun userIdSync() = userRepository.syncUsers()

    suspend fun getProfilePicturesForUserIds(userIds: List<String>) =
        userRepository.getProfilePicturesForUserIds(userIds)

    suspend fun getAvailableUsers(searchTerm: String) : List<AvailableUser> =
        userRepository.getAvailableUsers(searchTerm)

    suspend fun sendFriendRequest(friendId: String) : Boolean = userRepository.sendFriendRequest(friendId)

    suspend fun denyFriendRequest(friendId: String) : Boolean = userRepository.denyFriendRequest(friendId)

    suspend fun removeFriend(friendId: String) : Boolean = userRepository.removeFriend(friendId)

    suspend fun changeUsername(newUsername: String) : Boolean = userRepository.changeUsername(newUsername)


    // ─── Password / sessions: moved to AuthRepository and SessionTerminator ─────
    // Thin delegates until every caller uses the interfaces directly (removed with this class).

    suspend fun changePassword(oldPassword: String, newPassword: String) : Boolean =
        authRepository.changePassword(oldPassword, newPassword)

    suspend fun getSessions(): List<DeviceSession>? = authRepository.getSessions()

    suspend fun logoutAllDevices(): Boolean = sessionTerminator.logoutAllDevices()

    suspend fun endSession(sessionId: String): Boolean = authRepository.endSession(sessionId)


    suspend fun changeProfilePic(newPic: ByteArray) : Boolean = userRepository.changeProfilePic(newPic)


    suspend fun changeUserDetails(
        userId: String,
        newStatus: String? = null,
        newDescription: String? = null,
        newEmail: String? = null,
        newBirthDate: String? = null,
        newNickName: String? = null,
        newPhoneNumber: String? = null,
    ) : Boolean = userRepository.changeUserDetails(
        userId = userId,
        newStatus = newStatus,
        newDescription = newDescription,
        newEmail = newEmail,
        newBirthDate = newBirthDate,
        newNickName = newNickName,
        newPhoneNumber = newPhoneNumber,
    )



    /*
        **************************************************************************

        Messages

        **************************************************************************
    */



    // ─── Send / offline queue / sync: moved to MessageRepository ───────────────
    // Thin delegates until every caller uses MessageRepository directly (removed with this class).

    suspend fun sendMessage(ownId: String, messageId: String?, empfaenger: String, gruppe: Boolean, content: MessageContent, answerid: String?, localpk: Long = 0, clientMessageId: String? = null) =
        messageRepository.sendMessage(ownId, messageId, empfaenger, gruppe, content, answerid, localpk, clientMessageId)

    suspend fun sendOfflineMessages(ownId: String) = messageRepository.sendOfflineMessages(ownId)

    suspend fun messageIdSync(fetchMedia: Boolean = true) = messageRepository.syncMessages(fetchMedia)


    // ─── Push apply / mark read: moved to MessageRepository ────────────────────
    // Thin delegates until every caller uses MessageRepository directly (removed with this class).

    suspend fun applyPushMessage(decoded: DecodedNotification.Message) = messageRepository.applyPushMessage(decoded)

    suspend fun setAllChatMessagesRead(chatid: String, gruppe: Boolean, timestamp: String): Boolean =
        messageRepository.setAllChatMessagesRead(chatid, gruppe, timestamp)


    // ─── Message media: moved to MessageRepository ─────────────────────────────
    // Thin delegates until every caller uses MessageRepository directly (removed with this class).

    suspend fun getPicturesForMessageIds(ids: List<String>) = messageRepository.getPicturesForMessageIds(ids)

    suspend fun getAudiosForMessageIds(ids: List<String>) = messageRepository.getAudiosForMessageIds(ids)


    // ─── Edit / delete / react / poll: moved to MessageRepository ──────────────
    // Thin delegates until every caller uses MessageRepository directly (removed with this class).

    suspend fun editMessage(message: Message, newContent: String) = messageRepository.editMessage(message, newContent)

    suspend fun deleteLocalMessage(localpk: Long) = messageRepository.deleteLocalMessage(localpk)

    suspend fun deleteMessage(messageId: String) = messageRepository.deleteMessage(messageId)

    suspend fun reactToMessage(messageId: String, content: String) = messageRepository.reactToMessage(messageId, content)

    suspend fun votePoll(ownId: String, pollVote: PollVote) = messageRepository.votePoll(ownId, pollVote)

    suspend fun deletePollOption(ownId: String, messageId: String, optionId: String) =
        messageRepository.deletePollOption(ownId, messageId, optionId)


    /*
    **************************************************************************

    Groups

    **************************************************************************
*/



    // ─── Groups: moved to GroupRepository ─────────────────────────────────────
    // Thin delegates until every caller uses GroupRepository directly (removed with this class).

    suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>,
        profilePic: ByteArray
    ) : String? = groupRepository.createGroup(name, description, memberIds, profilePic)

    suspend fun groupIdSync(): List<String> = groupRepository.syncGroups()

    suspend fun getProfilePicturesForGroupIds(groupIds: List<String>) =
        groupRepository.getProfilePicturesForGroupIds(groupIds)

    suspend fun changeGroupProfilePic(groupId: String, newPic: ByteArray) : Boolean =
        groupRepository.changeGroupProfilePic(groupId, newPic)

    suspend fun changeGroupDescription(groupId: String, newDescription: String) : Boolean =
        groupRepository.changeGroupDescription(groupId, newDescription)

    suspend fun changeGroupName(groupId: String, newName: String) : Boolean =
        groupRepository.changeGroupName(groupId, newName)

    suspend fun changeGroupMembers(action: GroupMemberAction, memberId: String, groupId: String) : Boolean =
        groupRepository.changeGroupMembers(action, memberId, groupId)

    /** Pass null for [expiresAt] to clear the expiry (group never auto-expires). */
    suspend fun changeGroupExpiry(groupId: String, expiresAt: Long?) : Boolean =
        groupRepository.changeGroupExpiry(groupId, expiresAt)


    // ─── Location sharing / wake: moved to UserRepository ──────────────────────
    // Thin delegates until every caller uses UserRepository directly (removed with this class).

    suspend fun setLocationSharing(
        friendId: String,
        share: Boolean,
        shareSpeedHeading: Boolean = false,
        snailTrail: Boolean = false,
    ): Boolean = userRepository.setLocationSharing(friendId, share, shareSpeedHeading, snailTrail)

    suspend fun disableLocationSharingForAllFriends(): Boolean = userRepository.disableLocationSharingForAllFriends()

    suspend fun setWakePermission(friendId: String, allowWake: Boolean): Boolean =
        userRepository.setWakePermission(friendId, allowWake)

    suspend fun setWakeGlobal(enabled: Boolean): Boolean = userRepository.setWakeGlobal(enabled)

    suspend fun sendWake(
        targetId: String,
        isGroup: Boolean,
        reason: String,
    ): NetworkResult<WakeResult, NetworkingError> = userRepository.sendWake(targetId, isGroup, reason)

    // ─── Personal settings: moved to SettingsRepository ────────────────────────
    // Thin delegates until every caller uses SettingsRepository directly (removed with this class).

    suspend fun applyServerSettings(settings: PersonalUserSettings) =
        settingsRepository.applyServerSettings(settings.toSyncedSettings())

    suspend fun setUseMd(value: Boolean) = settingsRepository.setUseMd(value)

    suspend fun setDevSettings(value: Boolean) = settingsRepository.setDevSettings(value)

    suspend fun setHighlightTodaysMessageTimestamp(value: Boolean) =
        settingsRepository.setHighlightTodaysMessageTimestamp(value)

    suspend fun setThemeSetting(theme: ThemeSetting) = settingsRepository.setThemeSetting(theme)

    suspend fun setLanguageSetting(language: LanguageSetting) = settingsRepository.setLanguageSetting(language)

    suspend fun setMergeMapLocations(value: Boolean) = settingsRepository.setMergeMapLocations(value)

    suspend fun setMergeMapUsers(value: Boolean) = settingsRepository.setMergeMapUsers(value)

    suspend fun setMapStyleSetting(style: MapStyleSetting) = settingsRepository.setMapStyleSetting(style)

    suspend fun setQuickReactions(reactions: List<String>) = settingsRepository.setQuickReactions(reactions)

    suspend fun resetQuickReactions() = settingsRepository.resetQuickReactions()

    suspend fun setLastContributePopupShown(epochMillis: Long) =
        settingsRepository.setLastContributePopupShown(epochMillis)

    /*





    //Network züg
    fun executeSync(onLoadingStateChange: (Boolean) -> Unit) {
        // global handler for uncaught exceptions inside the scope
        val handler = CoroutineExceptionHandler { _, throwable ->
            loggingRepository.logError("WorkManager failed: ${throwable.message}")
        }

        // consider making this a long-lived scope on the repository to avoid leaks;
        // kept here to match your existing pattern
        CoroutineScope(Dispatchers.IO + SupervisorJob() + handler).launch {
            onLoadingStateChange(true)
            try {
                supervisorScope {
                    val msgSync = async {
                        try {
                            // pass a no-op loading lambda because repository manages loading state
                            networkUtils.executeMsgIDSync(messageRepository = messageRepository, onLoadingStateChange = {})
                        } catch (e: Exception) {
                            loggingRepository.logWarning("Delayed action failed: ${e.message}")
                        }
                    }

                    val userSync = async {
                        try {
                            networkUtils.executeUserIDSync(userRepository = userRepository)
                        } catch (e: Exception) {
                            loggingRepository.logWarning("Delayed action failed: ${e.message}")
                        }
                    }

                    val groupSync = async {
                        try {
                            networkUtils.executeGroupIDSync(groupRepository = groupRepository)
                        } catch (e: Exception) {
                            loggingRepository.logWarning("Delayed action failed: ${e.message}")
                        }
                    }

                    // wait for all to finish; exceptions were handled in each async block so awaitAll won't throw
                    awaitAll(msgSync, userSync, groupSync)
                }
            } finally {
                onLoadingStateChange(false)
            }
        }
    }

     */


}

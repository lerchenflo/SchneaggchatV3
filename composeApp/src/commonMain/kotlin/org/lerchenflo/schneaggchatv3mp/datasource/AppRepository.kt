@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package org.lerchenflo.schneaggchatv3mp.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.PICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.VOICEMSG_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupLocalDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageLocalDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.UserLocalDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageSearchResult
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.isNewerThan
import org.lerchenflo.schneaggchatv3mp.chat.domain.Reaction
import org.lerchenflo.schneaggchatv3mp.chat.domain.toChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatFilter
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository.DataSyncJobType.EVENTS
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository.DataSyncJobType.GROUPS
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository.DataSyncJobType.MAP
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository.DataSyncJobType.MESSAGES
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository.DataSyncJobType.USERS
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository.MessageContent.AudioContent
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository.MessageContent.ImageContent
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository.MessageContent.PollContent
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository.MessageContent.TextContent
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.domain.AppInfoRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.AuthRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.datasource.domain.ErrorEvent
import org.lerchenflo.schneaggchatv3mp.datasource.domain.EventRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.domain.MapRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.SessionTerminator
import org.lerchenflo.schneaggchatv3mp.datasource.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.MessageResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.MessageSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.PersonalUserSettings
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.PollCreateRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.PollOptionDeleteRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.PollVoteRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.UserSettingsRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toDomainMessage
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toCreateRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toLocalPollMessage
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toPollMessage
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toSyncedSettings
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.PinnedChat
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.ThemeSetting
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.AvailableUser
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DeviceSession
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.EventType
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.EventVisibility
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.GroupMemberAction
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.GroupDeleteDelay
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.LatLong
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.LocationData
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.SavedCredsResult
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.SnailTrailPoint
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.User
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.WakeResult
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.AudioManager
import org.lerchenflo.schneaggchatv3mp.utilities.ChangelogEntry
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import org.lerchenflo.schneaggchatv3mp.utilities.getAudioBytes
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisLong
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisString
import org.lerchenflo.schneaggchatv3mp.utilities.isBirthdayToday
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.DecodedNotification
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.eventssync
import schneaggchatv3mp.composeapp.generated.resources.groupsync
import schneaggchatv3mp.composeapp.generated.resources.mapsync
import schneaggchatv3mp.composeapp.generated.resources.mediasync
import schneaggchatv3mp.composeapp.generated.resources.messagesync
import schneaggchatv3mp.composeapp.generated.resources.usersync
import kotlin.collections.map
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

//Message search in the chat selector: how much the user has to type before the message table is
//scanned, and how many hits are kept so a common word cannot build a list of thousands of rows.
private const val MESSAGE_SEARCH_MIN_TERM_LENGTH = 2
private const val MESSAGE_SEARCH_RESULT_LIMIT = 50

class AppRepository(
    private val database: AppDatabase,
    private val networkUtils: NetworkUtils,
    private val preferencemanager: Preferencemanager,
    private val pictureManager: PictureManager,
    private val audioManager: AudioManager,

    private val userLocalDataSource: UserLocalDataSource,
    private val groupLocalDataSource: GroupLocalDataSource,
    private val messageLocalDataSource: MessageLocalDataSource,

    val appVersion: AppVersion,
    private val loggingRepository: Logger,
    private val mapRepository: MapRepository,
    private val eventRepository: EventRepository,
    private val appInfoRepository: AppInfoRepository,
    private val authRepository: AuthRepository,
    private val sessionTerminator: SessionTerminator,
    private val errorChannel: ErrorChannel,
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
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


    val dataSyncLock = Mutex()

    enum class DataSyncJobStatus { IDLE, RUNNING, SUCCESS, FAILED }

    enum class DataSyncJobType {
        USERS,
        GROUPS,
        MESSAGES,
        MAP,
        EVENTS,

        MEDIA;

        fun toUiText() : UiText {
            return when (this) {
                USERS -> UiText.StringResourceText(Res.string.usersync)
                GROUPS -> UiText.StringResourceText(Res.string.groupsync)
                MESSAGES -> UiText.StringResourceText(Res.string.messagesync)
                MAP -> UiText.StringResourceText(Res.string.mapsync)
                EVENTS -> UiText.StringResourceText(Res.string.eventssync)
                MEDIA -> UiText.StringResourceText(Res.string.mediasync)
            }
        }
    }

    data class DataSyncJobState(
        val type: DataSyncJobType,
        val status: DataSyncJobStatus = DataSyncJobStatus.IDLE,
        val error: String? = null
    )

    data class DataSyncState(
        val isSyncing: Boolean = false,
        val exitedWithErrors: Boolean = false,
        val jobs: List<DataSyncJobState> = listOf(
            DataSyncJobState(type = USERS), DataSyncJobState(type = GROUPS),
            DataSyncJobState(type = MESSAGES), DataSyncJobState(type = MAP),
            DataSyncJobState(type = EVENTS),
            DataSyncJobState(type = DataSyncJobType.MEDIA)
        )
    )

    private fun updateSyncJob(type: DataSyncJobType, status: DataSyncJobStatus, error: String? = null) {
        _dataSyncState.update { current ->
            current.copy(
                jobs = current.jobs.map {
                    if (it.type == type) {
                        it.copy(
                            status = status, error = error
                        )
                    } else it
                },
                exitedWithErrors = if (current.exitedWithErrors) true else error != null //Keep error if one is set
            )
        }
    }

    private val _dataSyncState = MutableStateFlow(DataSyncState())
    val dataSyncState: StateFlow<DataSyncState> = _dataSyncState.asStateFlow()

    /**
     * @param reason short tag identifying what triggered this sync, logged so the origin of a
     * sync run is visible. Keep it stable and call-site specific (e.g. "appResumed").
     */
    suspend fun dataSync(reason: String) {

        SessionCache.requireLoggedIn() ?: return

        if (dataSyncLock.isLocked) {
            println("Datasync started but already running (reason: $reason)")
            dataSyncLock.withLock {  } //Awaiting finish and return to apply normal data sync await logic
            return
        }

        dataSyncLock.withLock {
            _dataSyncState.update {
                it.copy(
                    isSyncing = true,
                    exitedWithErrors = false, //Reset error when synching again
                    // Reset stale statuses from the previous run, otherwise a job that hasn't
                    // started yet (e.g. MEDIA, which runs last) would still show its old
                    // SUCCESS/FAILED status while other jobs are already RUNNING.
                    jobs = it.jobs.map { job -> job.copy(status = DataSyncJobStatus.IDLE, error = null) }
                )
            }
            println("Starting datasync (reason: $reason)")

            try {
                coroutineScope {
                    // Groups deleted this sync - purged again after awaitAll below, since the
                    // parallel messageJob could re-insert rows right after groupIdSync's own
                    // inline purge (GroupLocalDataSource.deleteGroup) ran.
                    val removedGroupIds = mutableListOf<String>()

                    // Launch all sync operations concurrently
                    val userJob = async {
                        try {
                            updateSyncJob(
                                type = USERS,
                                status = DataSyncJobStatus.RUNNING,
                                error = null
                            )
                            userRepository.syncUsers()
                            updateSyncJob(
                                type = USERS,
                                status = DataSyncJobStatus.SUCCESS,
                                error = null
                            )
                            //println("User sync completed successfully")
                        } catch (e: Exception) {
                            ensureActive()
                            updateSyncJob(
                                type = USERS,
                                status = DataSyncJobStatus.FAILED,
                                error = e.message
                            )
                            loggingRepository.logWarning("User sync failed: ${e.message}")
                        }
                    }

                    val messageJob = async {
                        try {
                            updateSyncJob(
                                type = MESSAGES,
                                status = DataSyncJobStatus.RUNNING,
                                error = null
                            )
                            // Media is fetched afterwards as part of the MEDIA job so its
                            // progress indicator covers the actual download work.
                            messageIdSync(fetchMedia = false)
                            updateSyncJob(
                                type = MESSAGES,
                                status = DataSyncJobStatus.SUCCESS,
                                error = null
                            )
                            //println("Message sync completed successfully")
                        } catch (e: Exception) {
                            ensureActive()
                            updateSyncJob(
                                type = MESSAGES,
                                status = DataSyncJobStatus.FAILED,
                                error = e.message
                            )
                            loggingRepository.logWarning("Message sync failed: ${e.message}")
                        }
                    }

                    val groupJob = async {
                        try {
                            updateSyncJob(
                                type = GROUPS,
                                status = DataSyncJobStatus.RUNNING,
                                error = null
                            )
                            removedGroupIds += groupRepository.syncGroups()
                            updateSyncJob(
                                type = GROUPS,
                                status = DataSyncJobStatus.SUCCESS,
                                error = null
                            )
                            //println("Group sync completed successfully")
                        } catch (e: Exception) {
                            ensureActive()
                            updateSyncJob(
                                type = GROUPS,
                                status = DataSyncJobStatus.FAILED,
                                error = e.message
                            )
                            loggingRepository.logWarning("Group sync failed: ${e.message}")
                        }
                    }

                    val mapJob = async {
                        try {
                            updateSyncJob(
                                type = MAP,
                                status = DataSyncJobStatus.RUNNING,
                                error = null
                            )
                            mapRepository.syncMapEntries()
                            updateSyncJob(
                                type = MAP,
                                status = DataSyncJobStatus.SUCCESS,
                                error = null
                            )
                            //println("Map sync completed successfully")
                        } catch (e: Exception) {
                            ensureActive()
                            updateSyncJob(
                                type = MAP,
                                status = DataSyncJobStatus.FAILED,
                                error = e.message
                            )
                            loggingRepository.logWarning("Map sync failed: ${e.message}")
                        }
                    }

                    val eventJob = async {
                        try {
                            updateSyncJob(
                                type = EVENTS,
                                status = DataSyncJobStatus.RUNNING,
                                error = null
                            )
                            eventRepository.syncEvents()
                            updateSyncJob(
                                type = EVENTS,
                                status = DataSyncJobStatus.SUCCESS,
                                error = null
                            )
                        } catch (e: Exception) {
                            ensureActive()
                            updateSyncJob(
                                type = EVENTS,
                                status = DataSyncJobStatus.FAILED,
                                error = e.message
                            )
                            loggingRepository.logWarning("Event sync failed: ${e.message}")
                        }
                    }

                    // Wait for all to complete (errors are already handled)
                    awaitAll(userJob, messageJob, groupJob, mapJob, eventJob)

                    // The message job runs in parallel and may have re-inserted rows just
                    // after groupIdSync's inline purge ran, so repeat it once everything
                    // has settled.
                    removedGroupIds.forEach { messageLocalDataSource.deleteMessagesForGroup(it) }

                    try {
                        updateSyncJob(
                            type = DataSyncJobType.MEDIA,
                            status = DataSyncJobStatus.RUNNING,
                            error = null
                        )
                        getMissingPics()
                        getMissingAudios()
                        updateSyncJob(
                            type = DataSyncJobType.MEDIA,
                            status = DataSyncJobStatus.SUCCESS,
                            error = null
                        )
                    } catch (e: Exception) {
                        currentCoroutineContext().ensureActive()
                        updateSyncJob(
                            type = DataSyncJobType.MEDIA,
                            status = DataSyncJobStatus.FAILED,
                            error = e.message
                        )
                        loggingRepository.logWarning("Media sync failed: ${e.message}")
                    }
                }

                println("Data sync finished, started from $reason")
            } finally {
                _dataSyncState.update {
                    it.copy(
                        isSyncing = false
                    )
                }
            }
        }
    }

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


    /**
     * Get missing profile pics (On ios after update installation, there are no more pictures)
     */
    suspend fun getMissingPics() = coroutineScope {
        // Check user profile pics (UserRepository), in parallel with the parts below
        launch { userRepository.fetchMissingProfilePictures() }

        // Check group profile pics (GroupRepository), in parallel with the parts below
        launch { groupRepository.fetchMissingGroupPictures() }

        // Check image messages
        val messages = messageLocalDataSource.getImageMessages()
        val missingImageMessageIds = messages.filter { image ->
            image.id != null && !pictureManager.checkImageExists(image.pictureUrl ?: "")
        }.map { it.id!! }
        
        if (missingImageMessageIds.isNotEmpty()) {
            launch { getPicturesForMessageIds(missingImageMessageIds) }
        }
    }

    suspend fun getMissingAudios() = coroutineScope {
        // Check audio messages
        val messages = messageLocalDataSource.getAudioMessages()
        val missingAudioMessageIds = messages.filter { audio ->
            if (audio.id == null) return@filter false
            // Null/empty audioPath means the file was never downloaded — always treat as missing.
            // Without this guard, getRecordingPath("") returns filesDir itself, which exists as a
            // directory, so checkAudioExists would wrongly return true and skip the download.
            val filename = audio.audioPath?.substringAfterLast('/')?.takeIf { it.isNotEmpty() }
                ?: return@filter true
            !audioManager.checkAudioExists(audioManager.getRecordingPath(filename))
        }.map { it.id!! }

        //println("Missing audio ids: $missingAudioMessageIds -------------------------------------")
        if (missingAudioMessageIds.isNotEmpty()) {
            launch { getAudiosForMessageIds(missingAudioMessageIds) }
        }
    }


    /*
    **************************************************************************

    Reusable flows

    **************************************************************************
     */
    fun getPendingFriends(searchTerm: String): Flow<List<ChatListItem>> {
        return userLocalDataSource.getAllUsersFlow(searchTerm)
            .map { list ->
                list.filter {
                    it.friendshipStatus == FriendshipStatus.PENDING
                }.map { user ->
                    user.toChatListItem()
                }
            }
    }


    // ─── Users / friends: moved to UserRepository ──────────────────────────────
    // Thin delegates until every caller uses UserRepository directly (removed with this class).

    fun getUserByIdFlow(userId: String): Flow<User?> = userRepository.getUserByIdFlow(userId)

    suspend fun getUserById(userId: String): User? = userRepository.getUserById(userId)

    suspend fun getFriends(searchTerm: String): List<User> = userRepository.getFriends(searchTerm)

    fun getFriendsFlow(searchTerm: String): Flow<List<User>> = userRepository.getFriendsFlow(searchTerm)

    fun getSnailTrailFlow(userId: String): Flow<List<SnailTrailPoint>> = userRepository.getSnailTrailFlow(userId)


    /** Chats with unread messages, for the chat tab's nav bar badge. */
    fun getUnreadChatCountFlow(): Flow<Int> = messageLocalDataSource.getUnreadChatCountFlow()

    /**
     * Every chat of the logged in user, unfiltered and sorted, for the chat selector.
     *
     * Counters and last-message previews come straight out of SQL ([MessageLocalDataSource.getChatAggregatesFlow],
     * [MessageLocalDataSource.getLastMessagePerChatFlow]) - one row per chat instead of the whole
     * message table, so nothing here scales with the number of messages.
     *
     * Search and filter are deliberately not part of this flow: they run on the emitted list via
     * [applySearchAndFilter], so typing never re-subscribes to the database.
     */
    fun getChatListFlow(userId: String): Flow<List<ChatListItem>> {
        val aggregatesFlow = messageLocalDataSource.getChatAggregatesFlow(userId)
        val lastMessagesFlow = messageLocalDataSource.getLastMessagePerChatFlow(userId)
        val usersFlow = userLocalDataSource.getAllUsersFlow()
        val groupsFlow = groupLocalDataSource.getAllGroupswithMembersFlow()
        val pinnedFlow = preferencemanager.getPinnedChatsFlow()

        return combine(
            aggregatesFlow,
            lastMessagesFlow,
            usersFlow,
            groupsFlow,
            pinnedFlow
        ) { aggregates, lastMessages, users, groups, pinnedList ->

            val userIdMap = users.associateBy { it.id }
            val pinnedMap = pinnedList.associate { it.chatId to it.pinTimePoint }
            val aggregateMap = aggregates.associateBy { it.chatId to it.isGroup }
            val lastMessageMap = lastMessages.associateBy { it.chatKey(userId) }

            val userItems = users
                .asSequence()
                .filter { it.id != userId }
                .filter { it.friendshipStatus == FriendshipStatus.ACCEPTED }
                .map { user ->
                    val aggregate = aggregateMap[user.id to false]
                    val last = lastMessageMap[user.id to false]?.let { message ->
                        message.copy(
                            senderAsString = userIdMap[message.senderId]?.name ?: message.senderAsString
                        )
                    }

                    user.toChatListItem(
                        unreadCount = aggregate?.unreadCount ?: 0,
                        unsentCount = aggregate?.unsentCount ?: 0,
                        lastMessage = last,
                        pinned = pinnedMap[user.id] ?: 0L
                    )
                }
                .toList()

            val groupItems = groups
                .asSequence()
                .map { group ->
                    val aggregate = aggregateMap[group.id to true]
                    val last = lastMessageMap[group.id to true]?.let { message ->
                        message.copy(
                            senderAsString = userIdMap[message.senderId]?.name ?: "Unknown"
                        )
                    }

                    group.toChatListItem(
                        unreadCount = aggregate?.unreadCount ?: 0,
                        unsentCount = aggregate?.unsentCount ?: 0,
                        lastMessage = last,
                        pinned = pinnedMap[group.id] ?: 0L
                    )
                }
                .toList()

            (userItems + groupItems).sortedForChatSelector()

        }.flowOn(Dispatchers.Default)
    }

    /**
     * Get main screen available items as flow
     */
    fun getChatSelectorFlow(
        searchTerm: String,
        userId: String,
        filter: ChatFilter = ChatFilter.NONE
    ): Flow<List<ChatListItem>> {
        return getChatListFlow(userId).map { chats ->
            chats.applySearchAndFilter(searchTerm, filter)
        }
    }


    /**
     * Messages whose text matches [searchTerm], newest first, for the message section of the chat
     * selector search. Returns an empty flow for terms shorter than [MESSAGE_SEARCH_MIN_TERM_LENGTH]
     * so typing a single character never scans the whole message table.
     *
     * Matches message text (which doubles as the caption of image/audio messages) plus poll title,
     * description and vote options. Deleted messages are skipped - their content is a tombstone.
     */
    fun getMessageSearchFlow(
        searchTerm: String,
        userId: String
    ): Flow<List<MessageSearchResult>> {
        val loweredSearch = searchTerm.trim().lowercase()
        if (loweredSearch.length < MESSAGE_SEARCH_MIN_TERM_LENGTH) return flowOf(emptyList())

        val messagesFlow = messageLocalDataSource.getAllMessages()
        val usersFlow = userLocalDataSource.getAllUsersFlow()
        val groupsFlow = groupLocalDataSource.getAllGroupswithMembersFlow()

        return combine(messagesFlow, usersFlow, groupsFlow) { messages, users, groups ->
            val userIdMap = users.associateBy { it.id }
            val groupIdMap = groups.associateBy { it.id }

            messages
                .asSequence()
                .filter { it.matchesSearchTerm(loweredSearch) }
                .sortedByDescending { it.getSendDateAsLong() }
                .take(MESSAGE_SEARCH_RESULT_LIMIT)
                .mapNotNull { message ->
                    val messageId = message.id ?: return@mapNotNull null

                    //Resolve which chat this message belongs to: for groups that is always the
                    //receiver, for direct messages it is whichever side is not us.
                    val chatId: String
                    val chatName: String
                    val chatPicture: String
                    if (message.isGroupMessage()) {
                        chatId = message.receiverId
                        val group = groupIdMap[chatId] ?: return@mapNotNull null
                        chatName = group.name
                        chatPicture = group.profilePictureUrl
                    } else {
                        chatId = if (message.senderId == userId) message.receiverId else message.senderId
                        val partner = userIdMap[chatId] ?: return@mapNotNull null
                        chatName = partner.nickName?.takeIf { it.isNotBlank() } ?: partner.name
                        chatPicture = partner.profilePictureUrl
                    }

                    MessageSearchResult(
                        messageId = messageId,
                        chatId = chatId,
                        isGroup = message.isGroupMessage(),
                        chatName = chatName,
                        chatProfilePictureUrl = chatPicture,
                        senderName = userIdMap[message.senderId]?.name
                            ?: message.senderAsString,
                        preview = message.searchPreview(),
                        sendDate = message.getSendDateAsLong(),
                    )
                }
                .toList()
        }.flowOn(Dispatchers.Default)
    }

    /** True when any searchable text of this message contains the already lowercased [term]. */
    private fun Message.matchesSearchTerm(term: String): Boolean {
        if (deleted) return false
        if (content.lowercase().contains(term)) return true

        val poll = poll ?: return false
        return poll.title.lowercase().contains(term) ||
                poll.description?.lowercase()?.contains(term) == true ||
                poll.voteOptions.any { it.text.lowercase().contains(term) }
    }

    /** Text shown underneath a search result - the poll title stands in for polls without text. */
    private fun Message.searchPreview(): String =
        content.takeIf { it.isNotBlank() } ?: poll?.title.orEmpty()


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



    sealed class MessageContent {
        data class TextContent(val message: String) : MessageContent()
        data class ImageContent(val image: ByteArray, val text: String) : MessageContent()
        data class AudioContent(val audio: ByteArray) : MessageContent()

        data class PollContent(val poll: PollCreateRequest) : MessageContent()
    }


    val sendMessageLock = Mutex()

    /**
     * @param localpk Local pk, only pass if already in db
     * @param clientMessageId Send idempotency key of an already-queued message. REQUIRED when
     * [localpk] is non-zero (a resend) - reusing the row's original key is the entire mechanism
     * that lets the server recognize a retry; silently minting a new one here for a resend would
     * defeat it. Never pass this for a brand-new message - one is minted below.
     *
     */
    suspend fun sendMessage(ownId: String, messageId: String?, empfaenger: String, gruppe: Boolean, content: MessageContent, answerid: String?, localpk: Long = 0, clientMessageId: String? = null){

        sendMessageLock.withLock {
            var localpkintern = localpk

            val senddate = getCurrentTimeMillisString()
            val clientMessageIdIntern = if (localpkintern != 0L) { //Already upserted to db
                requireNotNull(clientMessageId) {
                    "Resending an already-queued message (localPK=$localpkintern) must reuse its stored clientMessageId"
                }
            } else {
                //Generate random uuid per message for server side checking, pass when sending.
                //Randomuuid because localpk will be reset on db drop, invisible despawned messages when multi device user
                Uuid.random().toString()
            }

            //Interne message macha die ned alles hot
            val messageDto = when(content) {
                is PollContent -> {
                    MessageDto(
                        localPK = localpkintern,
                        id = null,
                        msgType = MessageType.POLL,
                        content = "",
                        poll = content.poll.toLocalPollMessage(ownId),
                        senderId = ownId,
                        receiverId = empfaenger,
                        sendDate = senddate.toLong(),
                        updatedAt = senddate,
                        deleted = false,
                        groupMessage = gruppe,
                        answerId = answerid,
                        sent = false,
                        myMessage = true,
                        readByMe = true,
                        clientMessageId = clientMessageIdIntern
                    )
                }
                is TextContent -> {
                    MessageDto(
                        localPK = localpkintern,
                        id = messageId,
                        msgType = MessageType.TEXT,
                        content = content.message,
                        senderId = ownId,
                        receiverId = empfaenger,
                        sendDate = senddate.toLong(),
                        updatedAt = senddate,
                        deleted = false,
                        groupMessage = gruppe,
                        answerId = answerid,
                        sent = false,
                        myMessage = true,
                        readByMe = true,
                        clientMessageId = clientMessageIdIntern
                    )
                }

                is ImageContent -> {

                    MessageDto(
                        localPK = localpkintern,
                        id = null,
                        msgType = MessageType.IMAGE,
                        content = content.text,
                        senderId = ownId,
                        receiverId = empfaenger,
                        sendDate = senddate.toLong(),
                        updatedAt = senddate,
                        deleted = false,
                        groupMessage = gruppe,
                        answerId = answerid,
                        sent = false,
                        myMessage = true,
                        readByMe = true,
                        clientMessageId = clientMessageIdIntern
                    )
                }

                is AudioContent -> {

                    MessageDto(
                        localPK = localpkintern,
                        id = null,
                        msgType = MessageType.AUDIO,
                        //content = content.text,
                        senderId = ownId,
                        receiverId = empfaenger,
                        sendDate = senddate.toLong(),
                        updatedAt = senddate,
                        deleted = false,
                        groupMessage = gruppe,
                        answerId = answerid,
                        sent = false,
                        myMessage = true,
                        readByMe = true,
                        clientMessageId = clientMessageIdIntern
                    )
                }
            }


            //Nachricht hot scho a pk vo da db, also scho din
            if (localpkintern == 0L){
                localpkintern = database.messageDao().insertMessageDto(messageDto)
                println("LocalPK: $localpkintern")
            }


            val serverrequest = when (content) {
                is PollContent -> {
                    networkUtils.sendPollMessageToServer(
                        empfaenger = empfaenger,
                        gruppe = gruppe,
                        content = content.poll,
                        answerid = answerid,
                        clientMessageId = clientMessageIdIntern
                    )
                }
                is TextContent -> {
                    networkUtils.sendTextMessageToServer(
                        messageId = messageId,
                        empfaenger = empfaenger,
                        gruppe = gruppe,
                        content = content.message,
                        answerid = answerid,
                        clientMessageId = clientMessageIdIntern
                    )
                }

                is ImageContent -> {
                    networkUtils.sendImageMessageToServer(
                        empfaenger = empfaenger,
                        gruppe = gruppe,
                        image = content.image,
                        text = content.text,
                        answerid = answerid,
                        clientMessageId = clientMessageIdIntern
                    )
                }

                is AudioContent -> {
                    networkUtils.sendAudioMessageToServer(
                        empfaenger = empfaenger,
                        gruppe = gruppe,
                        audio = content.audio,
                        answerid = answerid,
                        clientMessageId = clientMessageIdIntern
                    )
                }
            }.trackConnectivity()

            when (serverrequest){
                is NetworkResult.Error<NetworkingError> -> {

                    val error = serverrequest.error
                    //If badrequest (not met servers specifications (should not happen)) or conflict (duplicate message) delete locally
                    if (error is NetworkingError.BadRequest || error is NetworkingError.Conflict) {
                        database.messageDao().deleteMessageDtoByPk(localpkintern)
                    }

                    println("Message senden error: ${serverrequest.error}")
                    errorChannel.sendErrorSuspend(ErrorEvent(error = error))

                    when (content) {
                        is ImageContent -> {
                            pictureManager.savePictureToStorage(content.image, "unsent_" + localpkintern + PICTURE_FILE_NAME)
                            //ignore unsent audio or image, this should not happen anyway
                        }

                        is AudioContent -> {
                            audioManager.saveAudioToStorage(
                                audioBytes = content.audio,
                                filename = "unsent_" + localpkintern + VOICEMSG_FILE_NAME
                            )
                        }

                        else -> {}
                    }

                }
                is NetworkResult.Success<MessageResponse> -> {
                    withContext(Dispatchers.IO) {

                        println("Messageid returned from server: ${serverrequest.data.messageId}")

                        val existing =
                            database.messageDao().getMessageDtoById(serverrequest.data.messageId)

                        messageLocalDataSource.upsertMessage(
                            Message(
                                localPK = localpkintern,
                                id = serverrequest.data.messageId,
                                msgType = serverrequest.data.msgType,

                                content = serverrequest.data.content,
                                poll = serverrequest.data.pollResponse?.toPollMessage(ownId),

                                senderId = serverrequest.data.senderId,
                                receiverId = serverrequest.data.receiverId,
                                sendDate = serverrequest.data.sendDate.toString(),
                                changeDate = serverrequest.data.lastChanged.toString(),
                                deleted = serverrequest.data.deleted,
                                groupMessage = serverrequest.data.groupMessage,
                                answerId = serverrequest.data.answerId,
                                sent = true,
                                myMessage = true,
                                readByMe = true,
                                pictureUrl = existing?.pictureUrl,
                                readers = serverrequest.data.readers.map {
                                    MessageReader(
                                        readerEntryId = 0L,
                                        messageId = serverrequest.data.messageId,
                                        readerId = it.userId,
                                        readDate = it.readAt.toString()
                                    )
                                },
                                // Bypasses /messages/sync - never let it advance the sync
                                // cursor (MAX(version) over the messages table).
                                version = existing?.version ?: 0L
                            )
                        )

                        if (serverrequest.data.msgType == MessageType.IMAGE) {
                            getPicturesForMessageIds(listOf(serverrequest.data.messageId))
                            pictureManager.deletePicture("unsent_" + localpkintern + PICTURE_FILE_NAME)
                        } else if (serverrequest.data.msgType == MessageType.AUDIO) {
                            getAudiosForMessageIds(listOf(serverrequest.data.messageId))

                            audioManager.deleteAudio("unsent_" + localpkintern + VOICEMSG_FILE_NAME)
                        }
                    }
                }
            }
        }

    }



    private val sendOfflineLock = Mutex()

    /**
     * Send offline messages. Locks this function, and calls the sendmessage for every message.
     * No async, because multiple messages could be sent at the same time.
     * Always wait for the send lock to be unlocked, that way there can not be an unsent message
     * be retrieved from the db as unsent while it is already sending.
     */
    suspend fun sendOfflineMessages(ownId: String){
        sendOfflineLock.withLock {

            while (true) {

                while (sendMessageLock.isLocked) {
                    //If the message sending lock is active, do not send offline messages
                    //println("OFFLINE MESSAGE SENDING: Sending in progress, waiting for lock")
                    delay(15.milliseconds)
                }


                val messages = messageLocalDataSource.getUnsentMessages()
                if (messages.isEmpty()) {
                    //println("OFFLINE MESSAGE SENDING: NO UNSENT MESSAGES")
                    return
                }

                val m = messages.first()

                //A poll row whose poll no longer decodes (the type converter returns null for it) can
                //never be sent - drop it, or it would block the whole queue forever
                if (m.msgType == MessageType.POLL && m.poll == null) {
                    database.messageDao().deleteMessageDtoByPk(m.localPK)
                    continue
                }

                sendMessage(
                    empfaenger = m.receiverId,
                    gruppe = m.groupMessage,
                    content = when (m.msgType) {
                        MessageType.TEXT -> {
                            TextContent(m.content)
                        }

                        MessageType.IMAGE -> {
                            val image =
                                pictureManager.loadPictureFromStorage("unsent_" + m.localPK + PICTURE_FILE_NAME)

                            if (image == null) {
                                TextContent("Offline image sending failed")
                            } else {
                                ImageContent(
                                    image = image,
                                    text = m.content
                                )
                            }
                        }

                        MessageType.POLL -> {
                            //Null-checked above. Rebuilt with the shared mapper so sub polls and
                            //every other setting survive the resend
                            PollContent(m.poll!!.toCreateRequest())
                        }

                        MessageType.AUDIO -> {
                            if(m.audioPath == null){
                                TextContent("Offline audio sending failed")
                            }
                            val audio = getAudioBytes(
                                audioManager.getRecordingPath("unsent_" + m.localPK + VOICEMSG_FILE_NAME)
                            )

                            AudioContent(
                                audio = audio
                            )
                        }

                        MessageType.SYSTEM -> error("SYSTEM messages are server-authored and are never queued locally as unsent")
                    },
                    answerid = m.answerId,
                    localpk = m.localPK,
                    messageId = null,
                    ownId = ownId,
                    clientMessageId = m.clientMessageId
                )


                val nextMessages = messageLocalDataSource.getUnsentMessages()
                if (nextMessages.firstOrNull()?.localPK == m.localPK) return
            }


        }
    }



    /**
     * Serialises [messageIdSync]'s paging loop. Two overlapping runs each read `since` from
     * MAX(version) before the other has written its page, so the slower one can upsert an
     * already-superseded copy of a message on top of the newer one - which is how a chat marked
     * read came back unread again. Media downloads stay outside the lock, they only add files.
     */
    private val messageSyncLock = Mutex()

    /**
     * Execute a message sync
     *
     * @param fetchMedia whether to immediately download images/audios for newly-synced
     * messages. Disabled during the full [dataSync] so that media downloads happen as part
     * of the MEDIA job (and its progress indicator) instead of inside the MESSAGES job.
     */
    suspend fun messageIdSync(fetchMedia: Boolean = true) {

        val ownId = SessionCache.requireLoggedIn()?.userId ?: return

        val imagesToGet = mutableListOf<String>()
        val audiosToGet = mutableListOf<String>()

        messageSyncLock.withLock {

            var since = messageLocalDataSource.getLastSyncedMessageVersion()
            var moreMessages = true

            while (moreMessages) {

                val messageSyncResponse = networkUtils.messageSync(since = since).trackConnectivity()

                when (messageSyncResponse) {
                    is NetworkResult.Error<*> -> {
                        loggingRepository.logWarning("messageid sync error: ${messageSyncResponse.error}")
                        break // Stop on error, never advance past a batch we failed to fetch
                    }

                    is NetworkResult.Success<MessageSyncResponse> -> {
                        val updatedMessages = messageSyncResponse.data.updatedMessages
                        val deletedMessages = messageSyncResponse.data.deletedMessages
                        moreMessages = messageSyncResponse.data.moreMessages

                        // Deletions can now arrive on any page, not just page 0. Apply them
                        // before the updates below so a crash mid-batch never leaves MAX(version)
                        // ahead of what was actually committed.
                        messageLocalDataSource.deleteMessages(deletedMessages)

                        // The whole page goes in as one batch: every separate write would invalidate
                        // the message table again and re-run the chat selector's query chain.
                        val sortedUpdates = updatedMessages.sortedBy { it.version }
                        val existingById = messageLocalDataSource
                            .getMessageDtosByIds(sortedUpdates.map { it.messageId })
                            .associateBy { it.id }

                        // A message of our own can arrive here before our own sendMessage() call for
                        // it has processed its response (dataSync runs fully in parallel with sends -
                        // nothing serializes them). Falling back to the pending row's clientMessageId
                        // (only ever populated on our own sends, and only ever echoed back to us by
                        // the server for our own messages) merges onto that row instead of creating a
                        // second, permanently-orphaned one.
                        val unmatchedClientMessageIds = sortedUpdates
                            .filter { existingById[it.messageId] == null }
                            .mapNotNull { it.clientMessageId }
                        val existingByClientMessageId = messageLocalDataSource
                            .getMessageDtosByClientMessageIds(unmatchedClientMessageIds)
                            .associateBy { it.clientMessageId }

                        messageLocalDataSource.upsertMessages(
                            sortedUpdates.map { messageResponse ->
                                val existing = existingById[messageResponse.messageId]
                                    ?: messageResponse.clientMessageId?.let { existingByClientMessageId[it] }
                                messageResponse.toDomainMessage(
                                    ownId = ownId,
                                    existingLocalPK = existing?.localPK ?: 0L,
                                    existingPictureUrl = existing?.pictureUrl,
                                    existingAudioPath = existing?.audioPath,
                                    version = messageResponse.version
                                )
                            }
                        )

                        sortedUpdates.forEach { messageResponse ->
                            when (messageResponse.msgType) {
                                MessageType.IMAGE -> imagesToGet += messageResponse.messageId
                                MessageType.AUDIO -> audiosToGet += messageResponse.messageId
                                else -> {}
                            }
                        }

                        // Always trust the envelope's watermark, never max(updatedMessages) - a
                        // page can be all deletions, in which case updatedMessages is empty but
                        // newVersion still advances.
                        since = messageSyncResponse.data.newVersion

                        //println("MessageIdSync finished, new messages: ${updatedMessages.size}")
                    }
                }
            }

            //println("Messagesync completed")

        }

        if (fetchMedia) {
            if (imagesToGet.isNotEmpty()) {
                println("Images to fetch: ${imagesToGet.size}")
                getPicturesForMessageIds(imagesToGet)
            }

            if (audiosToGet.isNotEmpty()) {
                println("Audios to fetch: ${audiosToGet.size}")
                getAudiosForMessageIds(audiosToGet)
            }
        }
    }

    /**
     * Instantly upserts a message delivered via push notification (FCM/APNs) into Room, so a
     * chat opened before the next sync already shows it. Never overwrites an existing row and
     * never advances the sync cursor (version stays 0, the same trick [messageIdSync] and the
     * socket handler rely on) - the caller is still expected to follow up with [messageIdSync],
     * which replaces this provisional row with the authoritative one (readers, reactions,
     * real version).
     */
    suspend fun applyPushMessage(decoded: DecodedNotification.Message) {
        if (SessionCache.requireLoggedIn()?.userId == null) return

        if (decoded.reaction) {
            applyPushReaction(decoded)
            return
        }

        // Only TEXT pushes carry enough content for a useful provisional row - IMAGE/AUDIO/POLL
        // wait for the real sync + media fetch instead of flashing an empty bubble.
        if (decoded.messageType != MessageType.TEXT) return

        // Never overwrite an already-synced or socket-delivered row.
        if (messageLocalDataSource.getMessageById(decoded.msgId) != null) return

        // Blank when the push came from a server that predates the plaintext `content` field.
        val content = decoded.content.ifBlank { return }

        val sendDate = (decoded.sendDate.takeIf { it > 0L } ?: getCurrentTimeMillisLong()).toString()

        messageLocalDataSource.upsertMessage(
            Message(
                id = decoded.msgId,
                msgType = MessageType.TEXT,
                content = content,
                senderId = decoded.senderId,
                // Group chat is identified by groupId, not the push's receiverId (the
                // recipient user) - see NotificationContentResolver.resolveMessage.
                receiverId = if (decoded.groupMessage) decoded.groupId else decoded.receiverId,
                groupMessage = decoded.groupMessage,
                answerId = decoded.answerId.ifBlank { null },
                sendDate = sendDate,
                changeDate = sendDate,
                sent = true,
                myMessage = false,
                readByMe = false,
                readers = emptyList(),
                reactions = emptyList(),
                version = 0L,
            )
        )
    }

    /** Applies a reaction push to an already-local message. Never creates a new row. */
    private suspend fun applyPushReaction(decoded: DecodedNotification.Message) {
        val existing = messageLocalDataSource.getMessageById(decoded.msgId) ?: return

        val emoji = decoded.content.ifBlank { return }

        messageLocalDataSource.upsertMessage(
            existing.copy(
                reactions = existing.reactions.filterNot { it.userId == decoded.senderId } +
                    Reaction(userId = decoded.senderId, content = emoji, reactedAt = getCurrentTimeMillisLong())
            )
        )
    }

    suspend fun getPicturesForMessageIds(ids: List<String>){
        ids.forEach { messageId ->
            val savefilename = messageId + PICTURE_FILE_NAME

            when (val picture = networkUtils.getImageForImageMessage(messageId).trackConnectivity()) {
                is NetworkResult.Error<*> -> {loggingRepository.logWarning("Picture error for messageid $messageId")}
                is NetworkResult.Success<ByteArray> -> {
                    val filepath = pictureManager.savePictureToStorage(picture.data, savefilename)

                    messageLocalDataSource.updatePictureUrl(messageId, filepath)
                }
            }
        }
    }

    suspend fun getAudiosForMessageIds(ids: List<String>){
        ids.forEach { messageId ->
            val savefilename = messageId + VOICEMSG_FILE_NAME
            //println("Fetching Audio for messageid $messageId")

            when (val audio = networkUtils.getAudioForAudioMessage(messageId).trackConnectivity()) {
                is NetworkResult.Error<*> -> {loggingRepository.logWarning("Audio error for messageid $messageId")}
                is NetworkResult.Success<ByteArray> -> {
                    val filepath = audioManager.saveAudioToStorage(audio.data, savefilename)
                    //println("Audio saved to $filepath")
                    // Persist the bare filename, not the absolute container path: on iOS the
                    // container UUID in an absolute path changes across reinstalls/updates and
                    // would go stale. The absolute path is re-derived at use-time.
                    messageLocalDataSource.updateAudioPath(messageId, savefilename)
                }
            }
        }
    }


    /**
     * Marks a whole chat read. The server is the single source of truth for read state: nothing
     * is written to Room here, the call only tells the server and then pulls the result back
     * through [messageIdSync], which rewrites `readByMe` from the authoritative `readers` list.
     *
     * Writing it locally first used to lose a race against a parallel sync - opening a chat from
     * a notification kicks off a [dataSync] at the same moment, and that sync re-fetched the
     * message before the server had processed the read, resetting `readByMe` to false again.
     *
     * The pull is not optional: the server deliberately excludes the acting user from its own
     * MessageChange socket fan-out, so this device never hears about its own read receipt.
     *
     * @return true when the server accepted the read. False (offline, error) means nothing
     * changed anywhere and the caller is expected to try again later.
     */
    suspend fun setAllChatMessagesRead(chatid: String, gruppe: Boolean, timestamp: String): Boolean {
        SessionCache.requireLoggedIn() ?: return false

        val result = networkUtils.setMessagesRead(chatid, gruppe, timestamp.toLong()).trackConnectivity()
        if (result !is NetworkResult.Success) {
            loggingRepository.logWarning("Setting chat $chatid read failed: ${(result as? NetworkResult.Error<*>)?.error}")
            return false
        }

        // With media: unlike the MESSAGES job in [dataSync] there is no MEDIA job behind this
        // call, so a picture or voice message first seen by this sync would otherwise stay
        // undownloaded until the next full sync gets around to the missing-media backfill.
        messageIdSync()
        return true
    }


    suspend fun editMessage(message: Message, newContent: String) {

        if (message.id == null) {
            //edit a not sent message
            val existing = messageLocalDataSource.getMessageById(message.localPK)
            if (existing != null) {
                messageLocalDataSource.upsertMessage(existing.copy(
                    content = newContent,
                    sent = false
                ))
            }

        } else {
            val request = networkUtils.editMessage(
                messageId = message.id!!,
                newContent = newContent
            ).trackConnectivity()

            when (request) {
                is NetworkResult.Error<NetworkingError> ->  {
                    val existing = messageLocalDataSource.getMessageById(message.localPK)
                    if (existing != null) {
                        messageLocalDataSource.upsertMessage(existing.copy(
                            content = newContent,
                            sent = false
                        ))
                    }
                    errorChannel.sendErrorSuspend(ErrorEvent(error = request.error))
                }
                is NetworkResult.Success<MessageResponse> -> {
                    val existing = messageLocalDataSource.getMessageById(request.data.messageId)
                    if (existing != null) {
                        messageLocalDataSource.upsertMessage(existing.copy(
                            content = request.data.content,
                            changeDate = request.data.lastChanged.toString(),
                            edited = true,
                        ))
                    }

                }
            }
        }

    }

    suspend fun deleteLocalMessage(localpk: Long) {
        messageLocalDataSource.deleteMessage(localpk)
    }

    suspend fun deleteMessage(messageId: String){
        val request = networkUtils.deleteMessage(
            messageId = messageId
        ).trackConnectivity()

        when (request) {
            is NetworkResult.Error<NetworkingError> ->  {
                errorChannel.sendErrorSuspend(ErrorEvent(error = request.error))
            }
            is NetworkResult.Success<*> -> {
                messageLocalDataSource.deleteMessage(messageId)
            }

        }
    }

    suspend fun reactToMessage(messageId: String, content: String) {
        val  request = networkUtils.reactToMessage(messageId = messageId, content = content).trackConnectivity()
        when (request) {
            is NetworkResult.Error -> errorChannel.sendErrorSuspend(ErrorEvent(error = request.error))
            is NetworkResult.Success -> {
                val existing = messageLocalDataSource.getMessageById(request.data.messageId)
                if (existing != null) {
                    messageLocalDataSource.upsertMessage(
                        existing.copy(
                            reactions = request.data.reactions.map {
                                Reaction(userId = it.userId, content = it.content)
                            },
                            changeDate = request.data.lastChanged.toString()
                        )
                    )
                }
            }
        }
    }



    suspend fun votePoll(ownId: String, pollVoteRequest: PollVoteRequest) {
        val request = networkUtils.votePoll(
            pollVoteRequest
        ).trackConnectivity()

        when (request) {
            is NetworkResult.Error<NetworkingError> ->  {
                errorChannel.sendErrorSuspend(ErrorEvent(error = request.error))
            }
            is NetworkResult.Success<MessageResponse> -> {
                val existing = messageLocalDataSource.getMessageById(request.data.messageId)
                //Skip a response older than what is stored - a socket push or a later vote's
                //response may have landed first, and must not be rolled back
                if (existing != null && !existing.isNewerThan(request.data.lastChanged)) {
                    messageLocalDataSource.upsertMessage(existing.copy(
                        poll = request.data.pollResponse?.toPollMessage(ownId),
                        changeDate = request.data.lastChanged.toString(),
                    ))
                }

            }
        }
    }

    suspend fun deletePollOption(ownId: String, messageId: String, optionId: String) {
        val request = networkUtils.deletePollOption(
            PollOptionDeleteRequest(messageId = messageId, optionId = optionId)
        ).trackConnectivity()

        when (request) {
            is NetworkResult.Error<NetworkingError> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = request.error))
            }
            is NetworkResult.Success<MessageResponse> -> {
                val existing = messageLocalDataSource.getMessageById(request.data.messageId)
                //Skip a response older than what is stored - a socket push or a later vote's
                //response may have landed first, and must not be rolled back
                if (existing != null && !existing.isNewerThan(request.data.lastChanged)) {
                    messageLocalDataSource.upsertMessage(existing.copy(
                        poll = request.data.pollResponse?.toPollMessage(ownId),
                        changeDate = request.data.lastChanged.toString(),
                    ))
                }
            }
        }
    }


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

    // Pinned chats follow the same push pattern as the settings in SettingsRepository: write the
    // local preference first, then fire the partial-update request and ignore its result.

    /** Pins or unpins a chat locally, then pushes the whole resulting list (server stores it wholesale). */
    suspend fun setPinnedChat(chatId: String, group: Boolean, pinned: Boolean) {
        if (pinned) {
            preferencemanager.addPinnedChat(PinnedChat(chatId, group, getCurrentTimeMillisString().toLong()))
        } else {
            preferencemanager.removePinnedChat(chatId)
        }
        networkUtils.updateSettings(UserSettingsRequest(pinnedChats = preferencemanager.getPinnedChats())).trackConnectivity()
    }

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

/**
 * Which chat a message belongs to: the group for group messages, the other participant for direct
 * ones. Mirrors the PARTITION BY of MessageDao.getLastMessagePerChatFlow so both sides key alike.
 */
private fun Message.chatKey(ownId: String): Pair<String, Boolean> = when {
    groupMessage -> receiverId to true
    senderId == ownId -> receiverId to false
    else -> senderId to false
}

/**
 * Chat selector order: pinned chats first (newest pin on top), then today's birthdays, then
 * everything else by last message. Birthdays are resolved once per item instead of inside the
 * comparator, which would re-parse the date and re-read the clock on every comparison.
 */
private fun List<ChatListItem>.sortedForChatSelector(): List<ChatListItem> =
    map { ChatSortKey(chat = it, birthdayToday = isBirthdayToday(it.birthDate)) }
        .sortedWith { a, b ->
            val aPinned = a.chat.pinned > 0L
            val bPinned = b.chat.pinned > 0L
            when {
                aPinned && bPinned -> b.chat.pinned.compareTo(a.chat.pinned)
                aPinned -> -1
                bPinned -> 1

                a.birthdayToday && !b.birthdayToday -> -1
                b.birthdayToday && !a.birthdayToday -> 1

                else -> {
                    val timeA = a.chat.lastMessage?.getSendDateAsLong() ?: 0L
                    val timeB = b.chat.lastMessage?.getSendDateAsLong() ?: 0L
                    timeB.compareTo(timeA)
                }
            }
        }
        .map { it.chat }

private class ChatSortKey(val chat: ChatListItem, val birthdayToday: Boolean)

/**
 * Search and filter an already built chat list. Runs in memory so typing in the chat selector never
 * re-subscribes to the database.
 */
fun List<ChatListItem>.applySearchAndFilter(
    searchTerm: String,
    filter: ChatFilter
): List<ChatListItem> {
    val loweredSearch = searchTerm.trim().lowercase()

    return asSequence()
        .filter {
            loweredSearch.isEmpty()
                    || it.name.lowercase().contains(loweredSearch)
                    || it.nickName?.lowercase()?.contains(loweredSearch) == true
        }
        .filter {
            when (filter) {
                ChatFilter.NONE -> true
                ChatFilter.UNREAD -> it.unreadMessageCount > 0
                ChatFilter.GROUPS -> it.isGroup
                ChatFilter.PERSONS -> !it.isGroup
            }
        }
        .toList()
}

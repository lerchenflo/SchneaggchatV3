@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.datasource

import androidx.compose.runtime.Composable
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.clearAuthTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.GITHUB_URL
import org.lerchenflo.schneaggchatv3mp.GROUPPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.PICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.VOICEMSG_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.di.HTTPCLIENTTYPE
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoteOption
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.toSelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.toUser
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatFilter
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository.ErrorChannel.sendErrorSuspend
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository.ErrorChannel.trySendError
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository.MessageContent.*
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.*
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.GroupMemberAction
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.MessageResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.TokenManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toPollMessage
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.RequestError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.errorCodeToMessage
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoRepository
import org.lerchenflo.schneaggchatv3mp.utilities.AudioManager
import org.lerchenflo.schneaggchatv3mp.utilities.ChangelogEntry
import org.lerchenflo.schneaggchatv3mp.utilities.ChangelogParser
import org.lerchenflo.schneaggchatv3mp.utilities.JwtUtils
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import org.lerchenflo.schneaggchatv3mp.utilities.getAudioBytes
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_access_expired
import schneaggchatv3mp.composeapp.generated.resources.error_invalid_credentials
import schneaggchatv3mp.composeapp.generated.resources.log_out_successfully
import kotlin.time.ExperimentalTime

class AppRepository(
    private val database: AppDatabase,
    private val networkUtils: NetworkUtils,
    private val tokenManager: TokenManager,
    private val preferencemanager: Preferencemanager,
    private val pictureManager: PictureManager,
    private val audioManager: AudioManager,

    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository,
    private val todoRepository: TodoRepository,

    val appVersion: AppVersion,
    private val loggingRepository: LoggingRepository,
) {
    //Errorchannel for global error events (Show in every screen)
    object ErrorChannel {

        data class ErrorEvent (
            val errorCode: Int? = null,
            val errorMessage: String? = null,
            val error: RequestError? = null,
            val errorMessageUiText: UiText? = null,
            val duration: Long = 5000L
        ){
            @Composable
            fun toStringComposable(): String {
                var finalstr = ""

                if (errorCode != null){
                    finalstr += "Errorcode: ${errorCode} ${errorCodeToMessage(errorCode)}\n"
                }

                if (error != null) {
                    finalstr += "Errorcode: ${error.errorCode} ${errorCodeToMessage(errorCode)}\n"

                    // Add error message from RequestError
                    if (error.message != null){
                        finalstr += "${error.message}\n"
                    }
                }

                // Add errormessage if provided
                if (errorMessage != null)
                    finalstr += "${errorMessage}\n"

                // Add UI text if provided
                if (errorMessageUiText != null)
                    finalstr += "${errorMessageUiText.asString()}\n"


                return finalstr
            }
        }

        private val _channel = Channel<ErrorEvent>(capacity = Channel.BUFFERED)
        val errors = _channel.receiveAsFlow()

        suspend fun sendErrorSuspend(event: ErrorEvent) {
            _channel.send(event) // suspending send
        }

        fun trySendError(event: ErrorEvent) {
            _channel.trySend(event).onFailure {
                println("Error when adding error event: $it")
            }
        }
    }


    object ActionChannel {

        sealed interface ActionEvent {
            data object Login : ActionEvent
            data object AuthInvalidated : ActionEvent
        }

        private val _channel = Channel<ActionEvent>(capacity = Channel.BUFFERED)
        val actions = _channel.receiveAsFlow()

        suspend fun sendActionSuspend(event: ActionEvent) {
            _channel.send(event) // suspending send
        }

        fun trySendAction(event: ActionEvent) {
            _channel.trySend(event).onFailure {
                println("Error when adding action event: $it")
            }
        }
    }


    /*
    **************************************************************************

    Common

    **************************************************************************
     */

    suspend fun setFirebaseToken(token: String) {
        if (token.isEmpty()) return
        println("Sending firebase token to server...")
        networkUtils.setFirebaseToken(token)
    }

    suspend fun sendEmailVerify(){
        networkUtils.sendEmailVerify()
    }


    /**
     * Delete all app data (for example on appdatadelete or logout)
     */
    suspend fun deleteAllAppData(){

        preferencemanager.saveLastStartedVersion("")

        database.allDatabaseDao().clearAll()
        NotificationManager.removeToken()
        SessionCache.logout()
    }

    suspend fun logout(){
        deleteAllAppData() // delete all app data when logging out

        //Clear access tokens

        preferencemanager.clearAll()
        KoinPlatform.getKoin().get<HttpClient>(qualifier = named(HTTPCLIENTTYPE.AUTHENTICATED)).clearAuthTokens()

        SessionCache.logout()
        NotificationManager.removeToken()
        SnackbarManager.showMessage(getString(Res.string.log_out_successfully))
    }

    /**
     * Test the endpoint
     */
    suspend fun testServer(serverUrl: String = BASE_SERVER_URL) : Boolean {
        println("Testing server: $serverUrl")
        return when(networkUtils.testServer(serverUrl)){
            is NetworkResult.Error<*> -> false
            is NetworkResult.Success<*> -> true
        }
    }


    val dataSyncLock = Mutex()

    suspend fun dataSync() {

        if (dataSyncLock.isLocked) {
            println("Datasync running, canceling")
            return
        }

        dataSyncLock.withLock {
            println("Starting datasync")
            
            coroutineScope {
                // Launch all sync operations concurrently
                val userJob = async {
                    try {
                        userIdSync()
                        println("User sync completed successfully")
                    } catch (e: Exception) {
                        loggingRepository.logWarning("User sync failed: ${e.message}")
                    }
                }

                val messageJob = async {
                    try {
                        messageIdSync()
                        println("Message sync completed successfully")
                    } catch (e: Exception) {
                        loggingRepository.logWarning("Message sync failed: ${e.message}")
                    }
                }

                val groupJob = async {
                    try {
                        groupIdSync()
                        println("Group sync completed successfully")
                    } catch (e: Exception) {
                        loggingRepository.logWarning("Group sync failed: ${e.message}")
                    }
                }

                // Wait for all to complete (errors are already handled)
                awaitAll(userJob, messageJob, groupJob)

                val errorProfilePicJob = async {
                    getMissingPics()
                    getMissingAudios()
                }
                awaitAll(errorProfilePicJob)

            }

            println("Data sync completed")
        }
    }

    /**
     * Get missing profile pics (On ios after update installation, there are no more pictures)
     */
    suspend fun getMissingPics() = coroutineScope {
        // Check user profile pics
        val users = userRepository.getAllUsers()
        val missingPpUserIds = users.filter { user ->
            !pictureManager.checkImageExists(user.profilePictureUrl)
        }.map { it.id }
        
        if (missingPpUserIds.isNotEmpty()) {
            launch { getProfilePicturesForUserIds(missingPpUserIds) }
        }

        // Check group profile pics
        val groups = groupRepository.getAllGroups()
        val missingPpGroupIds = groups.filter { group ->
            !pictureManager.checkImageExists(group.profilePictureUrl)
        }.map { it.id }
        
        if (missingPpGroupIds.isNotEmpty()) {
            launch { getProfilePicturesForGroupIds(missingPpGroupIds) }
        }

        // Check image messages
        val messages = messageRepository.getImageMessages()
        val missingImageMessageIds = messages.filter { image ->
            image.id != null && !pictureManager.checkImageExists(image.pictureUrl ?: "")
        }.map { it.id!! }
        
        if (missingImageMessageIds.isNotEmpty()) {
            launch { getPicturesForMessageIds(missingImageMessageIds) }
        }
    }

    suspend fun getMissingAudios() = coroutineScope {
        // Check audio messages
        val messages = messageRepository.getAudioMessages()
        val missingImageMessageIds = messages.filter { audio ->
            audio.id != null && !audioManager.checkAudioExists(audio.audioPath ?: "")
        }.map { it.id!! }

        if (missingImageMessageIds.isNotEmpty()) {
            launch { getAudiosForMessageIds(missingImageMessageIds) }
        }
    }


    suspend fun getChangeLog(version: String) : ChangelogEntry? {
        val networkResult = networkUtils.getChangeLog("$GITHUB_URL/main/README.md")
        return when (networkResult) {
            is NetworkResult.Error<*> -> {
                println("get changelog network error: ${networkResult.error.message}")
                null
            }
            is NetworkResult.Success<*> -> {
                val changelog = ChangelogParser.getParsedChangelog(networkResult.data.toString(), version)
                changelog
            }
        }
    }


    /*
    **************************************************************************

    Reusable flows

    **************************************************************************
     */
    /**
     * Get the currently logged in user as flow
     */
    fun getUserFlow(userId: String): Flow<User?> {
        return database.userDao().getUserbyIdFlow(userId).map { user ->
            user?.toUser()
        }
    }

    fun getPendingFriends(searchTerm: String): Flow<List<SelectedChat>> {
        return userRepository.getAllUsersFlow(searchTerm)
            .map { list ->
                list.filter {
                    it.friendshipStatus == FriendshipStatus.PENDING
                }
            }
    }


    suspend fun getFriends(searchTerm: String): List<User> {
        return userRepository.getAllUsers().filter {
            it.name.contains(searchTerm)
                    && it.friendshipStatus == FriendshipStatus.ACCEPTED
                    && !it.isGroup
        }
    }

    fun getFriendsFlow(searchTerm: String): Flow<List<User>> {
        return userRepository.getAllUsersFlow(searchTerm).map { users ->
            users.filter {
                it.friendshipStatus == FriendshipStatus.ACCEPTED
                        && !it.isGroup
            }
        }
    }


    /**
     * Get main screen available items as flow
     */
    fun getChatSelectorFlow(
        searchTerm: String,
        userId: String,
        filter: ChatFilter = ChatFilter.NONE
    ): Flow<List<SelectedChat>> {
        val messagesFlow = messageRepository.getAllMessages()
        val usersFlow = userRepository.getAllUsersFlow()
        val groupsFlow = groupRepository.getAllGroupswithMembersFlow()
        val pinnedFlow = preferencemanager.getPinnedChatsFlow()

        return combine(messagesFlow, usersFlow, groupsFlow, pinnedFlow) { messages, users, groups, pinnedList ->

            val loweredSearch = searchTerm.trim().lowercase()

            // PRE-PROCESS: Build message indexes for O(1) lookup
            val messagesByUser = mutableMapOf<String, MutableList<Message>>()
            val messagesByGroup = mutableMapOf<String, MutableList<Message>>()
            val userIdMap = users.associateBy { it.id }
            val pinnedMap = pinnedList.associate { it.chatId to it.pinTimePoint }

            // Single pass through messages to build indexes
            messages.forEach { msg ->
                if (msg.isGroupMessage()) {
                    messagesByGroup.getOrPut(msg.receiverId) { mutableListOf() }.add(msg)
                } else {
                    val senderId = msg.senderId
                    val receiverId = msg.receiverId

                    if (senderId != userId) {
                        messagesByUser.getOrPut(senderId) { mutableListOf() }.add(msg)
                    }
                    if (receiverId != userId) {
                        messagesByUser.getOrPut(receiverId) { mutableListOf() }.add(msg)
                    }
                }
            }

            // Process users - CREATE NEW IMMUTABLE OBJECTS
            val userItems = users
                .asSequence()
                .filter { it.id != userId }
                .filter { it.friendshipStatus == FriendshipStatus.ACCEPTED }
                .filter { loweredSearch.isEmpty() || it.name.lowercase().contains(loweredSearch) }
                .map { user ->
                    val userMessages = messagesByUser[user.id] ?: emptyList()

                    // Find last message
                    val last = userMessages.maxByOrNull { it.getSendDateAsLong() }?.apply {
                        this.senderAsString =
                            userIdMap[this.senderId]?.name ?: this.senderAsString
                    }

                    // Count in single pass - my messages are automatically read
                    var unreadCount = 0
                    var unsentCount = 0
                    userMessages.forEach { message ->
                        // Only count as unread if it's NOT my message and NOT read by me
                        if (!message.myMessage && !message.readByMe) {
                            unreadCount++
                        }
                        if (!message.sent) unsentCount++
                    }

                    user.toSelectedChat(
                        unreadCount = unreadCount,
                        unsentCount = unsentCount,
                        lastMessage = last,
                        pinned = pinnedMap[user.id] ?: 0L
                    )
                }
                .toList()

            // Process groups - CREATE NEW IMMUTABLE OBJECTS
            val groupItems = groups
                .asSequence()
                .filter { loweredSearch.isEmpty() || it.name.lowercase().contains(loweredSearch) }
                .map { gwm ->
                    val groupMessages = messagesByGroup[gwm.id] ?: emptyList()

                    val last = groupMessages.maxByOrNull { it.getSendDateAsLong() }?.apply {
                        this.senderAsString =
                            userIdMap[this.senderId]?.name ?: "Unknown"
                    }

                    // Count in single pass - my messages are automatically read
                    var unreadCount = 0
                    var unsentCount = 0
                    groupMessages.forEach { message ->
                        // Only count as unread if it's NOT my message and NOT read by me
                        if (!message.myMessage && !message.readByMe) {
                            unreadCount++
                        }
                        if (!message.sent) unsentCount++
                    }

                    gwm.toSelectedChat(
                        unreadCount = unreadCount,
                        unsentCount = unsentCount,
                        lastMessage = last,
                        pinned = pinnedMap[gwm.id] ?: 0L
                    )
                }
                .toList()

            // Apply filter and sort once
            val allItems = userItems + groupItems
            val filtered = when (filter) {
                ChatFilter.NONE -> allItems
                ChatFilter.UNREAD -> allItems.filter { it.unreadMessageCount > 0 }
                ChatFilter.GROUPS -> allItems.filter { it.isGroup }
                ChatFilter.PERSONS -> allItems.filter { !it.isGroup }
            }

            //filtered.sortedByDescending { it.lastmessage?.getSendDateAsLong() ?: 0L }
            filtered.sortedWith { a, b ->
                when {
                    // Case 1: Both are pinned -> Sort by pin timestamp (newest first)
                    a.pinned > 0L && b.pinned > 0L -> b.pinned.compareTo(a.pinned)

                    // Case 2: Only A is pinned -> A comes first
                    a.pinned > 0L -> -1

                    // Case 3: Only B is pinned -> B comes first
                    b.pinned > 0L -> 1

                    // Case 4: Neither is pinned -> Sort by last message date
                    else -> {
                        val timeA = a.lastmessage?.getSendDateAsLong() ?: 0L
                        val timeB = b.lastmessage?.getSendDateAsLong() ?: 0L
                        timeB.compareTo(timeA)
                    }
                }
            }

        }.flowOn(Dispatchers.Default)
    }


    /*
    **************************************************************************

    Auth

    **************************************************************************
     */


    /**
     * Suspend version of onNewTokenPair that persists tokens synchronously.
     * Use this from refresh flows where we must ensure tokens are written before returning.
     */
    suspend fun onNewTokenPair(tokenPair: TokenPair){
        loggingRepository.logDebug("Token save started: Processing new token pair")
        
        try {
            //Parse the token to get the user id
            loggingRepository.logDebug("Token save: Extracting user ID from refresh token")
            val userid = JwtUtils.getUserIdFromToken(tokenPair.refreshToken)
            loggingRepository.logInfo("Token save: User ID extracted: $userid")

            loggingRepository.logDebug("Token save: Saving tokens to secure storage")
            preferencemanager.saveTokens(tokenPair)
            
            loggingRepository.logDebug("Token save: Saving user ID to preferences")
            preferencemanager.saveOWNID(userid)

            loggingRepository.logDebug("Token save: Updating session cache")
            SessionCache.updateTokens(tokenPair)
            SessionCache.updateOnline(true)
            
            loggingRepository.logInfo("Token save completed successfully: Session cache updated")
        } catch (e: Exception) {
            loggingRepository.logError("Token save failed: ${e.message}")
            throw e // Re-throw to maintain existing error handling behavior
        }
    }



    /**
     * Function to load all initial data on app start
     */
    suspend fun loadSavedLoginConfig(): Boolean{
        val tokens = preferencemanager.getTokens()

        val tokensNotEmpty = tokens.accessToken.isNotEmpty() && tokens.refreshToken.isNotEmpty()
        val tokenDateValid =
            JwtUtils.isTokenDateValid(tokens.refreshToken) //is the refreshtoken still valid? If not, user needs to login again

        //Token is expired, send errormessage
        if (!tokenDateValid && tokensNotEmpty){
            trySendError(
                event = ErrorChannel.ErrorEvent(
                    401,
                    errorMessageUiText = UiText.StringResourceText(Res.string.error_access_expired),
                    duration = 5000L,
                )
            )
        }

        val credsSaved = tokenDateValid && tokensNotEmpty

        if (credsSaved){
            println("Tokens are saved in local storage, autologin permitted")

            SessionCache.login(
                tokens = tokens,
                developer = preferencemanager.getDevSettings()
            )
        }

        return credsSaved
    }


    suspend fun login(
        username: String,
        password: String,
        onResult: (Boolean) -> Unit
    ) {
        when(val result = networkUtils.login(username.trim(), password)){
            is NetworkResult.Error<*> -> {
                println("Error: ${result.error}")

                sendErrorSuspend(
                    ErrorChannel.ErrorEvent(
                        errorMessageUiText = UiText.StringResourceText(Res.string.error_invalid_credentials),
                        duration = 5000L
                    )
                )

                onResult(false)
            }
            is NetworkResult.Success<TokenPair> -> {
                withContext(NonCancellable) {
                    onNewTokenPair(result.data)
                }

                SessionCache.login(
                    tokens = result.data,
                    developer = preferencemanager.getDevSettings()
                )

                onResult(true)
            }
        }
    }


    suspend fun createAccount(
        username: String,
        email: String,
        password: String,
        birthdate: String,
        profilePic: ByteArray,
        onResult: (Boolean) -> Unit
    ) {
        when(val response = networkUtils.register(username, password, email, birthdate, profilePic)){
            is NetworkResult.Error -> {
                println("Error: ${response.error}")

                sendErrorSuspend(
                    ErrorChannel.ErrorEvent(
                        error = response.error,
                        duration = 5000L
                    )
                )

                onResult(false)
            }
            is NetworkResult.Success<*> -> {
                onResult(true)
            }
        }
    }


    /*
    **************************************************************************

    User

    **************************************************************************
     */

    /**
     * Execute a useridsync
     */
    suspend fun userIdSync() {
        val localusers = userRepository.getuserchangeid()

        val userSyncResponse = networkUtils.userIdSync(localusers.map { (id, changedate) -> IdTimeStamp(id, changedate) })

        val profilePicsToGet = emptyList<String>().toMutableList()

        when (userSyncResponse) {
            is NetworkResult.Error<*> -> {
                println("userid sync error: ${userSyncResponse.error}")
            }
            is NetworkResult.Success<UserSyncResponse> -> {
                //println("Userid sync response: ${userSyncResponse.data.toString()}")

                val updatedUsers = userSyncResponse.data.updatedUsers
                val deletedUsers = userSyncResponse.data.deletedUsers

                updatedUsers.forEach { newUser ->
                    when (newUser) {
                        is UserResponse.FriendUserResponse -> {
                            val existing = database.userDao().getUserbyId(newUser.id)
                            database.userDao().upsert(UserDto(
                                id = newUser.id,
                                updatedAt = newUser.updatedAt,
                                name = newUser.username,
                                nickName = newUser.nickName,
                                description = newUser.userDescription,
                                status = newUser.userStatus,
                                birthDate = newUser.birthDate,
                                frienshipStatus = FriendshipStatus.ACCEPTED,
                                requesterId = newUser.requesterId,

                                // Preserve existing values:
                                locationLat = existing?.locationLat,
                                locationLong = existing?.locationLong,
                                locationDate = existing?.locationDate,
                                locationShared = existing?.locationShared ?: false,
                                wakeupEnabled = existing?.wakeupEnabled ?: false,
                                lastOnline = existing?.lastOnline,
                                notisMuted = existing?.notisMuted ?: false,
                                email = null,
                                emailVerifiedAt = null,
                                createdAt = null,
                                profilePictureUrl = "",
                                profilePicUpdatedAt = newUser.profilePicUpdatedAt,
                            ))
                            if (existing == null || existing.profilePicUpdatedAt < newUser.profilePicUpdatedAt) {
                                profilePicsToGet += newUser.id
                            }
                        }
                        is UserResponse.SelfUserResponse -> {
                            val existing = database.userDao().getUserbyId(newUser.id)
                            database.userDao().upsert(UserDto(
                                id = newUser.id,
                                updatedAt = newUser.updatedAt,
                                name = newUser.username,
                                description = newUser.userDescription,
                                status = newUser.userStatus,
                                birthDate = newUser.birthDate,

                                // Preserve existing values:
                                locationLat = existing?.locationLat,
                                locationLong = existing?.locationLong,
                                locationDate = existing?.locationDate,
                                locationShared = existing?.locationShared ?: false,
                                wakeupEnabled = existing?.wakeupEnabled ?: false,
                                lastOnline = existing?.lastOnline,
                                frienshipStatus = null,
                                requesterId = null,
                                notisMuted = false,
                                email = newUser.email,
                                emailVerifiedAt = newUser.emailVerifiedAt,
                                createdAt = newUser.createdAt,
                                profilePictureUrl = "",
                                profilePicUpdatedAt = newUser.profilePicUpdatedAt,
                                ))
                            if (existing == null || existing.profilePicUpdatedAt < newUser.profilePicUpdatedAt) {
                                profilePicsToGet += newUser.id
                            }                        }
                        is UserResponse.SimpleUserResponse -> {
                            val existing = database.userDao().getUserbyId(newUser.id)
                            database.userDao().upsert(UserDto(
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
                                lastOnline = null,
                                frienshipStatus = newUser.friendShipStatus,
                                requesterId = newUser.requesterId,
                                notisMuted = false,
                                birthDate = null,
                                email = null,
                                emailVerifiedAt = null,
                                createdAt = null,
                                profilePictureUrl = "",
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

    /**
     * Get the profile pics for all passed user ids from the server
     */
    suspend fun getProfilePicturesForUserIds(userIds: List<String>){
        userIds.forEach { userId ->
            val savefilename = userId + USERPROFILEPICTURE_FILE_NAME
            when (val picture = networkUtils.getProfilePicForUserId(userId)) {
                is NetworkResult.Error<*> -> {println("Profilepic error for userid $userId")}
                is NetworkResult.Success<ByteArray> -> {
                    val filepath = pictureManager.savePictureToStorage(picture.data, savefilename)

                    userRepository.updateUserProfilePicUrl(userId, filepath)
                }
            }
        }
    }


    /**
     * Get all availavble users from the server
     * @param searchTerm the searchterm which the user entered
     */
    suspend fun getAvailableUsers(searchTerm: String) : List<NewFriendsUserResponse> {
        return when (val response = networkUtils.getAvailableUsers(searchTerm)) {
            is NetworkResult.Error<RequestError> -> {
                sendErrorSuspend(
                    ErrorChannel.ErrorEvent(
                        error = response.error
                    )
                )
                emptyList()
            }
            is NetworkResult.Success<List<NewFriendsUserResponse>> -> {
                response.data
            }
        }

    }

    /**
     * Send or accept friend request from / to an userid
     */
    suspend fun sendFriendRequest(friendId: String) : Boolean {
        when (val success = networkUtils.sendFriendRequest(friendId)){
            is NetworkResult.Error<*> -> return false
            is NetworkResult.Success<*> -> {
                dataSync()
                return true
            }
        }
    }

    /**
     * deny a friendrequest from an userid / cancel an outgoing friend request to an userid
     */
    suspend fun denyFriendRequest(friendId: String) : Boolean {
        when (val success = networkUtils.denyFriendRequest(friendId)){
            is NetworkResult.Error<*> -> return false
            is NetworkResult.Success<*> -> {
                dataSync()
                return true
            }
        }
    }


    suspend fun removeFriend(friendId: String) : Boolean {
        when (val success = networkUtils.removeFriend(friendId)){
            is NetworkResult.Error<*> -> return false
            is NetworkResult.Success<*> -> {
                dataSync()
                return true
            }
        }
    }


    suspend fun changeUsername(newUsername: String) : Boolean {
        when (val success = networkUtils.changeUsername(newUsername)){
            is NetworkResult.Error<*> -> return false
            is NetworkResult.Success<*> -> {
                dataSync()
                return true
            }
        }
    }


    suspend fun changePassword(oldPassword: String, newPassword: String) : Boolean {
        when (val success = networkUtils.changePassword(oldPassword, newPassword)){
            is NetworkResult.Error<*> -> return false
            is NetworkResult.Success<*> -> {
                return true
            }
        }
    }


    suspend fun changeProfilePic(newPic: ByteArray) : Boolean {
        when (val success = networkUtils.changeProfilePic(newPic)){
            is NetworkResult.Error<*> -> return false
            is NetworkResult.Success<*> -> {
                dataSync()
                return true
            }
        }
    }


    /**
     * Change status and description. if userid = self then only status, else only description
     */
    suspend fun changeUserDetails(
        userId: String,
        newStatus: String? = null,
        newDescription: String? = null,
        newEmail: String? = null,
        newBirthDate: String? = null,
        newNickName: String? = null,
        ) : Boolean {
        when (val success = networkUtils.changeProfile(
            userId = userId,
            newStatus = newStatus,
            newDescription = newDescription,
            newEmail = newEmail,
            newBirthDate = newBirthDate,
            newNickName = newNickName,
        )){
            is NetworkResult.Error<*> -> return false
            is NetworkResult.Success<*> -> {
                dataSync()
                return true
            }
        }
    }



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
     *
     */
    suspend fun sendMessage(ownId: String, messageId: String?, empfaenger: String, gruppe: Boolean, content: MessageContent, answerid: String?, localpk: Long = 0){

        sendMessageLock.withLock {
            var localpkintern = localpk

            val senddate = getCurrentTimeMillisString()

            //Interne message macha die ned alles hot
            val messageDto = when(content) {
                is PollContent -> {
                    MessageDto(
                        localPK = localpkintern,
                        id = null,
                        msgType = MessageType.POLL,
                        content = "",
                        poll = PollMessage(
                            creatorId = ownId,
                            title = content.poll.title,
                            description = content.poll.description,
                            maxAnswers = content.poll.maxAnswers,
                            customAnswersEnabled = content.poll.customAnswersEnabled,
                            maxAllowedCustomAnswers = content.poll.maxAllowedCustomAnswers,
                            visibility = content.poll.visibility,
                            expiresAt = content.poll.closeDate,
                            voteOptions = content.poll.voteOptions.mapIndexed { index, request ->
                                PollVoteOption(
                                    id = index.toString(),
                                    text = request.text,
                                    custom = false,
                                    creatorId = ownId,
                                    voters = emptyList()
                                )
                            }
                        ),
                        senderId = ownId,
                        receiverId = empfaenger,
                        sendDate = senddate,
                        updatedAt = senddate,
                        deleted = false,
                        groupMessage = gruppe,
                        answerId = answerid,
                        sent = false,
                        myMessage = true,
                        readByMe = true
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
                        sendDate = senddate,
                        updatedAt = senddate,
                        deleted = false,
                        groupMessage = gruppe,
                        answerId = answerid,
                        sent = false,
                        myMessage = true,
                        readByMe = true
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
                        sendDate = senddate,
                        updatedAt = senddate,
                        deleted = false,
                        groupMessage = gruppe,
                        answerId = answerid,
                        sent = false,
                        myMessage = true,
                        readByMe = true
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
                        sendDate = senddate,
                        updatedAt = senddate,
                        deleted = false,
                        groupMessage = gruppe,
                        answerId = answerid,
                        sent = false,
                        myMessage = true,
                        readByMe = true
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
                        answerid = answerid
                    )
                }
                is TextContent -> {
                    networkUtils.sendTextMessageToServer(
                        messageId = messageId,
                        empfaenger = empfaenger,
                        gruppe = gruppe,
                        content = content.message,
                        answerid = answerid
                    )
                }

                is ImageContent -> {
                    networkUtils.sendImageMessageToServer(
                        empfaenger = empfaenger,
                        gruppe = gruppe,
                        image = content.image,
                        text = content.text,
                        answerid = answerid
                    )
                }

                is AudioContent -> {
                    networkUtils.sendAudioMessageToServer(
                        empfaenger = empfaenger,
                        gruppe = gruppe,
                        audio = content.audio,
                        answerid = answerid
                    )
                }
            }

            when (serverrequest){
                is NetworkResult.Error<*> -> {
                    println("Message senden error: ${serverrequest.error}")

                    when (content) {
                        is ImageContent -> {
                            pictureManager.savePictureToStorage(content.image, "unsent_" + localpkintern + PICTURE_FILE_NAME)

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

                        messageRepository.upsertMessage(
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
                                }
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
                    delay(5)
                }


                val messages = messageRepository.getUnsentMessages()
                if (messages.isEmpty()) {
                    //println("OFFLINE MESSAGE SENDING: NO UNSENT MESSAGES")
                    return
                }

                val m = messages.first()

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

                            val poll = m.poll!!
                            PollContent(
                                PollCreateRequest(
                                    title = poll.title,
                                    description = poll.description,
                                    maxAnswers = poll.maxAnswers,
                                    customAnswersEnabled = poll.customAnswersEnabled,
                                    maxAllowedCustomAnswers = poll.maxAllowedCustomAnswers,
                                    visibility = poll.visibility,
                                    closeDate = poll.expiresAt,
                                    voteOptions = poll.voteOptions.map {
                                        PollVoteOptionCreateRequest(
                                            text = it.text
                                        )
                                    }
                                )
                            )
                        }

                        MessageType.AUDIO -> {
                            if(m.audioPath == null){
                                TextContent("Offline audio sending failed")
                            }
                            val audio = getAudioBytes("unsent_" + m.localPK + VOICEMSG_FILE_NAME)

                            AudioContent(
                                audio = audio
                            )
                        }
                    },
                    answerid = m.answerId,
                    localpk = m.localPK,
                    messageId = null,
                    ownId = ownId
                )


                val nextMessages = messageRepository.getUnsentMessages()
                if (nextMessages.firstOrNull()?.localPK == m.localPK) return
            }


        }
    }



    /**
     * Execute a message sync
     */
    suspend fun messageIdSync() {

        val ownId = SessionCache.requireLoggedIn()?.userId ?: return

        val localmessages = messageRepository.getmessagechangeid()
        val imagesToGet = mutableListOf<String>()
        val audiosToGet = mutableListOf<String>()

        var currentPage = 0
        var moreMessages = true

        while (moreMessages) {

            val messageSyncResponse = networkUtils.messageSync(
                messageIds = localmessages.map { IdTimeStamp(it.id, it.updatedAt) },
                page = currentPage
            )


            when (messageSyncResponse) {
                is NetworkResult.Error<*> -> {
                    println("messageid sync error")
                    break // Stop on error
                }

                is NetworkResult.Success<MessageSyncResponse> -> {
                    val updatedMessages = messageSyncResponse.data.updatedMessages
                    val deletedMessages = messageSyncResponse.data.deletedMessages
                    moreMessages = messageSyncResponse.data.moreMessages

                    updatedMessages.forEach { messageResponse ->
                        when (messageResponse.msgType) {
                            MessageType.IMAGE -> {
                                val existing = database.messageDao().getMessageDtoById(messageResponse.messageId)
                                messageRepository.upsertMessage(
                                    Message(
                                        localPK = existing?.localPK ?: 0L,
                                        id = messageResponse.messageId,
                                        msgType = messageResponse.msgType,
                                        content = messageResponse.content,
                                        pictureUrl = existing?.pictureUrl,
                                        senderId = messageResponse.senderId,
                                        receiverId = messageResponse.receiverId,
                                        sendDate = messageResponse.sendDate.toString(),
                                        changeDate = messageResponse.lastChanged.toString(),
                                        deleted = messageResponse.deleted,
                                        groupMessage = messageResponse.groupMessage,
                                        answerId = messageResponse.answerId,
                                        sent = true,
                                        myMessage = messageResponse.senderId == ownId,
                                        readByMe = messageResponse.readers.any { it.userId == ownId },
                                        senderAsString = "",
                                        senderColor = 0,
                                        readers = messageResponse.readers.map {
                                            MessageReader(
                                                readerEntryId = 0L,
                                                messageId = messageResponse.messageId,
                                                readerId = it.userId,
                                                readDate = it.readAt.toString()
                                            )
                                        }
                                    )
                                )
                                imagesToGet += messageResponse.messageId
                            }

                            MessageType.AUDIO -> {
                                val existing = database.messageDao().getMessageDtoById(messageResponse.messageId)
                                messageRepository.upsertMessage(
                                    Message(
                                        localPK = existing?.localPK ?: 0L,
                                        id = messageResponse.messageId,
                                        msgType = messageResponse.msgType,
                                        content = messageResponse.content,
                                        audioPath = existing?.audioPath,
                                        senderId = messageResponse.senderId,
                                        receiverId = messageResponse.receiverId,
                                        sendDate = messageResponse.sendDate.toString(),
                                        changeDate = messageResponse.lastChanged.toString(),
                                        deleted = messageResponse.deleted,
                                        groupMessage = messageResponse.groupMessage,
                                        answerId = messageResponse.answerId,
                                        sent = true,
                                        myMessage = messageResponse.senderId == ownId,
                                        readByMe = messageResponse.readers.any { it.userId == ownId },
                                        senderAsString = "",
                                        senderColor = 0,
                                        readers = messageResponse.readers.map {
                                            MessageReader(
                                                readerEntryId = 0L,
                                                messageId = messageResponse.messageId,
                                                readerId = it.userId,
                                                readDate = it.readAt.toString()
                                            )
                                        }
                                    )
                                )
                                audiosToGet += messageResponse.messageId
                            }

                            MessageType.TEXT -> {
                                val existing = database.messageDao().getMessageDtoById(messageResponse.messageId)
                                messageRepository.upsertMessage(
                                    Message(
                                        localPK = existing?.localPK ?: 0L,
                                        id = messageResponse.messageId,
                                        msgType = messageResponse.msgType,
                                        content = messageResponse.content,
                                        senderId = messageResponse.senderId,
                                        receiverId = messageResponse.receiverId,
                                        sendDate = messageResponse.sendDate.toString(),
                                        changeDate = messageResponse.lastChanged.toString(),
                                        deleted = messageResponse.deleted,
                                        groupMessage = messageResponse.groupMessage,
                                        answerId = messageResponse.answerId,
                                        sent = true,
                                        myMessage = messageResponse.senderId == ownId,
                                        readByMe = messageResponse.readers.any { it.userId == ownId },
                                        senderAsString = "",
                                        senderColor = 0,
                                        readers = messageResponse.readers.map {
                                            MessageReader(
                                                readerEntryId = 0L,
                                                messageId = messageResponse.messageId,
                                                readerId = it.userId,
                                                readDate = it.readAt.toString()
                                            )
                                        }
                                    )
                                )
                            }

                            MessageType.POLL -> {
                                val existing = database.messageDao().getMessageDtoById(messageResponse.messageId)

                                val savedMessage = Message(
                                    localPK = existing?.localPK ?: 0L,
                                    id = messageResponse.messageId,
                                    msgType = messageResponse.msgType,
                                    content = messageResponse.content,
                                    poll = messageResponse.pollResponse?.toPollMessage(ownId),
                                    senderId = messageResponse.senderId,
                                    receiverId = messageResponse.receiverId,
                                    sendDate = messageResponse.sendDate.toString(),
                                    changeDate = messageResponse.lastChanged.toString(),
                                    deleted = messageResponse.deleted,
                                    groupMessage = messageResponse.groupMessage,
                                    answerId = messageResponse.answerId,
                                    sent = true,
                                    myMessage = messageResponse.senderId == ownId,
                                    readByMe = messageResponse.readers.any { it.userId == ownId },
                                    senderAsString = "",
                                    senderColor = 0,
                                    readers = messageResponse.readers.map {
                                        MessageReader(
                                            readerEntryId = 0L,
                                            messageId = messageResponse.messageId,
                                            readerId = it.userId,
                                            readDate = it.readAt.toString()
                                        )
                                    }
                                )
                                messageRepository.upsertMessage(
                                    savedMessage
                                )
                            }
                        }
                    }

                    // Delete all non-existing messages
                    deletedMessages.forEach { id ->
                        messageRepository.deleteMessage(id)
                    }

                    println("MessageIdSync finished, new messages: ${updatedMessages.size}")
                }
            }

            currentPage++
        }

        //println("Messagesync completed. Total pages: $currentPage")


        if (imagesToGet.isNotEmpty()) {
            println("Images to fetch: ${imagesToGet.size}")
            getPicturesForMessageIds(imagesToGet)
        }

        if (audiosToGet.isNotEmpty()) {
            println("Audios to fetch: ${audiosToGet.size}")
            getAudiosForMessageIds(audiosToGet)
        }
    }

    suspend fun getPicturesForMessageIds(ids: List<String>){
        ids.forEach { messageId ->
            val savefilename = messageId + PICTURE_FILE_NAME

            when (val picture = networkUtils.getImageForImageMessage(messageId)) {
                is NetworkResult.Error<*> -> {println("Picture error for messageid $messageId")}
                is NetworkResult.Success<ByteArray> -> {
                    val filepath = pictureManager.savePictureToStorage(picture.data, savefilename)

                    messageRepository.updatePictureUrl(messageId, filepath)
                }
            }
        }
    }

    suspend fun getAudiosForMessageIds(ids: List<String>){
        ids.forEach { messageId ->
            val savefilename = messageId + VOICEMSG_FILE_NAME
            println("Fetching Audio for messageid $messageId")

            when (val audio = networkUtils.getAudioForAudioMessage(messageId)) {
                is NetworkResult.Error<*> -> {println("Audio error for messageid $messageId")}
                is NetworkResult.Success<ByteArray> -> {
                    val filepath = audioManager.saveAudioToStorage(audio.data, savefilename)
                    println("Audio saved to $filepath")
                    messageRepository.updateAudioPath(messageId, filepath)
                }
            }
        }
    }


    suspend fun setAllChatMessagesRead(ownId: String, chatid: String, gruppe: Boolean, timestamp: String){
        messageRepository.setAllChatMessagesRead(
            ownId = ownId,
            chatid = chatid,
            gruppe = gruppe,
            timestamp = timestamp
        )

        networkUtils.setMessagesRead(chatid, gruppe, timestamp.toLong())
    }


    suspend fun editMessage(message: Message, newContent: String) {

        if (message.id == null) {
            //edit a not sent message
            val existing = messageRepository.getMessageById(message.localPK)
            if (existing != null) {
                messageRepository.upsertMessage(existing.copy(
                    content = newContent,
                    sent = false
                ))
            }

        } else {
            val request = networkUtils.editMessage(
                messageId = message.id!!,
                newContent = newContent
            )

            when (request) {
                is NetworkResult.Error<RequestError> ->  {
                    val existing = messageRepository.getMessageById(message.localPK)
                    if (existing != null) {
                        messageRepository.upsertMessage(existing.copy(
                            content = newContent,
                            sent = false
                        ))
                    }
                    //sendErrorSuspend(ErrorEvent(error = request.error))
                }
                is NetworkResult.Success<MessageResponse> -> {
                    val existing = messageRepository.getMessageById(request.data.messageId)
                    if (existing != null) {
                        messageRepository.upsertMessage(existing.copy(
                            content = request.data.content,
                            changeDate = request.data.lastChanged.toString(),
                        ))
                    }

                }
            }
        }

    }

    suspend fun deleteLocalMessage(localpk: Long) {
        messageRepository.deleteMessage(localpk)
    }

    suspend fun deleteMessage(messageId: String){
        val request = networkUtils.deleteMessage(
            messageId = messageId
        )

        when (request) {
            is NetworkResult.Error<RequestError> ->  {
                sendErrorSuspend(ErrorChannel.ErrorEvent(error = request.error))
            }
            is NetworkResult.Success<*> -> {
                messageRepository.deleteMessage(messageId)
            }

        }
    }



    suspend fun votePoll(ownId: String, pollVoteRequest: PollVoteRequest) {
        val request = networkUtils.votePoll(
            pollVoteRequest
        )

        when (request) {
            is NetworkResult.Error<RequestError> ->  {
                sendErrorSuspend(ErrorChannel.ErrorEvent(error = request.error))
            }
            is NetworkResult.Success<MessageResponse> -> {
                val existing = messageRepository.getMessageById(request.data.messageId)
                if (existing != null) {
                    messageRepository.upsertMessage(existing.copy(
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



    /**
     * Create a group
     * @return the id of the created group to directly open the chat
     */
    suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>,
        profilePic: ByteArray
    ) : String? {
        val response = networkUtils.createGroup(
            name = name,
            description = description,
            memberIds = memberIds,
            profilePicBytes = profilePic
        )

        return when (response){
            is NetworkResult.Error<*> -> {
                sendErrorSuspend(ErrorChannel.ErrorEvent(error = response.error))
                null
            }
            is NetworkResult.Success<GroupResponse> -> {
                response.data.id
            }
        }
    }


    suspend fun groupIdSync() {

        val localgroups = groupRepository.getgroupchangeid()
        val profilepicsToGet = mutableListOf<String>()

        val groupSyncResponse = networkUtils.groupIdSync(
            groupIds = localgroups.map {
                IdTimeStamp(it.id, it.updatedAt)
            }
        )


        when (groupSyncResponse) {
            is NetworkResult.Error<*> -> {
                println("groupid sync error")
            }

            is NetworkResult.Success<GroupSyncResponse> -> {
                //println("GroupIdSync sync response: ${groupSyncResponse.data.toString()}")

                groupSyncResponse.data.updatedGroups.forEach { groupResponse ->
                    val existing = groupRepository.getGroupById(groupResponse.id)
                    groupRepository.upsertGroup(Group(
                        id = groupResponse.id,
                        name = groupResponse.name,
                        profilePictureUrl = "",
                        description = groupResponse.description,
                        createDate = groupResponse.createdAt,
                        updatedAt = groupResponse.updatedAt,
                        profilePicUpdatedAt = groupResponse.profilePicUpdatedAt,
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


                groupSyncResponse.data.deletedGroups.forEach { id ->
                    groupRepository.deleteGroup(id)
                }
            }
        }

        if (profilepicsToGet.isNotEmpty()) {
            getProfilePicturesForGroupIds(profilepicsToGet)
        }
    }

    suspend fun getProfilePicturesForGroupIds(groupIds: List<String>){
        groupIds.forEach { groupId ->
            val savefilename = groupId + GROUPPROFILEPICTURE_FILE_NAME
            when (val picture = networkUtils.getProfilePicForGroupId(groupId)) {
                is NetworkResult.Error<*> -> {println("Profilepic error for groupid $groupId")}
                is NetworkResult.Success<ByteArray> -> {
                    val filepath = pictureManager.savePictureToStorage(picture.data, savefilename)

                    groupRepository.updateGroupProfilePicUrl(groupId, filepath)
                }
            }
        }
    }


    suspend fun changeGroupProfilePic(groupId: String, newPic: ByteArray) : Boolean {
        when (val success = networkUtils.changeGroupProfilePic(newPic, groupId)){
            is NetworkResult.Error<*> -> return false
            is NetworkResult.Success<*> -> {
                dataSync()
                return true
            }
        }
    }

    suspend fun changeGroupDescription(groupId: String, newDescription: String) : Boolean {
        when (val success = networkUtils.changeGroupDescription(newDescription, groupId)){
            is NetworkResult.Error<*> -> return false
            is NetworkResult.Success<*> -> {
                dataSync()
                return true
            }
        }
    }

    suspend fun changeGroupName(groupId: String, newName: String) : Boolean {
        when (val success = networkUtils.changeGroupName(newName, groupId)){
            is NetworkResult.Error<*> -> return false
            is NetworkResult.Success<*> -> {
                dataSync()
                return true
            }
        }
    }

    suspend fun changeGroupMembers(action: GroupMemberAction, memberId: String, groupId: String) : Boolean {
        when (val success = networkUtils.changeGroupMembers(
            action = action,
            memberId = memberId,
            groupId = groupId
        )){
            is NetworkResult.Error<*> -> return false
            is NetworkResult.Success<*> -> {
                dataSync()
                return true
            }
        }
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

                    val todoSync = async {
                        try {
                            networkUtils.executeTodoIDSync(todoRepository = todoRepository)
                        } catch (e: Exception) {
                            loggingRepository.logWarning("Delayed action failed: ${e.message}")
                        }
                    }


                    // wait for all to finish; exceptions were handled in each async block so awaitAll won't throw
                    awaitAll(msgSync, userSync, groupSync, todoSync)
                }
            } finally {
                onLoadingStateChange(false)
            }
        }
    }

     */


}

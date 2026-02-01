@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.datasource

import androidx.compose.runtime.Composable
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.clearAuthTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.core.parameter.ParametersHolder
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.GROUPPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.toSelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.toUser
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatFilter
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.GroupMemberAction
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.MessageResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.TokenManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.RequestError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.errorCodeToMessage
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoRepository
import org.lerchenflo.schneaggchatv3mp.utilities.JwtUtils
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
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

    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository,
    private val todoRepository: TodoRepository,

    val appVersion: AppVersion,
    private val loggingRepository: LoggingRepository,
) {
    //Errorchannel for global error events (Show in every screen)
    companion object ErrorChannel{

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

        private val _channel = Channel<ErrorEvent>(capacity = Channel.Factory.BUFFERED)
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
        database.allDatabaseDao().clearAll()
        NotificationManager.removeToken()
        SessionCache.clear()
    }

    suspend fun logout(){
        deleteAllAppData() // delete all app data when logging out

        //Clear access tokens
        preferencemanager.saveTokens(tokenPair = NetworkUtils.TokenPair(accessToken = "", refreshToken = "")) // override credentials with empty string
        KoinPlatform.getKoin().get<HttpClient>(qualifier = named("auth")).clearAuthTokens()

        SessionCache.clear() //Alle variabla löscja
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


    var dataSyncRunning = false
    suspend fun dataSync() {
        if (dataSyncRunning) {
            println("Data sync canceled, already running")
            return
        }
        dataSyncRunning = true

        try {
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
                }
                awaitAll(errorProfilePicJob)
            }

            println("Data sync completed")
        } finally {
            dataSyncRunning = false
        }
    }

    /**
     * Get missing profile pics (On ios after update installation, there are no more pictures)
     */
    suspend fun getMissingPics() {

        //Check user profile pics
        val users = userRepository.getAllUsers()
        var missingPpUserIds: List<String> = emptyList()

        users.forEach { user ->
            val exists = pictureManager.checkImageExists(user.profilePictureUrl)
            if (!exists) {
                println("Profilepic for ${user.name} does not exist: ${user.profilePictureUrl}, getting")
                missingPpUserIds = missingPpUserIds + user.id
            }
        }
        getProfilePicturesForUserIds(missingPpUserIds)



        val groups = groupRepository.getAllGroups()
        var missingPpGroupIds: List<String> = emptyList()

        groups.forEach { group ->
            val exists = pictureManager.checkImageExists(group.profilePictureUrl)
            if (!exists) {
                println("Profilepic for ${group.name} does not exist: ${group.profilePictureUrl}, getting")
                missingPpGroupIds = missingPpGroupIds + group.id
            }
        }
        getProfilePicturesForGroupIds(missingPpGroupIds)

        //TODO: Add image messages if implemented

    }


    /*
    **************************************************************************

    Reusable flows

    **************************************************************************
     */
    /**
     * Get the currently logged in user as flow
     */
    fun getOwnUserFlow(): Flow<User?> {
        return database.userDao().getUserbyIdFlow(SessionCache.getOwnIdValue()).map { user ->
            user?.toUser()
        }
    }

    fun getPendingFriends(searchTerm: String): Flow<List<SelectedChat>> {
        return userRepository.getAllUsersFlow(searchTerm)
            .map { list ->
                list.filter {
                    it.friendshipStatus == NetworkUtils.FriendshipStatus.PENDING
                }
            }
    }


    suspend fun getFriends(searchTerm: String): List<User> {
        return userRepository.getAllUsers().filter {
            it.name.contains(searchTerm)
                    && it.friendshipStatus == NetworkUtils.FriendshipStatus.ACCEPTED
                    && !it.isGroup
        }
    }

    fun getFriendsFlow(searchTerm: String): Flow<List<User>> {
        return userRepository.getAllUsersFlow(searchTerm).map { users ->
            users.filter {
                it.friendshipStatus == NetworkUtils.FriendshipStatus.ACCEPTED
                        && !it.isGroup
            }
        }
    }


    /**
     * Get main screen available items as flow
     */
    fun getChatSelectorFlow(
        searchTerm: String,
        filter: ChatFilter = ChatFilter.NONE
    ): Flow<List<SelectedChat>> {
        val messagesFlow = messageRepository.getAllMessages()
        val usersFlow = userRepository.getAllUsersFlow()
        val groupsFlow = groupRepository.getAllGroupswithMembersFlow()
        val pinnedFlow = preferencemanager.getPinnedChatsFlow()

        return combine(messagesFlow, usersFlow, groupsFlow, pinnedFlow) { messages, users, groups, pinnedList ->

            val loweredSearch = searchTerm.trim().lowercase()
            val ownId = SessionCache.ownId

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

                    if (senderId != ownId.value) {
                        messagesByUser.getOrPut(senderId) { mutableListOf() }.add(msg)
                    }
                    if (receiverId != ownId.value) {
                        messagesByUser.getOrPut(receiverId) { mutableListOf() }.add(msg)
                    }
                }
            }

            // Process users - CREATE NEW IMMUTABLE OBJECTS
            val userItems = users
                .asSequence()
                .filter { it.id != ownId.value }
                .filter { it.friendshipStatus == NetworkUtils.FriendshipStatus.ACCEPTED }
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
     * Actions to execute when the tokenpair is updated
     */
    fun onNewTokenPair(tokenPair: NetworkUtils.TokenPair){

        //Parse the token to get the user id
        val userid = JwtUtils.getUserIdFromToken(tokenPair.refreshToken)

        CoroutineScope(Dispatchers.IO).launch {
            preferencemanager.saveTokens(tokenPair)
            preferencemanager.saveOWNID(userid)
        }

        SessionCache.updateTokenPair(tokenPair)
        SessionCache.updateOwnId(userid)
        SessionCache.updateLoggedIn(true)
        SessionCache.updateOnline(true)
        println(SessionCache.toString())
    }


    var refreshTokenRequestRunning = false //Stop concurrent requests
    /**
     * Function to refresh the tokens
     */
    suspend fun refreshTokens() : RequestError? {
        if (refreshTokenRequestRunning){
            return null
        }
        refreshTokenRequestRunning = true

        println("Tokenmanager refreshing tokenbs")
        val error = tokenManager.refreshTokenPairLocked(networkUtils)

        if (error == null) {
            println("Tokenpair refresh successful")
            SessionCache.updateLoggedIn(true)
            SessionCache.updateOnline(true)
        } else {
            println("Refreshing tokens failed: $error")
        }

        refreshTokenRequestRunning = false
        return error
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
                event = ErrorEvent(
                    401,
                    errorMessageUiText = UiText.StringResourceText(Res.string.error_access_expired),
                    duration = 5000L,
                )
            )
        }

        val credsSaved = tokenDateValid && tokensNotEmpty

        if (credsSaved){
            println("Tokens are saved in local storage, autologin permitted")
            SessionCache.updateOwnId(JwtUtils.getUserIdFromToken(tokens.refreshToken))
            SessionCache.updateTokenPair(tokens)
            SessionCache.updateLoggedIn(true)
            SessionCache.setDeveloperValue(preferencemanager.getDevSettings())
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

                sendErrorSuspend(ErrorEvent(
                    errorMessageUiText = UiText.StringResourceText(Res.string.error_invalid_credentials),
                    duration = 5000L
                ))

                onResult(false)
            }
            is NetworkResult.Success<NetworkUtils.TokenPair> -> {
                onNewTokenPair(result.data)
                onResult(true)

                //New coroutine to persist when this function exits
                CoroutineScope(Dispatchers.IO).launch {
                    dataSync()
                }
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

                sendErrorSuspend(ErrorEvent(
                    error = response.error,
                    duration = 5000L
                ))

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

        val userSyncResponse = networkUtils.userIdSync(localusers.map { (id, changedate) -> NetworkUtils.IdTimeStamp(id, changedate) })

        val profilePicsToGet = emptyList<String>().toMutableList()

        when (userSyncResponse) {
            is NetworkResult.Error<*> -> {println("userid sync error")}
            is NetworkResult.Success<NetworkUtils.UserSyncResponse> -> {
                println("Userid sync response: ${userSyncResponse.data.toString()}")

                val updatedUsers = userSyncResponse.data.updatedUsers
                val deletedUsers = userSyncResponse.data.deletedUsers

                updatedUsers.forEach { newUser ->
                    when (newUser) {
                        is NetworkUtils.UserResponse.FriendUserResponse -> {
                            val existing = database.userDao().getUserbyId(newUser.id)
                            database.userDao().upsert(UserDto(
                                id = newUser.id,
                                changedate = newUser.updatedAt,
                                name = newUser.username,
                                description = newUser.userDescription,
                                status = newUser.userStatus,
                                birthDate = newUser.birthDate,
                                frienshipStatus = NetworkUtils.FriendshipStatus.ACCEPTED,
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
                                profilePictureUrl = ""
                            ))
                            profilePicsToGet += newUser.id
                        }
                        is NetworkUtils.UserResponse.SelfUserResponse -> {
                            val existing = database.userDao().getUserbyId(newUser.id)
                            database.userDao().upsert(UserDto(
                                id = newUser.id,
                                changedate = newUser.updatedAt,
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
                                profilePictureUrl = ""

                            ))
                            profilePicsToGet += newUser.id
                        }
                        is NetworkUtils.UserResponse.SimpleUserResponse -> {
                            database.userDao().upsert(UserDto(
                                id = newUser.id,
                                changedate = newUser.updatedAt,
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
                                profilePictureUrl = ""
                            ))

                            //Get the profile pic for this user too??
                            profilePicsToGet += newUser.id

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

        getProfilePicturesForUserIds(profilePicsToGet)
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
    suspend fun getAvailableUsers(searchTerm: String) : List<NetworkUtils.NewFriendsUserResponse> {
        return when (val response = networkUtils.getAvailableUsers(searchTerm)) {
            is NetworkResult.Error<RequestError> -> {
                sendErrorSuspend(ErrorEvent(
                    error = response.error
                ))
                emptyList()
            }
            is NetworkResult.Success<List<NetworkUtils.NewFriendsUserResponse>> -> {
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
    suspend fun changeUserDetails( // todo flo macht a server update denn loft des
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




    /**
     * @param localpk Local pk, only pass if already in db
     *
     */
    suspend fun sendTextMessage(empfaenger: String, gruppe: Boolean, content: String, answerid: String?, localpk: Long = 0){

        var localpkintern = localpk

        if (SessionCache.getOwnIdValue() == null){
            println("Message senden abort: No OWNID")
            return
        }

        val senddate = getCurrentTimeMillisString()

        //Interne message macha die ned alles hot
        val messageDto = MessageDto(
            localPK = localpkintern,
            id = null,
            msgType = MessageType.TEXT,
            content = content,
            senderId = SessionCache.getOwnIdValue()!!,
            receiverId = empfaenger,
            sendDate = senddate,
            changedate = senddate,
            deleted = false,
            groupMessage = gruppe,
            answerId = answerid,
            sent = false,
            myMessage = true,
            readByMe = true
        )

        //Nachricht hot scho a pk vo da db, also scho din
        if (localpkintern == 0L){
            localpkintern = database.messageDao().insertMessageDto(messageDto)
            println("LocalPK: $localpkintern")
        }


        val serverrequest = networkUtils.sendTextMessageToServer(
            empfaenger = empfaenger,
            gruppe = gruppe,
            content = content,
            answerid = answerid
        )

        when (serverrequest){
            is NetworkResult.Error<*> -> println("Message senden error: ${serverrequest.error}")
            is NetworkResult.Success<MessageResponse> -> {
                withContext(Dispatchers.IO) {

                    println("Messageid returned from server: ${serverrequest.data.messageId}")

                    messageRepository.upsertMessage(
                        Message(
                            localPK = localpkintern,
                            id = serverrequest.data.messageId,
                            msgType = serverrequest.data.msgType,
                            content = serverrequest.data.content,
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
                }
            }
        }

    }



    fun sendOfflineMessages(){
        CoroutineScope(Dispatchers.IO).launch {

            val messages = messageRepository.getUnsentMessages()

            println("Unsent message count: $messages")

            //Do no parallel des loft jetzt alles seriell
            for (m in messages){
                if (m.isText()){
                    try {
                        sendTextMessage(
                            empfaenger = m.receiverId,
                            gruppe = m.groupMessage,
                            content = m.content,
                            answerid = m.answerId,
                            localpk = m.localPK
                        )
                    } catch (e: Exception){
                        println("Retry send failed for localPK=${m.localPK}: $e")
                        // optional: increment retry counter in DB, break or continue
                    }
                }
                //TODO: Offline message send for images
            }
        }

    }



    /**
     * Execute a message sync
     */
    suspend fun messageIdSync() {

        val localmessages = messageRepository.getmessagechangeid()
        val imagesToGet = mutableListOf<String>()

        var currentPage = 0
        var moreMessages = true

        while (moreMessages) {

            val messageSyncResponse = networkUtils.messageSync(
                messageIds = localmessages.map { NetworkUtils.IdTimeStamp(it.id, it.changedate) },
                page = currentPage
            )

            if (SessionCache.getOwnIdValue() == null) {
                println("MESSAGEIDSYNC ABORT -- Ownid")
                return //Exit the sync when the own id is not set (Prevent messages from getting inserted with myMessage bool wrong)
            }


            when (messageSyncResponse) {
                is NetworkResult.Error<*> -> {
                    println("messageid sync error")
                    break // Stop on error
                }

                is NetworkResult.Success<NetworkUtils.MessageSyncResponse> -> {
                    val updatedMessages = messageSyncResponse.data.updatedMessages
                    val deletedMessages = messageSyncResponse.data.deletedMessages
                    moreMessages = messageSyncResponse.data.moreMessages

                    updatedMessages.forEach { messageResponse ->
                        when (messageResponse.msgType) {
                            MessageType.IMAGE -> {
                                imagesToGet += messageResponse.messageId
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
                                        myMessage = messageResponse.senderId == SessionCache.getOwnIdValue(),
                                        readByMe = messageResponse.readers.any { it.userId == SessionCache.ownId.value },
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
                                //TODO: Same as normal message but with poll answer upsert
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
                                        myMessage = messageResponse.senderId == SessionCache.getOwnIdValue(),
                                        readByMe = messageResponse.readers.any { it.userId == SessionCache.ownId.value },
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
                        }
                    }

                    // Delete all non-existing messages
                    deletedMessages.forEach { id ->
                        messageRepository.deleteMessage(id)
                    }
                }
            }

            currentPage++
        }

        println("Messagesync completed. Total pages: $currentPage")

        //TODO: Get all images
        if (imagesToGet.isNotEmpty()) {
            println("Images to fetch: ${imagesToGet.size}")
            // fetchImages(imagesToGet)
        }
    }


    suspend fun setAllChatMessagesRead(chatid: String, gruppe: Boolean, timestamp: String){
        messageRepository.setAllChatMessagesRead(chatid, gruppe, timestamp)

        networkUtils.setMessagesRead(chatid, gruppe, timestamp.toLong())
    }


    suspend fun editMessage(messageId: String, newContent: String) {
        val request = networkUtils.editMessage(
            messageId = messageId,
            newContent = newContent
        )

        when (request) {
            is NetworkResult.Error<RequestError> ->  {
                sendErrorSuspend(ErrorEvent(error = request.error))
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

    suspend fun deleteLocalMessage(localpk: Long) {
        messageRepository.deleteMessage(localpk)
    }

    suspend fun deleteMessage(messageId: String){
        val request = networkUtils.deleteMessage(
            messageId = messageId
        )

        when (request) {
            is NetworkResult.Error<RequestError> ->  {
                sendErrorSuspend(ErrorEvent(error = request.error))
            }
            is NetworkResult.Success<*> -> {
                messageRepository.deleteMessage(messageId)
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
                sendErrorSuspend(ErrorEvent(error = response.error))
                null
            }
            is NetworkResult.Success<NetworkUtils.GroupResponse> -> {
                response.data.id
            }
        }
    }


    suspend fun groupIdSync() {

        val localgroups = groupRepository.getgroupchangeid()
        val profilepicsToGet = mutableListOf<String>()

        val groupSyncResponse = networkUtils.groupIdSync(
            groupIds = localgroups.map {
                NetworkUtils.IdTimeStamp(it.id, it.changedate)
            }
        )


        when (groupSyncResponse) {
            is NetworkResult.Error<*> -> {
                println("groupid sync error")
            }

            is NetworkResult.Success<NetworkUtils.GroupSyncResponse> -> {
                println("GroupIdSync sync response: ${groupSyncResponse.data.toString()}")



                groupSyncResponse.data.updatedGroups.forEach { groupResponse ->
                    val existing = groupRepository.getGroupById(groupResponse.id)
                    groupRepository.upsertGroup(Group(
                        id = groupResponse.id,
                        name = groupResponse.name,
                        profilePictureUrl = "",
                        description = groupResponse.description,
                        createDate = groupResponse.createdAt,
                        changedate = groupResponse.updatedAt,
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

                    profilepicsToGet += groupResponse.id
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

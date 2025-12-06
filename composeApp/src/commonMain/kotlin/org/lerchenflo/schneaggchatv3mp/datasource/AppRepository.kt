@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.datasource

import androidx.compose.runtime.Composable
import io.ktor.http.cio.Request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.relations.MessageWithReadersDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.toMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.toSelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.toUser
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatFilter
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.MessageResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.RequestError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.onError
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoRepository
import org.lerchenflo.schneaggchatv3mp.utilities.JwtUtils
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_access_expired
import schneaggchatv3mp.composeapp.generated.resources.error_access_not_permitted
import schneaggchatv3mp.composeapp.generated.resources.error_invalid_credentials
import kotlin.time.ExperimentalTime

class AppRepository(
    private val database: AppDatabase,
    private val networkUtils: NetworkUtils,
    private val preferencemanager: Preferencemanager,
    private val pictureManager: PictureManager,

    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository,
    private val todoRepository: TodoRepository,

    val appVersion: AppVersion, //Appversion, uf des darf jeder zugriefa

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

                // Add errorCode if provided
                if (errorCode != null){
                    finalstr += "Errorcode: ${errorCode}\n"
                }

                // Add error.errorCode if provided (even if errorCode was also provided)
                if (error != null) {
                    finalstr += "Error.errorCode: ${error.errorCode}\n"

                    // Add error message from RequestError
                    if (error.message != null){
                        finalstr += "Error.message: ${error.message}\n"
                    }
                }

                // Add errormessage if provided
                if (errorMessage != null)
                    finalstr += "ErrorMessage: ${errorMessage}\n"

                // Add UI text if provided
                if (errorMessageUiText != null)
                    finalstr += "UiText: ${errorMessageUiText.asString()}\n"


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





    suspend fun deleteAllAppData(){
        database.allDatabaseDao().clearAll()
        NotificationManager.removeToken()
    }

    //TODO: Move into message repository
    fun getMessagesByUserId(userId: String, gruppe: Boolean): Flow<List<Message>> {
        return database.messageDao().getMessagesByUserIdFlow(userId, gruppe).map { messages ->
            messages.map { it.toMessage() }
        }
    }

    fun getownUser(): Flow<User?> {
        return database.userDao().getUserbyIdFlow(SessionCache.getOwnIdValue()).map { user ->
            user?.toUser()
        }
    }



    //Get main screen available items as flow
    fun getChatSelectorFlow(
        searchTerm: String,
        filter: ChatFilter = ChatFilter.NONE
    ): Flow<List<SelectedChat>> {
        val messagesFlow = messageRepository.getAllMessages()
        val usersFlow = userRepository.getallusers()
        val groupsFlow = groupRepository.getallgroupswithmembers()

        return combine(messagesFlow, usersFlow, groupsFlow) { messages, users, groups ->

            val loweredSearch = searchTerm.trim().lowercase()
            val ownId = SessionCache.ownId

            // PRE-PROCESS: Build message indexes for O(1) lookup
            val messagesByUser = mutableMapOf<String, MutableList<Message>>()
            val messagesByGroup = mutableMapOf<String, MutableList<Message>>()
            val userIdMap = users.associateBy { it.id }

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
                        lastMessage = last
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
                        lastMessage = last
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

            filtered.sortedByDescending { it.lastmessage?.getSendDateAsLong() ?: 0L }

        }.flowOn(Dispatchers.Default)
    }


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
        println("Sessioncache: ${SessionCache.toDetailedString()}")
    }


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
        }

        return credsSaved
    }


    suspend fun testServer() : Boolean {
        return when(val response = networkUtils.test()){
            is NetworkResult.Error<*> -> false
            is NetworkResult.Success<*> -> true
        }
    }


    fun login(
        username: String,
        password: String,
        onResult: (Boolean) -> Unit
    ) {

        CoroutineScope(Dispatchers.IO).launch {
            when(val result = networkUtils.login(username, password)){
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
                }
            }

        }

    }


    fun createAccount(
        username: String,
        email: String,
        password: String,
        birthdate: String,
        profilePic: ByteArray,
        onResult: (Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
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
    }


    var refreshTokenRequestRunning = false //Stop concurrent requests
    suspend fun refreshTokens() : RequestError? {
        if (refreshTokenRequestRunning){
            return null
        }
        refreshTokenRequestRunning = true

        val tokens = preferencemanager.getTokens()

        println("Token refresh networktask starting: $tokens")
        return when(val result = networkUtils.refresh(tokens.refreshToken)){
            is NetworkResult.Error<*> -> {
                println("Refreshing tokens failed: ${result.error}")
                refreshTokenRequestRunning = false
                result.error
            }
            is NetworkResult.Success<NetworkUtils.TokenPair> -> {
                preferencemanager.saveTokens(result.data)
                println("Tokenpair refresh successful")
                onNewTokenPair(result.data)
                refreshTokenRequestRunning = false
                null
            }
        }
    }



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
                                changedate = newUser.updatedAt.toLong(),
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

    private suspend fun getProfilePicturesForUserIds(userIds: List<String>){
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



    //Add new users
    suspend fun getAvailableUsers(searchTerm: String) : List<NetworkUtils.NewFriendsUserResponse> {
        return when (val response = networkUtils.getAvailableUsers(searchTerm)) {
            is NetworkResult.Error<*> -> {
                //TODO: FABI Get available users failed (Popup??)
                emptyList()
            }
            is NetworkResult.Success<List<NetworkUtils.NewFriendsUserResponse>> -> {
                response.data
            }
        }

    }

    //Send or accept friend request
    suspend fun sendFriendRequest(friendId: String) : Boolean {
        when (val success = networkUtils.sendFriendRequest(friendId)){
            is NetworkResult.Error<*> -> return false
            is NetworkResult.Success<*> -> {
                userIdSync()
                return true
            }
        }
    }

    suspend fun denyFriendRequest(friendId: String) : Boolean {
        when (val success = networkUtils.denyFriendRequest(friendId)){
            is NetworkResult.Error<*> -> return false
            is NetworkResult.Success<*> -> {
                userIdSync()
                return true
            }
        }
    }



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

                    messageRepository.upsertMessageWithoutReaders(
                        MessageDto(
                            localPK = localpkintern,
                            id = serverrequest.data.messageId,
                            msgType = serverrequest.data.msgType,
                            content = serverrequest.data.content,
                            senderId = serverrequest.data.senderId,
                            receiverId = serverrequest.data.receiverId,
                            sendDate = serverrequest.data.sendDate.toString(),
                            changedate = serverrequest.data.lastChanged.toString(),
                            deleted = serverrequest.data.deleted,
                            groupMessage = serverrequest.data.groupMessage,
                            answerId = serverrequest.data.answerId,
                            sent = true,
                            myMessage = true,
                            readByMe = true
                        )

                    )

                    //TODO: Message gelesen + reader
                    /*
                    database.messageDao().markMessageAsSent( msgid, localpkintern)

                    database.messagereaderDao().upsertReader(MessageReaderDto(
                        messageId = msgid,
                        readerID = SessionCache.getOwnIdValue() ?: 0,
                        readDate = messageDto.sendDate
                    ))

                     */
                }
            }
        }

    }


    /*

    fun sendOfflineMessages(){
        CoroutineScope(Dispatchers.IO).launch {
            val messages = database.messageDao().getUnsentMessages()

            println("Unsent: $messages")

            //Do no parallel des loft jetzt alles seriell
            for (m in messages){
                try {
                    sendMessage(
                        msgtype = m.msgType,
                        empfaenger = m.receiverId,
                        gruppe = m.groupMessage,
                        content = m.content,
                        answerid = m.answerId,
                        sendedatum = m.sendDate,
                        localpk = m.localPK
                    )
                } catch (e: Exception){
                    println("Retry send failed for localPK=${m.localPK}: $e")
                    // optional: increment retry counter in DB, break or continue
                }
            }
        }

    }




    //Network züg
    fun executeSync(onLoadingStateChange: (Boolean) -> Unit) {
        // global handler for uncaught exceptions inside the scope
        val handler = CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
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
                            e.printStackTrace()
                        }
                    }

                    val userSync = async {
                        try {
                            networkUtils.executeUserIDSync(userRepository = userRepository)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val groupSync = async {
                        try {
                            networkUtils.executeGroupIDSync(groupRepository = groupRepository)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val todoSync = async {
                        try {
                            networkUtils.executeTodoIDSync(todoRepository = todoRepository)
                        } catch (e: Exception) {
                            e.printStackTrace()
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
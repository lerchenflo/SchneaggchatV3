@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.datasource

import androidx.compose.runtime.Composable
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
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageWithReadersDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.toSelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.toUser
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatFilter
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.RequestError
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoRepository
import org.lerchenflo.schneaggchatv3mp.utilities.JwtUtils
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_access_expired
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
            val errorMessageUiText: UiText? = null,
            val duration: Long = 5000L
        ){
            @Composable
            fun toStringComposable(): String {
                var finalstr = ""

                //Add errorcode
                finalstr += if (errorCode != null) "Errorcode: ${errorCode}\n" else "" //TODO FABI? Add errorcode tostring (in network error class and insert here)
                //TODO FABI erste zeile ca so(oda andersch, vlt o code unta mir egal) Errorcode: 401 Forbidden(No valid credentials) //Goht eh locker mit uitext
                //Add errormessage
                if (errorMessage != null)
                    finalstr += errorMessage + "\n"

                if (errorMessageUiText != null)
                    finalstr += errorMessageUiText.asString()

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

    fun getMessagesByUserId(userId: String, gruppe: Boolean): Flow<List<MessageWithReadersDto>> {
        return database.messageDao().getMessagesByUserId(userId, gruppe)
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
        val messagesFlow = messageRepository.getAllMessagesWithReaders()
        val usersFlow = userRepository.getallusers()
        val groupsFlow = groupRepository.getallgroupswithmembers()

        return combine(messagesFlow, usersFlow, groupsFlow) { messages, users, groups ->

            println("Gegnerauswahl refresh: DB Users: ${users.map { it.name to it.id }}")

            val loweredSearch = searchTerm.trim().lowercase()
            val ownId = SessionCache.ownId

            // PRE-PROCESS: Build message indexes for O(1) lookup
            val messagesByUser = mutableMapOf<String, MutableList<MessageWithReadersDto>>()
            val messagesByGroup = mutableMapOf<String, MutableList<MessageWithReadersDto>>()
            val userIdMap = users.associateBy { it.id }

            // Single pass through messages to build indexes
            messages.forEach { msg ->
                if (msg.isGroupMessage()) {
                    messagesByGroup.getOrPut(msg.messageDto.receiverId) { mutableListOf() }.add(msg)
                } else {
                    val senderId = msg.messageDto.senderId
                    val receiverId = msg.messageDto.receiverId

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
                        // Update sender name in a copy (if MessageWithReadersDto is immutable)
                        messageDto.senderAsString =
                            userIdMap[messageDto.senderId]?.name ?: messageDto.senderAsString
                    }

                    // Count in single pass
                    var unreadCount = 0
                    var unsentCount = 0
                    userMessages.forEach { message ->
                        if (!message.isReadbyMe()) unreadCount++
                        if (!message.messageDto.sent) unsentCount++
                    }

                    // CREATE NEW IMMUTABLE OBJECT - don't mutate original user
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
                        messageDto.senderAsString =
                            userIdMap[messageDto.senderId]?.name ?: "Unknown"
                    }

                    var unreadCount = 0
                    var unsentCount = 0
                    groupMessages.forEach { message ->
                        if (!message.isReadbyMe()) unreadCount++
                        if (!message.messageDto.sent) unsentCount++
                    }

                    // CREATE NEW IMMUTABLE OBJECT - don't mutate original group
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

        println("LOGIN: Userid: $userid")
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
            AppRepository.trySendError(
                event = AppRepository.ErrorChannel.ErrorEvent(
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

                    //TODO: Improve error messages (One string for each error message???)
                    sendErrorSuspend(ErrorEvent(
                        errorMessage = result.error.toString(),
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
                        errorMessage = response.error.toString(),
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


        println("profilepics to get count: ${profilePicsToGet.size}")

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
            is NetworkResult.Success<*> -> {
                response.data as List<NetworkUtils.NewFriendsUserResponse>
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


    /* TODO vornazua wieder iboua

    /**
     * @param localpk Local pk, only pass if already in db
     *
     */
    suspend fun sendMessage(msgtype: String, empfaenger: Long, gruppe: Boolean, content: String, answerid: Long, sendedatum: String, localpk: Long = 0){

        var localpkintern = localpk


        if (SessionCache.getOwnIdValue() == null){
            println("Message senden abort: No OWNID")
            return
        }


        //Interne message macha die ned alles hot
        val messageDto = MessageDto(
            localPK = localpkintern,
            id = null,
            msgType = msgtype,
            content = content,
            senderId = SessionCache.getOwnIdValue() ?: 0,
            receiverId = empfaenger,
            sendDate = sendedatum,
            changeDate = sendedatum,
            deleted = false,
            groupMessage = gruppe,
            answerId = answerid,
            sent = false
        )

        //Nachricht hot scho a pk vo da db, also scho din
        if (localpkintern == 0L){
            localpkintern = database.messageDao().insertMessage(messageDto)
            println("LocalPK: $localpkintern")
        }


        val serverrequest = networkUtils.sendMessageToServer(msgtype, empfaenger, gruppe, content, answerid, sendedatum)
        serverrequest.onSuccess { headers ->

            withContext(Dispatchers.IO) {
                val msgid = headers["msgid"]?.toLong()


                if (msgid != null){
                    println("Message gesendet: msgid $msgid")

                    database.messageDao().markMessageAsSent( msgid, localpkintern)

                    database.messagereaderDao().upsertReader(MessageReaderDto(
                        messageId = msgid,
                        readerID = SessionCache.getOwnIdValue() ?: 0,
                        readDate = messageDto.sendDate
                    ))
                }else{
                    println("Message senden error: Keine Msgid erhalten -----------------------------------------------------------------------")
                }
            }

        }
        serverrequest.onError {
            println("Message senden error: $it")
        }
    }


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
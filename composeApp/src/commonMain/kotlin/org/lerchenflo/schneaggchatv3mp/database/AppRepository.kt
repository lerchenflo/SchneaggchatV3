package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.Transaction
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatEntity
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatSelectorItem
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.network.util.onError
import org.lerchenflo.schneaggchatv3mp.network.util.onSuccess
import org.lerchenflo.schneaggchatv3mp.network.util.onSuccessWithBody
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoRepository
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager

class AppRepository(
    private val database: AppDatabase,
    private val networkUtils: NetworkUtils,
    private val preferencemanager: Preferencemanager,

    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository,
    private val todoRepository: TodoRepository,
) {

    suspend fun deleteAllAppData(){
        database.allDatabaseDao().clearAll()
    }

    @Transaction
    fun getMessagesByUserId(userId: Long, gruppe: Boolean): Flow<List<MessageWithReaders>> {
        return database.messageDao().getMessagesByUserId(userId, gruppe)
    }

    @Transaction
    fun getownUser(): Flow<User?> {
        return database.userDao().getUserbyId(SessionCache.getOwnIdValue()?: 0)
    }



    //Gegnerauswahl getten
    // In repository / data layer
    fun getChatSelectorFlow(searchTerm: String): Flow<List<ChatSelectorItem>> {
        val messagesFlow = messageRepository.getAllMessagesWithReaders()
        val usersFlow = userRepository.getallusers()
        val groupsFlow = groupRepository.getallgroupswithmembers()

        return combine(messagesFlow, usersFlow, groupsFlow) { messages, users, groups ->

            val loweredSearch = searchTerm.trim().lowercase()

            val userItems = users.map { user ->
                val last = messages
                    .filter {
                        (it.message.senderId == user.id || it.message.receiverId == user.id)
                                && !it.isGroupMessage()
                    }
                    .maxByOrNull { it.getSendDateAsLong() }

                ChatSelectorItem(
                    id = user.id,
                    gruppe = false,
                    lastmessage = last, // may be null
                    entity = ChatEntity.UserEntity(user)
                )
            }.filter { item ->
                loweredSearch.isEmpty() ||
                        (item.entity as? ChatEntity.UserEntity)?.user?.name?.lowercase()?.contains(loweredSearch) == true
            }

            val groupItems = groups.map { gwm ->
                val groupId = gwm.group.id
                val last = messages
                    .filter { it.message.receiverId == groupId && it.isGroupMessage() }
                    .maxByOrNull { it.getSendDateAsLong() }

                ChatSelectorItem(
                    id = groupId,
                    gruppe = true,
                    lastmessage = last, // may be null
                    entity = ChatEntity.GroupEntity(gwm)
                )
            }.filter { item ->
                loweredSearch.isEmpty() ||
                        (item.entity as? ChatEntity.GroupEntity)?.groupWithMembers?.group?.name?.lowercase()?.contains(loweredSearch) == true
            }

            (userItems + groupItems)
                .sortedByDescending { it.lastmessage?.getSendDateAsLong() ?: 0L } // nulls treated as 0
        }.flowOn(Dispatchers.Default)
    }







    fun login(
        username: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {

        CoroutineScope(Dispatchers.IO).launch {
            networkUtils.login(username, password)
                .onSuccessWithBody { headers, message ->
                    //println("Success: $success $message")
                    CoroutineScope(Dispatchers.IO).launch {
                        preferencemanager.saveAutologinCreds(username, password)
                        preferencemanager.saveOWNID(headers["userid"]?.toLong() ?: 0)
                    }

                    println(headers)
                    SessionCache.updateSessionId(headers["sessionid"])
                    SessionCache.updateOwnId(headers["userid"]?.toLong())
                    SessionCache.updateLoggedIn(true)
                    println("Sessioncache: ${SessionCache.toString()}")
                    onResult(true, message)
                }
                .onError { error ->
                    println("Error: $error")

                    onResult(false, error.toString())
                }
        }

    }

    fun createAccount(
        username: String,
        email: String,
        password: String,
        gender: String,
        birthdate: String,
        onResult: (Boolean, String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            networkUtils.createAccount(username, password, email, gender, birthdate)
                .onSuccessWithBody { success, message ->
                    println("Success: $success $message")
                    onResult(success, message)
                }
                .onError { error ->
                    println("Error: $error")

                    onResult(false, error.toString())
                }
        }
    }

    /**
     * @param localpk Local pk, only pass if already in db
     *
     */
    suspend fun sendMessage(msgtype: String, empfaenger: Long, gruppe: Boolean, content: String, answerid: Long, sendedatum: String, localpk: Long = 0){

        var localpkintern = localpk

        //TODO: FABI leas des
        /*
        Do siaht ma schö s prinzip vo nam repository. die funktion ruft ma uf, und sie addet die nachricht in die lokale datenbank und versuacht
        glichzittig no an send zum server. wenn da server des ned mag oda halt offline isch, denn wird se lokal gspeichert und kann süäter neu gschickt werra.
        so wird se direkt im chat azoagt und ma muss o nur ua tolle funktion ufrufa
         */

        if (SessionCache.getOwnIdValue() == null){
            println("Message senden abort: No OWNID")
            return
        }


        //Interne message macha die ned alles hot
        val message = Message(
            localPK = localpkintern,
            id = 0,
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
            localpkintern = database.messageDao().insertMessage(message)
            println("LocalPK: $localpkintern")
        }


        val serverrequest = networkUtils.sendMessageToServer(msgtype, empfaenger, gruppe, content, answerid, sendedatum)
        serverrequest.onSuccess { headers ->

            withContext(Dispatchers.IO) {
                val msgid = headers["msgid"]?.toLong()


                if (msgid != null){
                    println("Message gesendet: msgid $msgid")

                    database.messageDao().markMessageAsSent( msgid, localpkintern)

                    database.messagereaderDao().upsertReader(MessageReader(
                        messageId = msgid,
                        readerID = SessionCache.getOwnIdValue() ?:0,
                        readDate = message.sendDate
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


    suspend fun areLoginCredentialsSaved(): Boolean{
        val (username, password) = preferencemanager.getAutologinCreds()
        if (username.isNotBlank() && password.isNotBlank()){
            SessionCache.updateOwnId(preferencemanager.getOWNID())
            SessionCache.updateUsername(username)
            SessionCache.updatePassword(password)
        }
        return username.isNotBlank() && password.isNotBlank()
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


}
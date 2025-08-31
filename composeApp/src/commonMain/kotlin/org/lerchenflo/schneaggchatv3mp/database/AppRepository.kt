package org.lerchenflo.schneaggchatv3mp.database

import androidx.compose.ui.graphics.Path.Companion.combine
import androidx.compose.ui.text.style.TextDecoration.Companion.combine
import androidx.lifecycle.viewModelScope
import androidx.room.Transaction
import io.ktor.util.Hash.combine
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.lerchenflo.schneaggchatv3mp.LOGGEDIN
import org.lerchenflo.schneaggchatv3mp.OWNID
import org.lerchenflo.schneaggchatv3mp.SESSIONID
import org.lerchenflo.schneaggchatv3mp.chat.presentation.ChatEntity
import org.lerchenflo.schneaggchatv3mp.chat.presentation.ChatSelectorItem
import org.lerchenflo.schneaggchatv3mp.database.tables.Group
import org.lerchenflo.schneaggchatv3mp.database.tables.GroupMember
import org.lerchenflo.schneaggchatv3mp.database.tables.GroupWithMembers
import org.lerchenflo.schneaggchatv3mp.database.tables.Message
import org.lerchenflo.schneaggchatv3mp.database.tables.MessageReader
import org.lerchenflo.schneaggchatv3mp.database.tables.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.database.tables.User
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.network.util.onError
import org.lerchenflo.schneaggchatv3mp.network.util.onSuccess
import org.lerchenflo.schneaggchatv3mp.network.util.onSuccessWithBody
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager

class AppRepository(
    private val database: AppDatabase,
    private val networkUtils: NetworkUtils,
    private val preferencemanager: Preferencemanager

) {

    suspend fun upsertUser(user: User){
        database.userDao().upsert(user)
    }

    suspend fun deleteAllAppData(){
        database.allDatabaseDao().clearAll()
    }

    suspend fun deleteUser(userid: Long){
        database.userDao().delete(userid)
    }

    fun getallusers(searchterm: String = ""): Flow<List<User>>{
        return database.userDao().getallusers(searchterm)
    }

    @Transaction
    suspend fun getuserchangeid(): List<IdChangeDate>{
        return database.userDao().getUserIdsWithChangeDates()
    }



    suspend fun upsertMessage(message: Message){
        database.messageDao().upsertMessage(message)
    }

    suspend fun upsertMessages(messages: List<Message>){
        database.messageDao().upsertMessages(messages)
    }

    @Transaction
    suspend fun upsertMessagesWithReaders(batch: List<MessageWithReaders>) {
        for (mwr in batch) {
            // upsert the message first (message.id must exist in JSON or be assigned)
            upsertMessage(mwr.message)

            // normalize readers to ensure messageId matches message.id
            val readers = mwr.readers.map { it.copy(messageId = mwr.message.id) }
            if (readers.isNotEmpty()) insertReaders(readers)
        }
    }

    @Transaction
    suspend fun upsertMessageWithReaders(message: MessageWithReaders) {
        upsertMessage(message.message)

        // normalize readers to ensure messageId matches message.id
        val readers = message.readers.map { it.copy(messageId = message.message.id) }
        if (readers.isNotEmpty()) insertReaders(readers)
    }

    @Transaction
    fun getAllMessagesWithReaders(): Flow<List<MessageWithReaders>>{
        return database.messageDao().getAllMessagesWithReaders()
    }

    @Transaction
    suspend fun getmessagechangeid(): List<IdChangeDate>{
        return database.messageDao().getMessageIdsWithChangeDates()
    }

    @Transaction
    fun getMessagesByUserId(userId: Long, gruppe: Boolean): Flow<List<MessageWithReaders>> {
        return database.messageDao().getMessagesByUserId(userId, gruppe)
    }




    suspend fun insertReader(reader: MessageReader) {
        database.messagereaderDao().upsertReader(reader)
    }


    suspend fun insertReaders(readers: List<MessageReader>){
        database.messagereaderDao().upsertReaders(readers)
    }

    suspend fun deleteReadersForMessage(messageId: Long){
        database.messagereaderDao().deleteReadersForMessage(messageId)
    }


    suspend fun upsertGroup(group: Group){
        database.groupDao().upsertGroup(group)
    }

    @Transaction
    suspend fun getgroupchangeid(): List<IdChangeDate>{
        return database.groupDao().getGroupIdsWithChangeDates()
    }

    @Transaction
    fun getallgroupswithmembers(): Flow<List<GroupWithMembers>> {
        return database.groupDao().getAllGroupsWithMembers()
    }

    suspend fun deleteGroup(groupid: Long){
        database.groupDao().deleteGroup(groupid)
    }

    @Transaction
    suspend fun upsertGroupWithMembers(gwm: GroupWithMembers) {
        // 1) upsert the group
        database.groupDao().upsertGroup(gwm.group)

        // 3) replace membership rows for this group
        // delete old members for the group
        database.groupDao().deleteMembersForGroup(gwm.group.id)

        // create new join rows and insert
        val joinRows = gwm.members.map { member ->
            GroupMember(0, gwm.group.id, member.id, member.color, member.joinDate, member.isAdmin)
        }

        if (joinRows.isNotEmpty()) {
            database.groupDao().upsertMembers(joinRows)
        }
    }


    //Gegnerauswahl getten
    // In repository / data layer
    fun getChatSelectorFlow(searchTerm: String): Flow<List<ChatSelectorItem>> {
        val messagesFlow = getAllMessagesWithReaders()
        val usersFlow = getallusers()
        val groupsFlow = getallgroupswithmembers()

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
        // chat von flo: bitte a todo iboua dassas mem login besser gmacht wird des mitam login delay

        CoroutineScope(Dispatchers.IO).launch {
            networkUtils.login(username, password)
                .onSuccessWithBody { headers, message ->
                    //println("Success: $success $message")
                    CoroutineScope(Dispatchers.IO).launch {
                        preferencemanager.saveAutologinCreds(username, password)
                        preferencemanager.saveOWNID(headers["userid"]?.toLong() ?: 0)
                    }

                    println(headers)
                    SESSIONID = headers["sessionid"]
                    OWNID = headers["userid"]?.toLong()
                    println("SESSIONID gesetzt: $SESSIONID")
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

        if (OWNID == null){
            println("Message senden abort: No OWNID")
            return
        }


        //Interne message macha die ned alles hot
        val message = Message(
            localPK = localpkintern,
            id = 0,
            msgType = msgtype,
            content = content,
            senderId = OWNID ?: 0,
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
                        readerID = OWNID ?:0,
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


    //TODO: Do passiert an fehler, iwenn gits ahuffa ungelesene messages die alle nomml gschickt werrand ka warum
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
            OWNID = preferencemanager.getOWNID()
        }
        CoroutineScope(Dispatchers.IO).launch {
            login(username, password, onResult = { success, body ->
                LOGGEDIN = success
                println("LOGGEDIN $success")
            })
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
                            networkUtils.executeMsgIDSync(appRepository = this@AppRepository, onLoadingStateChange = {})
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val userSync = async {
                        try {
                            networkUtils.executeUserIDSync(appRepository = this@AppRepository)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val groupSync = async {
                        try {
                            networkUtils.executeGroupIDSync(appRepository = this@AppRepository)
                        } catch (e: Exception) {
                            e.printStackTrace()
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


}
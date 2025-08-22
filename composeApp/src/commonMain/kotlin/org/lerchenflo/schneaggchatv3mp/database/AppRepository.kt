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
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.lerchenflo.schneaggchatv3mp.database.tables.Group
import org.lerchenflo.schneaggchatv3mp.database.tables.GroupMember
import org.lerchenflo.schneaggchatv3mp.database.tables.GroupWithMembers
import org.lerchenflo.schneaggchatv3mp.database.tables.Message
import org.lerchenflo.schneaggchatv3mp.database.tables.MessageReader
import org.lerchenflo.schneaggchatv3mp.database.tables.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.database.tables.User
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils

class AppRepository(
    private val database: AppDatabase,
    private val networkUtils: NetworkUtils
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
        database.messageDao().updateMessage(message)
    }

    suspend fun upsertMessages(messages: List<Message>){
        database.messageDao().updateMessages(messages)
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
    fun getMessagesByUserId(userId: Long): Flow<List<MessageWithReaders>> {
        return database.messageDao().getMessagesByUserId(userId)
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
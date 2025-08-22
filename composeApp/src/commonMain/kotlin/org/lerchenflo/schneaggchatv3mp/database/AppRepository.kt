package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
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



}
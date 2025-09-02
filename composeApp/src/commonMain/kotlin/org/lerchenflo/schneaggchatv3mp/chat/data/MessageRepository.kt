package org.lerchenflo.schneaggchatv3mp.chat.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils

class MessageRepository(
    private val database: AppDatabase,
    private val networkUtils: NetworkUtils,
) {

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
package org.lerchenflo.schneaggchatv3mp.chat.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageReaderDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageWithReadersDto
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils

class MessageRepository(
    private val database: AppDatabase,
    private val networkUtils: NetworkUtils,
) {

    suspend fun upsertMessage(messageDto: MessageDto){
        database.messageDao().upsertMessage(messageDto)
    }

    suspend fun upsertMessages(messageDtos: List<MessageDto>){
        database.messageDao().upsertMessages(messageDtos)
    }

    @Transaction
    suspend fun upsertMessagesWithReaders(batch: List<MessageWithReadersDto>) {
        for (mwr in batch) {
            // upsert the message first (message.id must exist in JSON or be assigned)
            upsertMessage(mwr.messageDto)

            // normalize readers to ensure messageId matches message.id
            val readers = mwr.readers.map {
                it.copy(messageId = mwr.messageDto.id!!)
            }
            if (readers.isNotEmpty()) insertReaders(readers)
        }
    }

    @Transaction
    suspend fun upsertMessageWithReaders(message: MessageWithReadersDto) {
        upsertMessage(message.messageDto)

        // normalize readers to ensure messageId matches message.id
        val readers = message.readers.map { it.copy(messageId = message.messageDto.id!!) }
        if (readers.isNotEmpty()) insertReaders(readers)
    }

    @Transaction
    fun getAllMessagesWithReaders(): Flow<List<MessageWithReadersDto>>{
        return database.messageDao().getAllMessagesWithReaders()
    }

    @Transaction
    suspend fun getmessagechangeid(): List<IdChangeDate>{
        return database.messageDao().getMessageIdsWithChangeDates()
    }

    suspend fun insertReader(reader: MessageReaderDto) {
        database.messagereaderDao().upsertReader(reader)
    }


    suspend fun insertReaders(readers: List<MessageReaderDto>){
        database.messagereaderDao().upsertReaders(readers)
    }

    suspend fun deleteReadersForMessage(messageId: String){
        database.messagereaderDao().deleteReadersForMessage(messageId)
    }

    @Transaction
    suspend fun setAllChatMessagesRead(chatid: String, gruppe: Boolean, timestamp: String) {
        //networkUtils.setAllChatMessagesRead(chatid, gruppe, timestamp)TODO Messages

        database.messageDao().getMessagesByUserId(chatid, gruppe).collect { messagelist ->
            for (message in messagelist){
                if (!message.isReadbyMe()){
                    database.messagereaderDao().upsertReader(MessageReaderDto(messageId = message.messageDto.id!!, readerID = SessionCache.getOwnIdValue()!!, readDate = timestamp))
                }
            }
        }
    }

}
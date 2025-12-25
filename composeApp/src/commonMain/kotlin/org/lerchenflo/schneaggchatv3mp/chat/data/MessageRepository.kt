package org.lerchenflo.schneaggchatv3mp.chat.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageReaderDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.relations.MessageWithReadersDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.chat.domain.toDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.toMessage
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils

class MessageRepository(
    private val database: AppDatabase,
) {

    suspend fun upsertMessage(message: Message){
        val messageWithReadersDto = message.toDto()
        upsertMessageWithReaders(messageWithReadersDto)
    }

    suspend fun deleteMessage(id: String){
        deleteReadersForMessage(id)
        deleteMessageDto(id)
    }


    fun getMessagesByUserIdFlow(userId: String, gruppe: Boolean): Flow<List<Message>> {
        return database.messageDao().getMessagesByUserIdFlow(userId, gruppe).map { messages ->
            messages.map { it.toMessage() }
        }
    }

    suspend fun getUnsentMessages() : List<Message> {
        return database.messageDao().getUnsentMessages().map {
            it.toMessage()
        }
    }


    /**
     * Update a message in the database
     */
    @Transaction
    private suspend fun upsertMessageWithReaders(message: MessageWithReadersDto) {
        //Upsert the message dto without readers
        val upserted = database.messageDao().upsertMessageDto(message.messageDto)

        //if the readers are not empty, remove all existing an re - insert
        if (message.readers.isNotEmpty() && upserted.id != null){
            deleteReadersForMessage(upserted.id!!)

            val readers = message.readers.map { it.copy(messageId = upserted.id!!) }
            insertReaders(readers)
        }
    }

    /**
     * Delete all readers of a message
     */
    private suspend fun deleteReadersForMessage(messageId: String){
        database.messagereaderDao().deleteReadersForMessage(messageId)
    }

    /**
     * Insert new readers for a message
     */
    private suspend fun insertReaders(readers: List<MessageReaderDto>){
        database.messagereaderDao().upsertReaders(readers)
    }



    @Transaction
    fun getAllMessages(): Flow<List<Message>>{
        return database.messageDao().getAllMessagesWithReadersFlow().map { messageWithReadersDtos ->
            messageWithReadersDtos.map {
                it.toMessage()
            }
        }
    }

    @Transaction
    suspend fun getmessagechangeid(): List<IdChangeDate>{
        return database.messageDao().getMessageIdsWithChangeDates()
    }


    private suspend fun deleteMessageDto(messageId: String) {
        database.messageDao().deleteMessageDtoById(messageId)
    }



    @Transaction
    suspend fun setAllChatMessagesRead(chatid: String, gruppe: Boolean, timestamp: String) {
        val ownId = SessionCache.getOwnIdValue()
        if (ownId != null) {
            // Use efficient bulk updates instead of loading individual messages
            database.messageDao().markAllChatMessagesRead(chatid, gruppe, timestamp)
            database.messageDao().addMessageReadersForChat(chatid, gruppe, ownId, timestamp)
        }
    }

}
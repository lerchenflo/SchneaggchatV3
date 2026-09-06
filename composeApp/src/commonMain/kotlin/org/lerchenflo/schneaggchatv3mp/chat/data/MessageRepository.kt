package org.lerchenflo.schneaggchatv3mp.chat.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.ChatAggregateDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageReaderDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.relations.MessageWithReadersDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.toDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.toMessage
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate

/** Keeps `IN (:ids)` queries well under SQLite's bound-parameter limit. */
private const val SQL_ID_CHUNK_SIZE = 500

class MessageRepository(
    private val database: AppDatabase,
) {

    suspend fun upsertMessage(message: Message){
        val messageWithReadersDto = message.toDto()
        upsertMessageWithReaders(messageWithReadersDto)
    }

    /**
     * Upsert a whole batch of messages in one transaction. Preferred over looping [upsertMessage]
     * whenever more than one message lands at once (sync pages) - one transaction means one
     * invalidation instead of one per message.
     */
    suspend fun upsertMessages(messages: List<Message>) {
        messages.chunked(SQL_ID_CHUNK_SIZE).forEach { chunk ->
            database.messageDao().upsertMessagesWithReaders(chunk.map { it.toDto() })
        }
    }

    /** Batch counterpart of [deleteMessage], readers included, in one transaction per chunk. */
    suspend fun deleteMessages(ids: List<String>) {
        ids.chunked(SQL_ID_CHUNK_SIZE).forEach { chunk ->
            database.messageDao().deleteMessagesByIds(chunk)
        }
    }

    suspend fun getMessageDtosByIds(ids: List<String>): List<MessageDto> {
        return ids.chunked(SQL_ID_CHUNK_SIZE).flatMap { chunk ->
            database.messageDao().getMessageDtosByIds(chunk)
        }
    }

    suspend fun deleteMessage(id: String){
        deleteReadersForMessage(id)
        deleteMessageDto(id)
    }

    suspend fun deleteMessage(lokalPk: Long) {
        database.messageDao().deleteMessageDtoByPk(lokalPk)
    }

    suspend fun getLastSyncedMessageVersion(): Long {
        return database.messageDao().getLastSyncedMessageVersion()
    }

    suspend fun deleteMessagesForGroup(groupId: String) {
        database.messageReaderDao().deleteReadersForGroupMessages(groupId)
        database.messageDao().deleteMessagesForGroup(groupId)
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

    suspend fun getImageMessages(): List<Message> {
        return database.messageDao().getImageMessages().map {
            it.toMessage()
        }
    }

    suspend fun getAudioMessages(): List<Message> {
        return database.messageDao().getAudioMessages().map {
            it.toMessage()
        }
    }

    suspend fun getMessageById(id: String) : Message? {
        return database.messageDao().getMessageById(id)?.toMessage()
    }

    suspend fun getMessageById(id: Long) : Message? {
        return database.messageDao().getMessageById(id)?.toMessage()
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
        database.messageReaderDao().deleteReadersForMessage(messageId)
    }

    /**
     * Insert new readers for a message
     */
    private suspend fun insertReaders(readers: List<MessageReaderDto>){
        database.messageReaderDao().upsertReaders(readers)
    }

    suspend fun updatePictureUrl(messageId: String, newUrl: String) {
        val dbMessage = database.messageDao().getMessageById(messageId)
        if (dbMessage != null) {
            database.messageDao().upsertMessageDto(
                dbMessage.messageDto.copy(
                    pictureUrl = newUrl
                )
            )
        }
    }

    suspend fun updateAudioPath(messageId: String, newPath: String) {
        val dbMessage = database.messageDao().getMessageById(messageId)
        if (dbMessage != null) {
            database.messageDao().upsertMessageDto(
                dbMessage.messageDto.copy(
                    audioPath = newPath
                )
            )
        }
    }

    /** Chats with at least one unread message, counted in SQL for the nav bar badge. */
    fun getUnreadChatCountFlow(): Flow<Int> = database.messageDao().getUnreadChatCountFlow()

    @Transaction
    fun getAllMessages(): Flow<List<Message>>{
        return database.messageDao().getAllMessagesWithReadersFlow().map { messageWithReadersDtos ->
            messageWithReadersDtos.map {
                it.toMessage()
            }
        }
    }

    /** Per-chat unread/unsent counters, aggregated in SQL. */
    fun getChatAggregatesFlow(ownId: String): Flow<List<ChatAggregateDto>> =
        database.messageDao().getChatAggregatesFlow(ownId)

    /** The newest message of every chat, one row per chat, without readers. */
    fun getLastMessagePerChatFlow(ownId: String): Flow<List<Message>> =
        database.messageDao().getLastMessagePerChatFlow(ownId).map { dtos ->
            dtos.map { it.toMessage() }
        }

    @Transaction
    suspend fun getmessagechangeid(): List<IdChangeDate>{
        return database.messageDao().getMessageIdsWithChangeDates()
    }


    private suspend fun deleteMessageDto(messageId: String) {
        database.messageDao().deleteMessageDtoById(messageId)
    }

    private suspend fun deleteMessageDto(localpk: Long) {
        database.messageDao().deleteMessageDtoByPk(localpk)
    }




    @Transaction
    suspend fun setAllChatMessagesRead(ownId: String, chatid: String, gruppe: Boolean, timestamp: String) {

        // Use efficient bulk updates instead of loading individual messages
        database.messageDao().markAllChatMessagesRead(chatid, gruppe, timestamp)
        database.messageDao().addMessageReadersForChat(chatid, gruppe, ownId, timestamp)
    }

}
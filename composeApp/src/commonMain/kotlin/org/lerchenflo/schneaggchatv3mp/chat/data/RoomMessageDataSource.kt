package org.lerchenflo.schneaggchatv3mp.chat.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.ChatAggregateDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageReaderDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.relations.MessageWithReadersDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.toDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.toMessage
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate

/** Keeps `IN (:ids)` queries well under SQLite's bound-parameter limit. */
private const val SQL_ID_CHUNK_SIZE = 500

class RoomMessageDataSource(
    private val database: AppDatabase,
) : MessageLocalDataSource {

    override suspend fun upsertMessage(message: Message){
        val messageWithReadersDto = message.toDto()
        upsertMessageWithReaders(messageWithReadersDto)
    }

    override suspend fun upsertMessages(messages: List<Message>) {
        messages.chunked(SQL_ID_CHUNK_SIZE).forEach { chunk ->
            database.messageDao().upsertMessagesWithReaders(chunk.map { it.toDto() })
        }
    }

    override suspend fun deleteMessages(ids: List<String>) {
        ids.chunked(SQL_ID_CHUNK_SIZE).forEach { chunk ->
            database.messageDao().deleteMessagesByIds(chunk)
        }
    }

    override suspend fun getMessageDtosByIds(ids: List<String>): List<MessageDto> {
        return ids.chunked(SQL_ID_CHUNK_SIZE).flatMap { chunk ->
            database.messageDao().getMessageDtosByIds(chunk)
        }
    }

    override suspend fun getMessageDtosByClientMessageIds(clientMessageIds: List<String>): List<MessageDto> {
        return clientMessageIds.chunked(SQL_ID_CHUNK_SIZE).flatMap { chunk ->
            database.messageDao().getMessageDtosByClientMessageIds(chunk)
        }
    }

    override suspend fun getMessageByClientMessageId(clientMessageId: String): Message? {
        return database.messageDao().getMessageDtosByClientMessageIds(listOf(clientMessageId))
            .firstOrNull()
            ?.toMessage()
    }

    override suspend fun deleteMessage(id: String){
        deleteReadersForMessage(id)
        deleteMessageDto(id)
    }

    override suspend fun deleteMessage(lokalPk: Long) {
        database.messageDao().deleteMessageDtoByPk(lokalPk)
    }

    override suspend fun getLastSyncedMessageVersion(): Long {
        return database.messageDao().getLastSyncedMessageVersion()
    }

    override suspend fun deleteMessagesForGroup(groupId: String) {
        database.messageReaderDao().deleteReadersForGroupMessages(groupId)
        database.messageDao().deleteMessagesForGroup(groupId)
    }


    override fun getMessagesByUserIdFlow(userId: String, gruppe: Boolean, limit: Int): Flow<List<Message>> {
        return database.messageDao().getMessagesByUserIdFlow(userId, gruppe, limit).map { messages ->
            messages.map { it.toMessage() }
        }
    }

    override suspend fun getMessageCountSince(chatId: String, gruppe: Boolean, sinceMillis: Long): Int =
        database.messageDao().getMessageCountSince(chatId, gruppe, sinceMillis)

    override suspend fun getOldestUnreadSendDate(chatId: String, gruppe: Boolean): Long? =
        database.messageDao().getOldestUnreadSendDate(chatId, gruppe)

    override fun getImageMessagesForChatFlow(chatId: String, gruppe: Boolean): Flow<List<Message>> {
        return database.messageDao().getImageMessagesForChatFlow(chatId, gruppe).map { messages ->
            messages.map { it.toMessage() }
        }
    }

    override fun getMessagesOfTypeForChatFlow(chatId: String, gruppe: Boolean, type: MessageType): Flow<List<Message>> {
        return database.messageDao().getMessagesOfTypeForChatFlow(chatId, gruppe, type.name).map { messages ->
            messages.map { it.toMessage() }
        }
    }

    override fun getMessageCountForChatFlow(chatId: String, gruppe: Boolean): Flow<Int> =
        database.messageDao().getMessageCountForChatFlow(chatId, gruppe)

    override fun getLinkCandidateMessagesForChatFlow(chatId: String, gruppe: Boolean): Flow<List<Message>> {
        return database.messageDao().getLinkCandidateMessagesForChatFlow(chatId, gruppe).map { messages ->
            messages.map { it.toMessage() }
        }
    }

    override suspend fun getUnsentMessages() : List<Message> {
        return database.messageDao().getUnsentMessages().map {
            it.toMessage()
        }
    }

    override suspend fun getImageMessages(): List<Message> {
        return database.messageDao().getImageMessages().map {
            it.toMessage()
        }
    }

    override suspend fun getAudioMessages(): List<Message> {
        return database.messageDao().getAudioMessages().map {
            it.toMessage()
        }
    }

    override suspend fun getMessageById(id: String) : Message? {
        return database.messageDao().getMessageById(id)?.toMessage()
    }

    override suspend fun getMessageById(id: Long) : Message? {
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

    override suspend fun updatePictureUrl(messageId: String, newUrl: String) {
        val dbMessage = database.messageDao().getMessageById(messageId)
        if (dbMessage != null) {
            database.messageDao().upsertMessageDto(
                dbMessage.messageDto.copy(
                    pictureUrl = newUrl
                )
            )
        }
    }

    override suspend fun updateAudioPath(messageId: String, newPath: String) {
        val dbMessage = database.messageDao().getMessageById(messageId)
        if (dbMessage != null) {
            database.messageDao().upsertMessageDto(
                dbMessage.messageDto.copy(
                    audioPath = newPath
                )
            )
        }
    }

    override fun getUnreadChatCountFlow(): Flow<Int> = database.messageDao().getUnreadChatCountFlow()

    @Transaction
    override fun getAllMessages(): Flow<List<Message>>{
        return database.messageDao().getAllMessagesWithReadersFlow().map { messageWithReadersDtos ->
            messageWithReadersDtos.map {
                it.toMessage()
            }
        }
    }

    override fun getChatAggregatesFlow(ownId: String): Flow<List<ChatAggregateDto>> =
        database.messageDao().getChatAggregatesFlow(ownId)

    override fun getLastMessagePerChatFlow(ownId: String): Flow<List<Message>> =
        database.messageDao().getLastMessagePerChatFlow(ownId).map { dtos ->
            dtos.map { it.toMessage() }
        }

    @Transaction
    override suspend fun getmessagechangeid(): List<IdChangeDate>{
        return database.messageDao().getMessageIdsWithChangeDates()
    }


    private suspend fun deleteMessageDto(messageId: String) {
        database.messageDao().deleteMessageDtoById(messageId)
    }

    private suspend fun deleteMessageDto(localpk: Long) {
        database.messageDao().deleteMessageDtoByPk(localpk)
    }




}
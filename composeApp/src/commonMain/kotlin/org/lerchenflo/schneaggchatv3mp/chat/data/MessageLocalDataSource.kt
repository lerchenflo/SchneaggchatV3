package org.lerchenflo.schneaggchatv3mp.chat.data

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.ChatAggregate
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.MessageEntity
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate

/**
 * Local (Room) storage for messages and their readers.
 */
interface MessageLocalDataSource {

    suspend fun upsertMessage(message: Message)

    /**
     * Upsert a whole batch of messages in one transaction. Preferred over looping [upsertMessage]
     * whenever more than one message lands at once (sync pages) - one transaction means one
     * invalidation instead of one per message.
     */
    suspend fun upsertMessages(messages: List<Message>)

    /** Batch counterpart of [deleteMessage], readers included, in one transaction per chunk. */
    suspend fun deleteMessages(ids: List<String>)

    suspend fun getMessageEntitiesByIds(ids: List<String>): List<MessageEntity>

    suspend fun getMessageEntitiesByClientMessageIds(clientMessageIds: List<String>): List<MessageEntity>

    /** Single-item convenience for the socket handler - see [getMessageEntitiesByClientMessageIds]. */
    suspend fun getMessageByClientMessageId(clientMessageId: String): Message?

    suspend fun deleteMessage(id: String)

    suspend fun deleteMessage(lokalPk: Long)

    suspend fun getLastSyncedMessageVersion(): Long

    suspend fun deleteMessagesForGroup(groupId: String)

    /** The newest [limit] messages of one chat, newest first, readers included. */
    fun getMessagesByUserIdFlow(userId: String, gruppe: Boolean, limit: Int): Flow<List<Message>>

    /** Number of messages in one chat sent at or after [sinceMillis]. */
    suspend fun getMessageCountSince(chatId: String, gruppe: Boolean, sinceMillis: Long): Int

    /** Send date (epoch millis) of the oldest unread message in one chat, null when all are read. */
    suspend fun getOldestUnreadSendDate(chatId: String, gruppe: Boolean): Long?

    /** Every image shared in one chat, newest first, without readers. */
    fun getImageMessagesForChatFlow(chatId: String, gruppe: Boolean): Flow<List<Message>>

    /** Messages of one type in one chat, newest first, without readers. */
    fun getMessagesOfTypeForChatFlow(chatId: String, gruppe: Boolean, type: MessageType): Flow<List<Message>>

    /** Number of messages in one chat, all types. */
    fun getMessageCountForChatFlow(chatId: String, gruppe: Boolean): Flow<Int>

    /** Messages of one chat that might carry a url, newest first, without readers. */
    fun getLinkCandidateMessagesForChatFlow(chatId: String, gruppe: Boolean): Flow<List<Message>>

    suspend fun getUnsentMessages(): List<Message>

    suspend fun getImageMessages(): List<Message>

    suspend fun getAudioMessages(): List<Message>

    suspend fun getMessageById(id: String): Message?

    suspend fun getMessageById(id: Long): Message?

    suspend fun updatePictureUrl(messageId: String, newUrl: String)

    suspend fun updateAudioPath(messageId: String, newPath: String)

    /** Chats with at least one unread message, counted in SQL for the nav bar badge. */
    fun getUnreadChatCountFlow(): Flow<Int>

    fun getAllMessages(): Flow<List<Message>>

    /** Per-chat unread/unsent counters, aggregated in SQL. */
    fun getChatAggregatesFlow(ownId: String): Flow<List<ChatAggregate>>

    /** The newest message of every chat, one row per chat, without readers. */
    fun getLastMessagePerChatFlow(ownId: String): Flow<List<Message>>

    suspend fun getmessagechangeid(): List<IdChangeDate>
}

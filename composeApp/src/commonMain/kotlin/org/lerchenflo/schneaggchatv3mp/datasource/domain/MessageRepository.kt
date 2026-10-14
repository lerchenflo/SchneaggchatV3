package org.lerchenflo.schneaggchatv3mp.datasource.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.Message
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.PollVote

/**
 * Messages: reads come from the local database, edits, deletes, reactions and poll votes go to
 * the server first. Errors of the write calls are sent to the [ErrorChannel].
 */
interface MessageRepository {

    // ─── Reads (local database) ───────────────────────────────────────────────

    /** The newest [limit] messages of one chat, newest first, readers included. */
    fun getMessagesByUserIdFlow(userId: String, gruppe: Boolean, limit: Int): Flow<List<Message>>

    /** Number of messages in one chat sent at or after [sinceMillis]. */
    suspend fun getMessageCountSince(chatId: String, gruppe: Boolean, sinceMillis: Long): Int

    /** Send date (epoch millis) of the oldest unread message in one chat, null when all are read. */
    suspend fun getOldestUnreadSendDate(chatId: String, gruppe: Boolean): Long?

    suspend fun getMessageById(id: String): Message?

    /** The stored messages with these server ids, without readers. Unknown ids are skipped. */
    suspend fun getMessagesByIds(ids: List<String>): List<Message>

    /** Every image shared in one chat, newest first, without readers. */
    fun getImageMessagesForChatFlow(chatId: String, gruppe: Boolean): Flow<List<Message>>

    /** Messages of one type in one chat, newest first, without readers. */
    fun getMessagesOfTypeForChatFlow(chatId: String, gruppe: Boolean, type: MessageType): Flow<List<Message>>

    /** Number of messages in one chat, all types. */
    fun getMessageCountForChatFlow(chatId: String, gruppe: Boolean): Flow<Int>

    /** Messages of one chat that might carry a url, newest first, without readers. */
    fun getLinkCandidateMessagesForChatFlow(chatId: String, gruppe: Boolean): Flow<List<Message>>

    // ─── Media ────────────────────────────────────────────────────────────────

    /** Downloads the pictures of these image messages and stores their local paths. */
    suspend fun getPicturesForMessageIds(ids: List<String>)

    /** Downloads the voice recordings of these audio messages and stores their file names. */
    suspend fun getAudiosForMessageIds(ids: List<String>)

    /** Downloads every stored image and audio message's file that is missing locally. */
    suspend fun fetchMissingMessageMedia()

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    /** Deletes the stored messages of these groups (e.g. groups removed during a sync). */
    suspend fun deleteMessagesForGroups(groupIds: List<String>)

    // ─── Writes ───────────────────────────────────────────────────────────────

    /** Edits a message. A message that was never sent is only changed locally. */
    suspend fun editMessage(message: Message, newContent: String)

    /** Deletes a message that was never sent, by its local primary key (local only). */
    suspend fun deleteLocalMessage(localpk: Long)

    /** Deletes a sent message on the server, then locally. */
    suspend fun deleteMessage(messageId: String)

    /** Reacts to a message with [content]; the stored reactions are replaced by the server's list. */
    suspend fun reactToMessage(messageId: String, content: String)

    /** Votes in a poll, or adds a custom option ([PollVote.id] null). */
    suspend fun votePoll(ownId: String, pollVote: PollVote)

    suspend fun deletePollOption(ownId: String, messageId: String, optionId: String)
}

package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.DecodedNotification
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel

/**
 * Messages: reads come from the local database, edits, deletes, reactions and poll votes go to
 * the server first. Sends are stored locally first and queued while they can't be delivered.
 * Errors of the write calls are sent to the [ErrorChannel].
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

    /**
     * Edits a message. A message that was never sent is only changed locally. A sent message
     * needs the server: if the edit fails (e.g. offline) the message stays unchanged and the
     * error is shown. @return true when the edit was saved
     */
    suspend fun editMessage(message: Message, newContent: String): Boolean

    /** Deletes a message that was never sent, by its local primary key (local only). */
    suspend fun deleteLocalMessage(localpk: Long)

    /** Deletes a sent message on the server, then locally. */
    suspend fun deleteMessage(messageId: String)

    /** Reacts to a message with [content]; the stored reactions are replaced by the server's list. */
    suspend fun reactToMessage(messageId: String, content: String)

    /** Votes in a poll, or adds a custom option ([PollVote.id] null). */
    suspend fun votePoll(ownId: String, pollVote: PollVote)

    suspend fun deletePollOption(ownId: String, messageId: String, optionId: String)

    // ─── Send / offline queue / sync ──────────────────────────────────────────

    /**
     * Sends a message: stores it as an unsent row, sends it to the server, then writes the
     * server's copy onto that row. A failed send stays queued for [sendOfflineMessages], except
     * when the server rejects it (bad request, conflict), which deletes the row.
     *
     * Runs in the app-wide scope: the send finishes even when the caller (e.g. a closed chat
     * screen) is cancelled while it is in flight.
     *
     * @param localpk Local pk, only pass if already in db
     * @param clientMessageId Send idempotency key of an already-queued message. REQUIRED when
     * [localpk] is non-zero (a resend). Never pass this for a brand-new message.
     */
    suspend fun sendMessage(
        ownId: String,
        messageId: String?,
        empfaenger: String,
        gruppe: Boolean,
        content: MessageContent,
        answerid: String?,
        localpk: Long = 0,
        clientMessageId: String? = null,
    )

    /**
     * [sendMessage] for a recorded voice message: reads the recording at [recordingPath] off the
     * main thread, inside the app-wide scope, so a closed chat screen can't drop it either.
     */
    suspend fun sendVoiceMessage(
        ownId: String,
        empfaenger: String,
        gruppe: Boolean,
        recordingPath: String,
        answerid: String?,
    )

    /**
     * Resends the unsent messages one by one, oldest first, and stops at the first one that is
     * still unsent after its attempt. Waits for a send in flight before picking the next row.
     */
    suspend fun sendOfflineMessages(ownId: String)

    /**
     * Pulls every message change since the newest stored version from the server.
     *
     * @param fetchMedia whether to download the images/audios of the synced messages right away.
     * The full data sync passes false and downloads them in its own media step.
     */
    suspend fun syncMessages(fetchMedia: Boolean = true)

    // ─── Push apply / mark read ───────────────────────────────────────────────

    /**
     * Stores a message delivered by push (FCM/APNs) as a provisional row, so an open chat shows
     * it before the next sync; a reaction push updates the stored message. Never overwrites an
     * existing row and never advances the sync cursor - follow up with [syncMessages].
     */
    suspend fun applyPushMessage(decoded: DecodedNotification.Message)

    /**
     * Marks a whole chat read on the server, then pulls the result back through [syncMessages].
     * Nothing is written locally before that.
     *
     * Runs in the app-wide scope: the request and the sync finish even when the caller is
     * cancelled while they are in flight.
     *
     * @return true when the server accepted the read. False (offline, error) means nothing
     * changed anywhere and the caller is expected to try again later.
     */
    suspend fun setAllChatMessagesRead(chatid: String, gruppe: Boolean, timestamp: String): Boolean

    /**
     * Fire-and-forget [setAllChatMessagesRead] for a chat screen that may close right away.
     * Returns at once; the request runs in the app-wide scope, including the wait behind a
     * request of the same [guard] that is still in flight.
     *
     * Requests of one [guard] run one after another. Each one calls [readNewestMessageId] once it
     * holds the lock and is skipped when [MarkReadGuard.lastMarkedReadMessageId] already covers
     * it; that id only advances when the server accepted the read (reset to null otherwise).
     */
    fun markChatReadInBackground(
        chatid: String,
        gruppe: Boolean,
        guard: MarkReadGuard,
        readNewestMessageId: () -> String?,
    )
}

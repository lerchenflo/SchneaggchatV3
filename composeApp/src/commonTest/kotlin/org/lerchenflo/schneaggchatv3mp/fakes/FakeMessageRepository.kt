package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.MarkReadGuard
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageContent
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVote
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.DecodedNotification

/**
 * In-memory [MessageRepository]. The reads serve [messages]; every write is only recorded (it
 * does not change [messages]), so a test sets up exactly the list it wants to see.
 *
 * The suspending sync/send entry points run an `on…` hook, which a test can replace to make the
 * call suspend (e.g. on a `CompletableDeferred`) or fail.
 */
class FakeMessageRepository : MessageRepository {

    data class SendCall(
        val ownId: String,
        val messageId: String?,
        val receiverId: String,
        val isGroup: Boolean,
        val content: MessageContent,
        val answerId: String?,
        val localPk: Long,
        val clientMessageId: String?,
    )

    data class SendVoiceCall(
        val ownId: String,
        val empfaenger: String,
        val gruppe: Boolean,
        val recordingPath: String,
        val answerid: String?,
    )
    data class EditCall(val message: Message, val newContent: String)
    data class ReactCall(val messageId: String, val content: String)
    data class VotePollCall(val ownId: String, val pollVote: PollVote)
    data class DeletePollOptionCall(val ownId: String, val messageId: String, val optionId: String)
    data class MarkReadCall(val chatId: String, val isGroup: Boolean, val timestamp: String)

    /** [newestMessageId] is what the caller's `readNewestMessageId` returned at call time. */
    data class MarkReadInBackgroundCall(
        val chatId: String,
        val isGroup: Boolean,
        val guard: MarkReadGuard,
        val newestMessageId: String?,
    )

    /** What the reads return. Messages of one chat are found like the DAO does (see [isInChat]). */
    val messages = MutableStateFlow<List<Message>>(emptyList())

    val sendCalls = CallLog<SendCall>()
    val sendVoiceCalls = CallLog<SendVoiceCall>()
    val sendOfflineCalls = CallLog<String>()
    val syncCalls = CallLog<Boolean>()
    val editCalls = CallLog<EditCall>()
    val deleteLocalCalls = CallLog<Long>()
    val deleteCalls = CallLog<String>()
    val reactCalls = CallLog<ReactCall>()
    val votePollCalls = CallLog<VotePollCall>()
    val deletePollOptionCalls = CallLog<DeletePollOptionCall>()
    val pushMessages = CallLog<DecodedNotification.Message>()
    val markReadCalls = CallLog<MarkReadCall>()
    val markReadInBackgroundCalls = CallLog<MarkReadInBackgroundCall>()
    val deletedGroupMessages = CallLog<List<String>>()
    val pictureRequests = CallLog<List<String>>()
    val audioRequests = CallLog<List<String>>()
    val fetchMissingMediaCalls = CallLog<Unit>()

    var onSendMessage: suspend (SendCall) -> Unit = {}
    var onSendOfflineMessages: suspend (ownId: String) -> Unit = {}
    var onSyncMessages: suspend (fetchMedia: Boolean) -> Unit = {}
    var onFetchMissingMessageMedia: suspend () -> Unit = {}

    /** What [setAllChatMessagesRead] answers. */
    var markReadAccepted: Boolean = true

    // ─── Reads ────────────────────────────────────────────────────────────────

    override fun getMessagesByUserIdFlow(userId: String, gruppe: Boolean, limit: Int): Flow<List<Message>> =
        messages.map { list -> list.inChat(userId, gruppe).take(limit) }

    override suspend fun getMessageCountSince(chatId: String, gruppe: Boolean, sinceMillis: Long): Int =
        messages.value.inChat(chatId, gruppe).count { it.getSendDateAsLong() >= sinceMillis }

    override suspend fun getOldestUnreadSendDate(chatId: String, gruppe: Boolean): Long? =
        messages.value.inChat(chatId, gruppe)
            .filter { !it.myMessage && !it.readByMe }
            .minOfOrNull { it.getSendDateAsLong() }

    override suspend fun getMessageById(id: String): Message? = messages.value.firstOrNull { it.id == id }

    override suspend fun getMessagesByIds(ids: List<String>): List<Message> =
        messages.value.filter { it.id in ids }

    override fun getImageMessagesForChatFlow(chatId: String, gruppe: Boolean): Flow<List<Message>> =
        getMessagesOfTypeForChatFlow(chatId, gruppe, MessageType.IMAGE)

    override fun getMessagesOfTypeForChatFlow(chatId: String, gruppe: Boolean, type: MessageType): Flow<List<Message>> =
        messages.map { list -> list.inChat(chatId, gruppe).filter { it.msgType == type } }

    override fun getMessageCountForChatFlow(chatId: String, gruppe: Boolean): Flow<Int> =
        messages.map { list -> list.inChat(chatId, gruppe).size }

    override fun getLinkCandidateMessagesForChatFlow(chatId: String, gruppe: Boolean): Flow<List<Message>> =
        messages.map { list -> list.inChat(chatId, gruppe).filter { it.content.contains("http") } }

    // ─── Media / cleanup ──────────────────────────────────────────────────────

    override suspend fun getPicturesForMessageIds(ids: List<String>) = pictureRequests.record(ids)

    override suspend fun getAudiosForMessageIds(ids: List<String>) = audioRequests.record(ids)

    override suspend fun fetchMissingMessageMedia() {
        fetchMissingMediaCalls.record(Unit)
        onFetchMissingMessageMedia()
    }

    override suspend fun deleteMessagesForGroups(groupIds: List<String>) = deletedGroupMessages.record(groupIds)

    // ─── Writes ───────────────────────────────────────────────────────────────

    /** What [editMessage] answers - false like a failed (e.g. offline) edit. */
    var editSucceeds = true

    override suspend fun editMessage(message: Message, newContent: String): Boolean {
        editCalls.record(EditCall(message, newContent))
        return editSucceeds
    }

    override suspend fun deleteLocalMessage(localpk: Long) = deleteLocalCalls.record(localpk)

    override suspend fun deleteMessage(messageId: String) = deleteCalls.record(messageId)

    override suspend fun reactToMessage(messageId: String, content: String) =
        reactCalls.record(ReactCall(messageId, content))

    override suspend fun votePoll(ownId: String, pollVote: PollVote) =
        votePollCalls.record(VotePollCall(ownId, pollVote))

    override suspend fun deletePollOption(ownId: String, messageId: String, optionId: String) =
        deletePollOptionCalls.record(DeletePollOptionCall(ownId, messageId, optionId))

    // ─── Send / offline queue / sync ──────────────────────────────────────────

    override suspend fun sendMessage(
        ownId: String,
        messageId: String?,
        empfaenger: String,
        gruppe: Boolean,
        content: MessageContent,
        answerid: String?,
        localpk: Long,
        clientMessageId: String?,
    ) {
        val call = SendCall(ownId, messageId, empfaenger, gruppe, content, answerid, localpk, clientMessageId)
        sendCalls.record(call)
        onSendMessage(call)
    }

    override suspend fun sendVoiceMessage(
        ownId: String,
        empfaenger: String,
        gruppe: Boolean,
        recordingPath: String,
        answerid: String?,
    ) {
        sendVoiceCalls.record(SendVoiceCall(ownId, empfaenger, gruppe, recordingPath, answerid))
    }

    override suspend fun sendOfflineMessages(ownId: String) {
        sendOfflineCalls.record(ownId)
        onSendOfflineMessages(ownId)
    }

    override suspend fun syncMessages(fetchMedia: Boolean) {
        syncCalls.record(fetchMedia)
        onSyncMessages(fetchMedia)
    }

    // ─── Push apply / mark read ───────────────────────────────────────────────

    override suspend fun applyPushMessage(decoded: DecodedNotification.Message) = pushMessages.record(decoded)

    override suspend fun setAllChatMessagesRead(chatid: String, gruppe: Boolean, timestamp: String): Boolean {
        markReadCalls.record(MarkReadCall(chatid, gruppe, timestamp))
        return markReadAccepted
    }

    override fun markChatReadInBackground(
        chatid: String,
        gruppe: Boolean,
        guard: MarkReadGuard,
        readNewestMessageId: () -> String?,
    ) {
        markReadInBackgroundCalls.record(MarkReadInBackgroundCall(chatid, gruppe, guard, readNewestMessageId()))
    }

    /**
     * The messages of one chat, newest first (the order `MessageDao.getMessagesByUserIdFlow` uses):
     * a group's messages, or the direct messages from and to [chatId].
     */
    private fun List<Message>.inChat(chatId: String, isGroup: Boolean): List<Message> =
        filter { it.isInChat(chatId, isGroup) }.sortedByDescending { it.getSendDateAsLong() }

    private fun Message.isInChat(chatId: String, isGroup: Boolean): Boolean =
        groupMessage == isGroup && (receiverId == chatId || (!isGroup && senderId == chatId))
}

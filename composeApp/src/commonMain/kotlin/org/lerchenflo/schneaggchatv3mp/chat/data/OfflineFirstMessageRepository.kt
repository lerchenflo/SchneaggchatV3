package org.lerchenflo.schneaggchatv3mp.chat.data

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.PICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.VOICEMSG_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.toMessage
import org.lerchenflo.schneaggchatv3mp.datasource.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.datasource.domain.ErrorEvent
import org.lerchenflo.schneaggchatv3mp.datasource.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.domain.MessageRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.Message
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.PollVote
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.Reaction
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.isNewerThan
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.MessageResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.PollOptionDeleteRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toPollMessage
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toPollVoteRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.utilities.AudioManager
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager

/**
 * [MessageRepository] on top of the local [MessageLocalDataSource] and the server via [NetworkUtils].
 * The media and write code is moved verbatim from AppRepository (getPicturesForMessageIds,
 * getAudiosForMessageIds, the image-message part of getMissingPics + getMissingAudios, editMessage,
 * deleteLocalMessage, deleteMessage, reactToMessage, votePoll, deletePollOption).
 */
class OfflineFirstMessageRepository(
    private val messageLocalDataSource: MessageLocalDataSource,
    private val networkUtils: NetworkUtils,
    private val pictureManager: PictureManager,
    private val audioManager: AudioManager,
    private val loggingRepository: Logger,
    private val errorChannel: ErrorChannel,
) : MessageRepository {

    // ─── Reads ────────────────────────────────────────────────────────────────

    override fun getMessagesByUserIdFlow(userId: String, gruppe: Boolean, limit: Int): Flow<List<Message>> =
        messageLocalDataSource.getMessagesByUserIdFlow(userId, gruppe, limit)

    override suspend fun getMessageCountSince(chatId: String, gruppe: Boolean, sinceMillis: Long): Int =
        messageLocalDataSource.getMessageCountSince(chatId, gruppe, sinceMillis)

    override suspend fun getOldestUnreadSendDate(chatId: String, gruppe: Boolean): Long? =
        messageLocalDataSource.getOldestUnreadSendDate(chatId, gruppe)

    override suspend fun getMessageById(id: String): Message? =
        messageLocalDataSource.getMessageById(id)

    override suspend fun getMessagesByIds(ids: List<String>): List<Message> =
        messageLocalDataSource.getMessageDtosByIds(ids).map { it.toMessage() }

    override fun getImageMessagesForChatFlow(chatId: String, gruppe: Boolean): Flow<List<Message>> =
        messageLocalDataSource.getImageMessagesForChatFlow(chatId, gruppe)

    override fun getMessagesOfTypeForChatFlow(chatId: String, gruppe: Boolean, type: MessageType): Flow<List<Message>> =
        messageLocalDataSource.getMessagesOfTypeForChatFlow(chatId, gruppe, type)

    override fun getMessageCountForChatFlow(chatId: String, gruppe: Boolean): Flow<Int> =
        messageLocalDataSource.getMessageCountForChatFlow(chatId, gruppe)

    override fun getLinkCandidateMessagesForChatFlow(chatId: String, gruppe: Boolean): Flow<List<Message>> =
        messageLocalDataSource.getLinkCandidateMessagesForChatFlow(chatId, gruppe)

    // ─── Media ────────────────────────────────────────────────────────────────

    override suspend fun getPicturesForMessageIds(ids: List<String>){
        ids.forEach { messageId ->
            val savefilename = messageId + PICTURE_FILE_NAME

            when (val picture = networkUtils.getImageForImageMessage(messageId).trackConnectivity()) {
                is NetworkResult.Error<*> -> {loggingRepository.logWarning("Picture error for messageid $messageId")}
                is NetworkResult.Success<ByteArray> -> {
                    val filepath = pictureManager.savePictureToStorage(picture.data, savefilename)

                    messageLocalDataSource.updatePictureUrl(messageId, filepath)
                }
            }
        }
    }

    override suspend fun getAudiosForMessageIds(ids: List<String>){
        ids.forEach { messageId ->
            val savefilename = messageId + VOICEMSG_FILE_NAME
            //println("Fetching Audio for messageid $messageId")

            when (val audio = networkUtils.getAudioForAudioMessage(messageId).trackConnectivity()) {
                is NetworkResult.Error<*> -> {loggingRepository.logWarning("Audio error for messageid $messageId")}
                is NetworkResult.Success<ByteArray> -> {
                    val filepath = audioManager.saveAudioToStorage(audio.data, savefilename)
                    //println("Audio saved to $filepath")
                    // Persist the bare filename, not the absolute container path: on iOS the
                    // container UUID in an absolute path changes across reinstalls/updates and
                    // would go stale. The absolute path is re-derived at use-time.
                    messageLocalDataSource.updateAudioPath(messageId, savefilename)
                }
            }
        }
    }

    /**
     * Get missing message media (On ios after update installation, there are no more pictures).
     * Image-message part of AppRepository.getMissingPics, then all of getMissingAudios - each in
     * its own scope, so the audio check still starts once the image downloads are done.
     */
    override suspend fun fetchMissingMessageMedia() {
        coroutineScope {
            // Check image messages
            val messages = messageLocalDataSource.getImageMessages()
            val missingImageMessageIds = messages.filter { image ->
                image.id != null && !pictureManager.checkImageExists(image.pictureUrl ?: "")
            }.map { it.id!! }

            if (missingImageMessageIds.isNotEmpty()) {
                launch { getPicturesForMessageIds(missingImageMessageIds) }
            }
        }

        coroutineScope {
            // Check audio messages
            val messages = messageLocalDataSource.getAudioMessages()
            val missingAudioMessageIds = messages.filter { audio ->
                if (audio.id == null) return@filter false
                // Null/empty audioPath means the file was never downloaded — always treat as missing.
                // Without this guard, getRecordingPath("") returns filesDir itself, which exists as a
                // directory, so checkAudioExists would wrongly return true and skip the download.
                val filename = audio.audioPath?.substringAfterLast('/')?.takeIf { it.isNotEmpty() }
                    ?: return@filter true
                !audioManager.checkAudioExists(audioManager.getRecordingPath(filename))
            }.map { it.id!! }

            //println("Missing audio ids: $missingAudioMessageIds -------------------------------------")
            if (missingAudioMessageIds.isNotEmpty()) {
                launch { getAudiosForMessageIds(missingAudioMessageIds) }
            }
        }
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    /**
     * The purge AppRepository.dataSync runs after its parallel jobs: the message job may have
     * re-inserted rows of a removed group right after the group sync's own inline purge.
     */
    override suspend fun deleteMessagesForGroups(groupIds: List<String>) {
        groupIds.forEach { messageLocalDataSource.deleteMessagesForGroup(it) }
    }

    // ─── Writes ───────────────────────────────────────────────────────────────

    override suspend fun editMessage(message: Message, newContent: String) {

        if (message.id == null) {
            //edit a not sent message
            val existing = messageLocalDataSource.getMessageById(message.localPK)
            if (existing != null) {
                messageLocalDataSource.upsertMessage(existing.copy(
                    content = newContent,
                    sent = false
                ))
            }

        } else {
            val request = networkUtils.editMessage(
                messageId = message.id!!,
                newContent = newContent
            ).trackConnectivity()

            when (request) {
                is NetworkResult.Error<NetworkingError> ->  {
                    val existing = messageLocalDataSource.getMessageById(message.localPK)
                    if (existing != null) {
                        messageLocalDataSource.upsertMessage(existing.copy(
                            content = newContent,
                            sent = false
                        ))
                    }
                    errorChannel.sendErrorSuspend(ErrorEvent(error = request.error))
                }
                is NetworkResult.Success<MessageResponse> -> {
                    val existing = messageLocalDataSource.getMessageById(request.data.messageId)
                    if (existing != null) {
                        messageLocalDataSource.upsertMessage(existing.copy(
                            content = request.data.content,
                            changeDate = request.data.lastChanged.toString(),
                            edited = true,
                        ))
                    }

                }
            }
        }

    }

    override suspend fun deleteLocalMessage(localpk: Long) {
        messageLocalDataSource.deleteMessage(localpk)
    }

    override suspend fun deleteMessage(messageId: String){
        val request = networkUtils.deleteMessage(
            messageId = messageId
        ).trackConnectivity()

        when (request) {
            is NetworkResult.Error<NetworkingError> ->  {
                errorChannel.sendErrorSuspend(ErrorEvent(error = request.error))
            }
            is NetworkResult.Success<*> -> {
                messageLocalDataSource.deleteMessage(messageId)
            }

        }
    }

    override suspend fun reactToMessage(messageId: String, content: String) {
        val  request = networkUtils.reactToMessage(messageId = messageId, content = content).trackConnectivity()
        when (request) {
            is NetworkResult.Error -> errorChannel.sendErrorSuspend(ErrorEvent(error = request.error))
            is NetworkResult.Success -> {
                val existing = messageLocalDataSource.getMessageById(request.data.messageId)
                if (existing != null) {
                    messageLocalDataSource.upsertMessage(
                        existing.copy(
                            reactions = request.data.reactions.map {
                                Reaction(userId = it.userId, content = it.content)
                            },
                            changeDate = request.data.lastChanged.toString()
                        )
                    )
                }
            }
        }
    }

    override suspend fun votePoll(ownId: String, pollVote: PollVote) {
        val request = networkUtils.votePoll(
            pollVote.toPollVoteRequest()
        ).trackConnectivity()

        when (request) {
            is NetworkResult.Error<NetworkingError> ->  {
                errorChannel.sendErrorSuspend(ErrorEvent(error = request.error))
            }
            is NetworkResult.Success<MessageResponse> -> {
                val existing = messageLocalDataSource.getMessageById(request.data.messageId)
                //Skip a response older than what is stored - a socket push or a later vote's
                //response may have landed first, and must not be rolled back
                if (existing != null && !existing.isNewerThan(request.data.lastChanged)) {
                    messageLocalDataSource.upsertMessage(existing.copy(
                        poll = request.data.pollResponse?.toPollMessage(ownId),
                        changeDate = request.data.lastChanged.toString(),
                    ))
                }

            }
        }
    }

    override suspend fun deletePollOption(ownId: String, messageId: String, optionId: String) {
        val request = networkUtils.deletePollOption(
            PollOptionDeleteRequest(messageId = messageId, optionId = optionId)
        ).trackConnectivity()

        when (request) {
            is NetworkResult.Error<NetworkingError> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = request.error))
            }
            is NetworkResult.Success<MessageResponse> -> {
                val existing = messageLocalDataSource.getMessageById(request.data.messageId)
                //Skip a response older than what is stored - a socket push or a later vote's
                //response may have landed first, and must not be rolled back
                if (existing != null && !existing.isNewerThan(request.data.lastChanged)) {
                    messageLocalDataSource.upsertMessage(existing.copy(
                        poll = request.data.pollResponse?.toPollMessage(ownId),
                        changeDate = request.data.lastChanged.toString(),
                    ))
                }
            }
        }
    }
}

@file:OptIn(ExperimentalUuidApi::class)

package org.lerchenflo.schneaggchatv3mp.chat.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.lerchenflo.schneaggchatv3mp.PICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.VOICEMSG_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.toMessage
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.datasource.domain.ErrorEvent
import org.lerchenflo.schneaggchatv3mp.datasource.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.domain.MessageRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MarkReadGuard
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.Message
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MessageContent
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MessageContent.AudioContent
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MessageContent.ImageContent
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MessageContent.PollContent
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MessageContent.TextContent
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MessageReader
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.PollVote
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.Reaction
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.isNewerThan
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.MessageResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.MessageSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.PollOptionDeleteRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toDomainMessage
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toLocalPollMessage
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toNewPoll
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toPollCreateRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toPollMessage
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toPollVoteRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.utilities.AudioManager
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.getAudioBytes
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisLong
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisString
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.DecodedNotification
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * [MessageRepository] on top of the local [MessageLocalDataSource] and the server via [NetworkUtils].
 * The media and write code is moved verbatim from AppRepository (getPicturesForMessageIds,
 * getAudiosForMessageIds, the image-message part of getMissingPics + getMissingAudios, editMessage,
 * deleteLocalMessage, deleteMessage, reactToMessage, votePoll, deletePollOption), and so are
 * sendMessage, sendOfflineMessages and messageIdSync ([syncMessages]) with their three locks,
 * applyPushMessage and setAllChatMessagesRead. [markChatReadInBackground] is ChatViewModel's
 * mark-read block (serialised + deduplicated per [MarkReadGuard]).
 * The send path writes some rows through [AppDatabase.messageDao] directly, as it did there.
 *
 * @param externalScope app-wide scope (Koin passes `ApplicationScope`) that [sendMessage],
 * [setAllChatMessagesRead] and [markChatReadInBackground] run in, so they outlive the screen
 * that started them (plan R2). Tests pass `backgroundScope`.
 * @param ioDispatcher for the send's success branch, which ran on `Dispatchers.IO` (plan R4).
 */
class OfflineFirstMessageRepository(
    private val database: AppDatabase,
    private val messageLocalDataSource: MessageLocalDataSource,
    private val networkUtils: NetworkUtils,
    private val pictureManager: PictureManager,
    private val audioManager: AudioManager,
    private val loggingRepository: Logger,
    private val errorChannel: ErrorChannel,
    private val externalScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
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

    // ─── Send / offline queue / sync ──────────────────────────────────────────

    val sendMessageLock = Mutex()

    /**
     * @param localpk Local pk, only pass if already in db
     * @param clientMessageId Send idempotency key of an already-queued message. REQUIRED when
     * [localpk] is non-zero (a resend) - reusing the row's original key is the entire mechanism
     * that lets the server recognize a retry; silently minting a new one here for a resend would
     * defeat it. Never pass this for a brand-new message - one is minted below.
     *
     */
    override suspend fun sendMessage(ownId: String, messageId: String?, empfaenger: String, gruppe: Boolean, content: MessageContent, answerid: String?, localpk: Long, clientMessageId: String?){

        // externalScope (plan R2): started before the first suspension point, so the send
        // finishes even when the caller (e.g. a closed chat screen) is cancelled meanwhile.
        externalScope.async {
            sendMessageLock.withLock {
                var localpkintern = localpk

                val senddate = getCurrentTimeMillisString()
                val clientMessageIdIntern = if (localpkintern != 0L) { //Already upserted to db
                    requireNotNull(clientMessageId) {
                        "Resending an already-queued message (localPK=$localpkintern) must reuse its stored clientMessageId"
                    }
                } else {
                    //Generate random uuid per message for server side checking, pass when sending.
                    //Randomuuid because localpk will be reset on db drop, invisible despawned messages when multi device user
                    Uuid.random().toString()
                }

                //Interne message macha die ned alles hot
                val messageDto = when(content) {
                    is PollContent -> {
                        MessageDto(
                            localPK = localpkintern,
                            id = null,
                            msgType = MessageType.POLL,
                            content = "",
                            poll = content.poll.toLocalPollMessage(ownId),
                            senderId = ownId,
                            receiverId = empfaenger,
                            sendDate = senddate.toLong(),
                            updatedAt = senddate,
                            deleted = false,
                            groupMessage = gruppe,
                            answerId = answerid,
                            sent = false,
                            myMessage = true,
                            readByMe = true,
                            clientMessageId = clientMessageIdIntern
                        )
                    }
                    is TextContent -> {
                        MessageDto(
                            localPK = localpkintern,
                            id = messageId,
                            msgType = MessageType.TEXT,
                            content = content.message,
                            senderId = ownId,
                            receiverId = empfaenger,
                            sendDate = senddate.toLong(),
                            updatedAt = senddate,
                            deleted = false,
                            groupMessage = gruppe,
                            answerId = answerid,
                            sent = false,
                            myMessage = true,
                            readByMe = true,
                            clientMessageId = clientMessageIdIntern
                        )
                    }

                    is ImageContent -> {

                        MessageDto(
                            localPK = localpkintern,
                            id = null,
                            msgType = MessageType.IMAGE,
                            content = content.text,
                            senderId = ownId,
                            receiverId = empfaenger,
                            sendDate = senddate.toLong(),
                            updatedAt = senddate,
                            deleted = false,
                            groupMessage = gruppe,
                            answerId = answerid,
                            sent = false,
                            myMessage = true,
                            readByMe = true,
                            clientMessageId = clientMessageIdIntern
                        )
                    }

                    is AudioContent -> {

                        MessageDto(
                            localPK = localpkintern,
                            id = null,
                            msgType = MessageType.AUDIO,
                            //content = content.text,
                            senderId = ownId,
                            receiverId = empfaenger,
                            sendDate = senddate.toLong(),
                            updatedAt = senddate,
                            deleted = false,
                            groupMessage = gruppe,
                            answerId = answerid,
                            sent = false,
                            myMessage = true,
                            readByMe = true,
                            clientMessageId = clientMessageIdIntern
                        )
                    }
                }


                //Nachricht hot scho a pk vo da db, also scho din
                if (localpkintern == 0L){
                    localpkintern = database.messageDao().insertMessageDto(messageDto)
                    println("LocalPK: $localpkintern")
                }


                val serverrequest = when (content) {
                    is PollContent -> {
                        networkUtils.sendPollMessageToServer(
                            empfaenger = empfaenger,
                            gruppe = gruppe,
                            content = content.poll.toPollCreateRequest(),
                            answerid = answerid,
                            clientMessageId = clientMessageIdIntern
                        )
                    }
                    is TextContent -> {
                        networkUtils.sendTextMessageToServer(
                            messageId = messageId,
                            empfaenger = empfaenger,
                            gruppe = gruppe,
                            content = content.message,
                            answerid = answerid,
                            clientMessageId = clientMessageIdIntern
                        )
                    }

                    is ImageContent -> {
                        networkUtils.sendImageMessageToServer(
                            empfaenger = empfaenger,
                            gruppe = gruppe,
                            image = content.image,
                            text = content.text,
                            answerid = answerid,
                            clientMessageId = clientMessageIdIntern
                        )
                    }

                    is AudioContent -> {
                        networkUtils.sendAudioMessageToServer(
                            empfaenger = empfaenger,
                            gruppe = gruppe,
                            audio = content.audio,
                            answerid = answerid,
                            clientMessageId = clientMessageIdIntern
                        )
                    }
                }.trackConnectivity()

                when (serverrequest){
                    is NetworkResult.Error<NetworkingError> -> {

                        val error = serverrequest.error
                        //If badrequest (not met servers specifications (should not happen)) or conflict (duplicate message) delete locally
                        if (error is NetworkingError.BadRequest || error is NetworkingError.Conflict) {
                            database.messageDao().deleteMessageDtoByPk(localpkintern)
                        }

                        println("Message senden error: ${serverrequest.error}")
                        errorChannel.sendErrorSuspend(ErrorEvent(error = error))

                        when (content) {
                            is ImageContent -> {
                                pictureManager.savePictureToStorage(content.image, "unsent_" + localpkintern + PICTURE_FILE_NAME)
                                //ignore unsent audio or image, this should not happen anyway
                            }

                            is AudioContent -> {
                                audioManager.saveAudioToStorage(
                                    audioBytes = content.audio,
                                    filename = "unsent_" + localpkintern + VOICEMSG_FILE_NAME
                                )
                            }

                            else -> {}
                        }

                    }
                    is NetworkResult.Success<MessageResponse> -> {
                        withContext(ioDispatcher) {

                            println("Messageid returned from server: ${serverrequest.data.messageId}")

                            val existing =
                                database.messageDao().getMessageDtoById(serverrequest.data.messageId)

                            messageLocalDataSource.upsertMessage(
                                Message(
                                    localPK = localpkintern,
                                    id = serverrequest.data.messageId,
                                    msgType = serverrequest.data.msgType,

                                    content = serverrequest.data.content,
                                    poll = serverrequest.data.pollResponse?.toPollMessage(ownId),

                                    senderId = serverrequest.data.senderId,
                                    receiverId = serverrequest.data.receiverId,
                                    sendDate = serverrequest.data.sendDate.toString(),
                                    changeDate = serverrequest.data.lastChanged.toString(),
                                    deleted = serverrequest.data.deleted,
                                    groupMessage = serverrequest.data.groupMessage,
                                    answerId = serverrequest.data.answerId,
                                    sent = true,
                                    myMessage = true,
                                    readByMe = true,
                                    pictureUrl = existing?.pictureUrl,
                                    readers = serverrequest.data.readers.map {
                                        MessageReader(
                                            readerEntryId = 0L,
                                            messageId = serverrequest.data.messageId,
                                            readerId = it.userId,
                                            readDate = it.readAt.toString()
                                        )
                                    },
                                    // Bypasses /messages/sync - never let it advance the sync
                                    // cursor (MAX(version) over the messages table).
                                    version = existing?.version ?: 0L
                                )
                            )

                            if (serverrequest.data.msgType == MessageType.IMAGE) {
                                getPicturesForMessageIds(listOf(serverrequest.data.messageId))
                                pictureManager.deletePicture("unsent_" + localpkintern + PICTURE_FILE_NAME)
                            } else if (serverrequest.data.msgType == MessageType.AUDIO) {
                                getAudiosForMessageIds(listOf(serverrequest.data.messageId))

                                audioManager.deleteAudio("unsent_" + localpkintern + VOICEMSG_FILE_NAME)
                            }
                        }
                    }
                }
            }
        }.await()

    }


    private val sendOfflineLock = Mutex()

    /**
     * Send offline messages. Locks this function, and calls the sendmessage for every message.
     * No async, because multiple messages could be sent at the same time.
     * Always wait for the send lock to be unlocked, that way there can not be an unsent message
     * be retrieved from the db as unsent while it is already sending.
     */
    override suspend fun sendOfflineMessages(ownId: String){
        sendOfflineLock.withLock {

            while (true) {

                while (sendMessageLock.isLocked) {
                    //If the message sending lock is active, do not send offline messages
                    //println("OFFLINE MESSAGE SENDING: Sending in progress, waiting for lock")
                    delay(15.milliseconds)
                }


                val messages = messageLocalDataSource.getUnsentMessages()
                if (messages.isEmpty()) {
                    //println("OFFLINE MESSAGE SENDING: NO UNSENT MESSAGES")
                    return
                }

                val m = messages.first()

                //A poll row whose poll no longer decodes (the type converter returns null for it) can
                //never be sent - drop it, or it would block the whole queue forever
                if (m.msgType == MessageType.POLL && m.poll == null) {
                    database.messageDao().deleteMessageDtoByPk(m.localPK)
                    continue
                }

                sendMessage(
                    empfaenger = m.receiverId,
                    gruppe = m.groupMessage,
                    content = when (m.msgType) {
                        MessageType.TEXT -> {
                            TextContent(m.content)
                        }

                        MessageType.IMAGE -> {
                            val image =
                                pictureManager.loadPictureFromStorage("unsent_" + m.localPK + PICTURE_FILE_NAME)

                            if (image == null) {
                                TextContent("Offline image sending failed")
                            } else {
                                ImageContent(
                                    image = image,
                                    text = m.content
                                )
                            }
                        }

                        MessageType.POLL -> {
                            //Null-checked above. Rebuilt with the shared mapper so sub polls and
                            //every other setting survive the resend
                            PollContent(m.poll!!.toNewPoll())
                        }

                        MessageType.AUDIO -> {
                            if(m.audioPath == null){
                                TextContent("Offline audio sending failed")
                            }
                            val audio = getAudioBytes(
                                audioManager.getRecordingPath("unsent_" + m.localPK + VOICEMSG_FILE_NAME)
                            )

                            AudioContent(
                                audio = audio
                            )
                        }

                        MessageType.SYSTEM -> error("SYSTEM messages are server-authored and are never queued locally as unsent")
                    },
                    answerid = m.answerId,
                    localpk = m.localPK,
                    messageId = null,
                    ownId = ownId,
                    clientMessageId = m.clientMessageId
                )


                val nextMessages = messageLocalDataSource.getUnsentMessages()
                if (nextMessages.firstOrNull()?.localPK == m.localPK) return
            }


        }
    }


    /**
     * Serialises [syncMessages]'s paging loop. Two overlapping runs each read `since` from
     * MAX(version) before the other has written its page, so the slower one can upsert an
     * already-superseded copy of a message on top of the newer one - which is how a chat marked
     * read came back unread again. Media downloads stay outside the lock, they only add files.
     */
    private val messageSyncLock = Mutex()

    /**
     * Execute a message sync
     *
     * @param fetchMedia whether to immediately download images/audios for newly-synced
     * messages. Disabled during the full data sync (`AppRepository.dataSync`) so that media
     * downloads happen as part of the MEDIA job (and its progress indicator) instead of inside
     * the MESSAGES job.
     */
    override suspend fun syncMessages(fetchMedia: Boolean) {

        val ownId = SessionCache.requireLoggedIn()?.userId ?: return

        val imagesToGet = mutableListOf<String>()
        val audiosToGet = mutableListOf<String>()

        messageSyncLock.withLock {

            var since = messageLocalDataSource.getLastSyncedMessageVersion()
            var moreMessages = true

            while (moreMessages) {

                val messageSyncResponse = networkUtils.messageSync(since = since).trackConnectivity()

                when (messageSyncResponse) {
                    is NetworkResult.Error<*> -> {
                        loggingRepository.logWarning("messageid sync error: ${messageSyncResponse.error}")
                        break // Stop on error, never advance past a batch we failed to fetch
                    }

                    is NetworkResult.Success<MessageSyncResponse> -> {
                        val updatedMessages = messageSyncResponse.data.updatedMessages
                        val deletedMessages = messageSyncResponse.data.deletedMessages
                        moreMessages = messageSyncResponse.data.moreMessages

                        // Deletions can now arrive on any page, not just page 0. Apply them
                        // before the updates below so a crash mid-batch never leaves MAX(version)
                        // ahead of what was actually committed.
                        messageLocalDataSource.deleteMessages(deletedMessages)

                        // The whole page goes in as one batch: every separate write would invalidate
                        // the message table again and re-run the chat selector's query chain.
                        val sortedUpdates = updatedMessages.sortedBy { it.version }
                        val existingById = messageLocalDataSource
                            .getMessageDtosByIds(sortedUpdates.map { it.messageId })
                            .associateBy { it.id }

                        // A message of our own can arrive here before our own sendMessage() call for
                        // it has processed its response (dataSync runs fully in parallel with sends -
                        // nothing serializes them). Falling back to the pending row's clientMessageId
                        // (only ever populated on our own sends, and only ever echoed back to us by
                        // the server for our own messages) merges onto that row instead of creating a
                        // second, permanently-orphaned one.
                        val unmatchedClientMessageIds = sortedUpdates
                            .filter { existingById[it.messageId] == null }
                            .mapNotNull { it.clientMessageId }
                        val existingByClientMessageId = messageLocalDataSource
                            .getMessageDtosByClientMessageIds(unmatchedClientMessageIds)
                            .associateBy { it.clientMessageId }

                        messageLocalDataSource.upsertMessages(
                            sortedUpdates.map { messageResponse ->
                                val existing = existingById[messageResponse.messageId]
                                    ?: messageResponse.clientMessageId?.let { existingByClientMessageId[it] }
                                messageResponse.toDomainMessage(
                                    ownId = ownId,
                                    existingLocalPK = existing?.localPK ?: 0L,
                                    existingPictureUrl = existing?.pictureUrl,
                                    existingAudioPath = existing?.audioPath,
                                    version = messageResponse.version
                                )
                            }
                        )

                        sortedUpdates.forEach { messageResponse ->
                            when (messageResponse.msgType) {
                                MessageType.IMAGE -> imagesToGet += messageResponse.messageId
                                MessageType.AUDIO -> audiosToGet += messageResponse.messageId
                                else -> {}
                            }
                        }

                        // Always trust the envelope's watermark, never max(updatedMessages) - a
                        // page can be all deletions, in which case updatedMessages is empty but
                        // newVersion still advances.
                        since = messageSyncResponse.data.newVersion

                        //println("MessageIdSync finished, new messages: ${updatedMessages.size}")
                    }
                }
            }

            //println("Messagesync completed")

        }

        if (fetchMedia) {
            if (imagesToGet.isNotEmpty()) {
                println("Images to fetch: ${imagesToGet.size}")
                getPicturesForMessageIds(imagesToGet)
            }

            if (audiosToGet.isNotEmpty()) {
                println("Audios to fetch: ${audiosToGet.size}")
                getAudiosForMessageIds(audiosToGet)
            }
        }
    }

    // ─── Push apply / mark read ───────────────────────────────────────────────

    /**
     * Instantly upserts a message delivered via push notification (FCM/APNs) into Room, so a
     * chat opened before the next sync already shows it. Never overwrites an existing row and
     * never advances the sync cursor (version stays 0, the same trick
     * [MessageRepository.syncMessages] and the socket handler rely on) - the caller is still
     * expected to follow up with [MessageRepository.syncMessages], which replaces this
     * provisional row with the authoritative one (readers, reactions, real version).
     */
    override suspend fun applyPushMessage(decoded: DecodedNotification.Message) {
        if (SessionCache.requireLoggedIn()?.userId == null) return

        if (decoded.reaction) {
            applyPushReaction(decoded)
            return
        }

        // Only TEXT pushes carry enough content for a useful provisional row - IMAGE/AUDIO/POLL
        // wait for the real sync + media fetch instead of flashing an empty bubble.
        if (decoded.messageType != MessageType.TEXT) return

        // Never overwrite an already-synced or socket-delivered row.
        if (messageLocalDataSource.getMessageById(decoded.msgId) != null) return

        // Blank when the push came from a server that predates the plaintext `content` field.
        val content = decoded.content.ifBlank { return }

        val sendDate = (decoded.sendDate.takeIf { it > 0L } ?: getCurrentTimeMillisLong()).toString()

        messageLocalDataSource.upsertMessage(
            Message(
                id = decoded.msgId,
                msgType = MessageType.TEXT,
                content = content,
                senderId = decoded.senderId,
                // Group chat is identified by groupId, not the push's receiverId (the
                // recipient user) - see NotificationContentResolver.resolveMessage.
                receiverId = if (decoded.groupMessage) decoded.groupId else decoded.receiverId,
                groupMessage = decoded.groupMessage,
                answerId = decoded.answerId.ifBlank { null },
                sendDate = sendDate,
                changeDate = sendDate,
                sent = true,
                myMessage = false,
                readByMe = false,
                readers = emptyList(),
                reactions = emptyList(),
                version = 0L,
            )
        )
    }

    /** Applies a reaction push to an already-local message. Never creates a new row. */
    private suspend fun applyPushReaction(decoded: DecodedNotification.Message) {
        val existing = messageLocalDataSource.getMessageById(decoded.msgId) ?: return

        val emoji = decoded.content.ifBlank { return }

        messageLocalDataSource.upsertMessage(
            existing.copy(
                reactions = existing.reactions.filterNot { it.userId == decoded.senderId } +
                    Reaction(userId = decoded.senderId, content = emoji, reactedAt = getCurrentTimeMillisLong())
            )
        )
    }

    /**
     * Marks a whole chat read. The server is the single source of truth for read state: nothing
     * is written to Room here, the call only tells the server and then pulls the result back
     * through [MessageRepository.syncMessages], which rewrites `readByMe` from the authoritative
     * `readers` list.
     *
     * Writing it locally first used to lose a race against a parallel sync - opening a chat from
     * a notification kicks off a `AppRepository.dataSync` at the same moment, and that sync
     * re-fetched the message before the server had processed the read, resetting `readByMe` to
     * false again.
     *
     * The pull is not optional: the server deliberately excludes the acting user from its own
     * MessageChange socket fan-out, so this device never hears about its own read receipt.
     *
     * @return true when the server accepted the read. False (offline, error) means nothing
     * changed anywhere and the caller is expected to try again later.
     */
    override suspend fun setAllChatMessagesRead(chatid: String, gruppe: Boolean, timestamp: String): Boolean {

        // externalScope (plan R2): started before the first suspension point, so the request and
        // the sync behind it finish even when the caller (e.g. a closed chat screen) is cancelled.
        return externalScope.async {
            SessionCache.requireLoggedIn() ?: return@async false

            val result = networkUtils.setMessagesRead(chatid, gruppe, timestamp.toLong()).trackConnectivity()
            if (result !is NetworkResult.Success) {
                loggingRepository.logWarning("Setting chat $chatid read failed: ${(result as? NetworkResult.Error<*>)?.error}")
                return@async false
            }

            // With media: unlike the MESSAGES job in `AppRepository.dataSync` there is no MEDIA job
            // behind this call, so a picture or voice message first seen by this sync would otherwise
            // stay undownloaded until the next full sync gets around to the missing-media backfill.
            syncMessages()
            true
        }.await()
    }

    /**
     * Moved from ChatViewModel.setAllMessagesRead, which ran this block in `applicationScope`
     * (plan R2): leaving the chat must not cancel the request, a request still queued behind
     * the lock, or the sync that pulls the server's read receipt back in.
     */
    override fun markChatReadInBackground(
        chatid: String,
        gruppe: Boolean,
        guard: MarkReadGuard,
        readNewestMessageId: () -> String?,
    ) {
        externalScope.launch {
            guard.mutex.withLock {
                val newestMessageId = readNewestMessageId()
                //Whoever held the lock before us may already have covered this exact state.
                if (newestMessageId != null && newestMessageId == guard.lastMarkedReadMessageId) {
                    return@withLock
                }

                val accepted = setAllChatMessagesRead(
                    chatid = chatid,
                    gruppe = gruppe,
                    timestamp = getCurrentTimeMillisString()
                )
                guard.lastMarkedReadMessageId = if (accepted) newestMessageId else null
            }
        }
    }
}

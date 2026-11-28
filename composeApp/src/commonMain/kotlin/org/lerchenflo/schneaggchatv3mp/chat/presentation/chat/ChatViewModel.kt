package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ismoy.imagepickerkmp.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.picker.GalleryPhotoResult
import io.github.lerchenflo.voicemessages.VoiceMessageFiles
import io.github.lerchenflo.voicemessages.VoicePlaybackController
import io.github.lerchenflo.voicemessages.VoicePlaybackState
import io.github.lerchenflo.voicemessages.VoiceRecordingController
import io.github.lerchenflo.voicemessages.VoiceRecordingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.OpenChatTracker
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.app.domain.isSuccess
import org.lerchenflo.schneaggchatv3mp.app.domain.isTransient
import org.lerchenflo.schneaggchatv3mp.app.domain.reportErrorTo
import org.lerchenflo.schneaggchatv3mp.app.domain.reportLastingErrorTo
import org.lerchenflo.schneaggchatv3mp.app.domain.sendError
import org.lerchenflo.schneaggchatv3mp.app.launchOnMain
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendRequestState
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.MarkReadGuard
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageContent
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageDisplayItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.NewPoll
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVote
import org.lerchenflo.schneaggchatv3mp.chat.domain.SenderInfo
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentity
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.toChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.SendMessageContent.TextContent
import org.lerchenflo.schneaggchatv3mp.events.domain.EventRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.shownUserIds
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.userIdentityNameText
import org.lerchenflo.schneaggchatv3mp.utilities.device.IncomingDataManager
import org.lerchenflo.schneaggchatv3mp.utilities.device.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.device.PermissionState
import org.lerchenflo.schneaggchatv3mp.utilities.media.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.media.fileUriToPath
import org.lerchenflo.schneaggchatv3mp.utilities.ui.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import org.lerchenflo.schneaggchatv3mp.utilities.media.fullQualityImage
import schneaggchatv3mp.composeapp.generated.resources.image_full_quality_failed
import schneaggchatv3mp.composeapp.generated.resources.images_could_not_be_loaded
import schneaggchatv3mp.composeapp.generated.resources.message_too_long
import schneaggchatv3mp.composeapp.generated.resources.poll_cannot_vote_on_unsent
import kotlin.time.Clock

class ChatViewModel(
    private val errorChannel: ErrorChannel,
    private val applicationScope: ApplicationScope,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val userIdentityRepository: UserIdentityRepository,
    private val groupRepository: GroupRepository,
    private val eventRepository: EventRepository,
    private val settingsRepository: SettingsRepository,
    private val navigator: Navigator,
    private val loggingRepository: Logger,
    private val pictureManager: PictureManager,
    private val permissionsManager: PermissionManager,
    private val voiceFiles: VoiceMessageFiles,

    val chatId: String,
    val isGroup: Boolean,
): ViewModel() {

    companion object {
        private const val MAX_VOICE_MSG_TIME = 2*60*1000L

        // Messages loaded per step while scrolling up.
        private const val PAGE_SIZE = 100
        // Extra messages above the oldest unread / a jump target, so it isn't the topmost row.
        private const val WINDOW_MARGIN = 20
    }

    // Voice messages: playback of one message at a time and the recording of the unsent draft
    private val voicePlayback = VoicePlaybackController()
    private val voiceRecording = VoiceRecordingController(
        files = voiceFiles,
        playback = voicePlayback,
        maxDurationMs = MAX_VOICE_MSG_TIME
    )

    private val _state = MutableStateFlow(
        ChatState(
            chatId = chatId,
            isGroup = isGroup,
            maxVoiceMsgTime = MAX_VOICE_MSG_TIME
        )
    )
    val state: StateFlow<ChatState> = _state.asStateFlow()

    /**
     * Stable flow reference, kept OUT of ChatState on purpose: playback position ticks ~5x/sec
     * while a voice message plays, and folding that into the state value would re-emit (and
     * recompose) the whole screen on every tick instead of just the audio player.
     */
    val playbackProgress: StateFlow<VoicePlaybackState> = voicePlayback.state

    /** Live microphone levels while recording, kept out of ChatState for the same reason. */
    val recordingLevels: StateFlow<List<Float>> = voiceRecording.levels

    private var newMessagesBoundaryComputed = false
    private val newMessagesBoundaryId = MutableStateFlow<String?>(null)

    // Guards setAllMessagesRead() (lastMarkedReadMessageId + the lock that serialises the
    // requests, see MarkReadGuard). Owned by this screen, so the dedup lives as long as it does;
    // the requests themselves run in the repository's app-wide scope.
    private val markReadGuard = MarkReadGuard()

    /**
     * Centralized action handler for all chat-screen user interactions.
     * Composables only need a single `onAction: (ChatAction) -> Unit` callback.
     */
    fun onAction(action: ChatAction) {
        when (action) {
            ChatAction.OnBackClick -> onBackClick()
            ChatAction.OnChatDetailsClick -> onChatDetailsClick()

            is ChatAction.OnSendContentChange -> updateSendContent(action.content)
            ChatAction.OnSendClick -> sendCurrentMessage()

            ChatAction.OnConfirmEditClick -> confirmEditMessage()
            ChatAction.OnCancelReply -> updateReplyMessage(null)

            is ChatAction.OnImagesSelected -> onImagesSelected(action.results, action.fromCamera)
            is ChatAction.OnCreatePoll -> createPollMessage(action.poll)
            is ChatAction.OnSaveEditedPoll -> saveEditedPoll(action.poll)

            ChatAction.OnStartRecording -> startRecording()
            ChatAction.OnStopRecording -> stopRecording()
            ChatAction.OnDiscardRecording -> discardRecording()

            is ChatAction.OnMessageAction -> onMessageAction(action.action)

            ChatAction.OnLoadOlderMessages -> loadOlderMessages()
            is ChatAction.OnLoadMessagesUntil -> loadMessagesUntil(action.messageId)
        }
    }

    private fun updateSendContent(content: SendMessageContent) {
        _state.update { it.copy(sendContent = content) }
    }

    private fun updateReplyMessage(message: Message?, sender: SenderInfo? = null) {
        _state.update {
            it.copy(
                replyMessage = message,
                replyMessageSender = sender
            )
        }
    }

    private fun updateEditMessage(newValue: Message?) {
        _state.update { it.copy(editMessage = newValue) }
    }

    /**
     * Asks the server to mark this chat read. Nothing is written locally - the repository pulls
     * the server's answer back in, which is what finally flips `readByMe` (see
     * [MessageRepository.setAllChatMessagesRead]).
     */
    fun setAllMessagesRead() {

        SessionCache.requireLoggedIn() ?: return

        //The chat's notifications are dismissed by GlobalViewModel as soon as the chat is on screen,
        //whether or not anything here is unread

        //App-wide scope (in the repository), not viewModelScope: leaving the chat must not cancel
        //the request, a request still queued behind the lock, or the sync that pulls the server's
        //read receipt back in.
        messageRepository.markChatReadInBackground(
            chatid = chatId,
            gruppe = isGroup,
            guard = markReadGuard,
            readNewestMessageId = ::newestMessageId,
        )
    }

    /** Newest message currently on screen - the list is newest-first (see MessageDao.getMessagesByUserIdFlow). */
    private fun newestMessageId(): String? = _state.value.displayItems
        .filterIsInstance<MessageDisplayItem.MessageItem>()
        .firstOrNull()?.message?.id

    private fun hasUnreadMessages(): Boolean = _state.value.displayItems
        .any { it is MessageDisplayItem.MessageItem && !it.message.readByMe }

    // No scope needed: the repository writes in the app-wide scope, so this also works from onCleared
    private fun saveDraft(){
        // todo wenn bild oder sprachnachricht oder so künnt ma des speichera
        val sendContent = state.value.sendContent
        if(sendContent is TextContent) { // schoua ob es textfeld leer isch
            settingsRepository.saveDraft(
                chatId = chatId,
                group = isGroup,
                string = sendContent.textMessage.text
            )
        }
    }

    private fun sendCurrentMessage() {
        sendMessage(
            message = state.value.sendContent,
            replyTo = state.value.replyMessage
        )
    }

    private fun sendMessage(message: SendMessageContent, replyTo: Message? = null) {

        val ownId = SessionCache.requireLoggedIn()?.userId ?: return

        //Validation of message
        when (message) {
            is TextContent -> {
                if (message.textMessage.text.isBlank()) return

                if (message.textMessage.text.length >= 10000) {
                    showMessageTooLong()
                    return
                }
            }
            is SendMessageContent.ImageContent -> {
                if (message.images.isEmpty()) return

                if (message.text.text.length >= 10000) {
                    showMessageTooLong()
                    return
                }

                if (message.text.text.isBlank()) {
                    message.text = message.text.copy(text = "") //Clear string content if only linebreak / tab
                }
            }
            is SendMessageContent.AudioContent -> {
                // Still recording: the send button is the stop button then
                if (message.recording !is VoiceRecordingState.Recorded) return
            }

        }


        // An in-flight send survives leaving the chat screen, and so does its error popup: a
        // rejected message is gone, so the user has to hear about it
        when (message) {
            is TextContent -> {
                applicationScope.launchOnMain {
                    messageRepository.sendMessage(
                        empfaenger = chatId,
                        gruppe = isGroup,
                        content = MessageContent.TextContent(message.textMessage.text),
                        answerid = replyTo?.id,
                        messageId = null,
                        ownId = ownId,
                    ).reportLastingErrorTo(errorChannel)
                }
            }

            is SendMessageContent.ImageContent -> {
                applicationScope.launchOnMain {
                    // All prepared first, so the images still go out in the order they were picked
                    val imagesToSend = imagesToSend(message.images, message.fullQuality)
                    imagesToSend.forEachIndexed { index, image ->
                        launch {
                            messageRepository.sendMessage(
                                empfaenger = chatId,
                                gruppe = isGroup,
                                content = MessageContent.ImageContent(
                                    image = image,
                                    text = if (index == 0) message.text.text else ""
                                ),
                                answerid = replyTo?.id,
                                messageId = null,
                                ownId = ownId
                            ).reportLastingErrorTo(errorChannel)
                        }
                    }
                }
            }

            is SendMessageContent.AudioContent -> {
                val recorded = message.recording as VoiceRecordingState.Recorded // checked above
                applicationScope.launchOnMain {
                    messageRepository.sendVoiceMessage(
                        empfaenger = chatId,
                        gruppe = isGroup,
                        recordingPath = recorded.filePath,
                        answerid = replyTo?.id,
                        ownId = ownId
                    ).reportLastingErrorTo(errorChannel)
                }
                // The file stays for the send; the input bar goes back to text through observeVoiceRecording
                viewModelScope.launch { voiceRecording.clear() }
            }
        }

        updateReplyMessage(null)
        updateSendContent(TextContent(TextFieldValue("")))
    }

    private fun onImagesSelected(results: List<GalleryPhotoResult>, fromCamera: Boolean) {
        if (fromCamera) saveCameraPhotosToGallery(results)

        viewModelScope.launch {
            // The file as picked, not loadBytes(): that re-encodes and drops the metadata
            val originals = results.map { photo ->
                val bytes = pictureManager.readPickedImage(photo.uri)
                if (bytes == null) loggingRepository.logWarning("Picked image could not be read")
                bytes
            }
            stageImages(originals, text = (state.value.sendContent as? TextContent)?.textMessage ?: TextFieldValue(""))
        }

    }

    /**
     * Stages [originals] in the input bar, each with its small copy ([CHAT_IMAGE_TARGET_BYTES]).
     * An image that couldn't be read (null) or decoded is left out, with one popup for all of them.
     */
    private suspend fun stageImages(originals: List<ByteArray?>, text: TextFieldValue) {
        val staged = originals.mapNotNull { original -> original?.let { stagedImageOrNull(it) } }
        if (staged.size < originals.size) {
            errorChannel.sendError(UiText.StringResourceText(Res.string.images_could_not_be_loaded))
        }
        if (staged.isNotEmpty()) updateSendContent(SendMessageContent.ImageContent(images = staged, text = text))
    }

    private suspend fun stagedImageOrNull(original: ByteArray): StagedImage? =
        try {
            StagedImage(original = original, compressed = pictureManager.downscaleImage(original, CHAT_IMAGE_TARGET_BYTES))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loggingRepository.logWarning("Picked image could not be decoded: ${e.message}")
            null
        }

    /**
     * The bytes [images] are sent as: the small copies, or with [fullQuality] the originals
     * (re-encoded only where the server needs it). An image whose full-quality version fails is
     * not sent, with one popup for all of them.
     */
    private suspend fun imagesToSend(images: List<StagedImage>, fullQuality: Boolean): List<ByteArray> {
        if (!fullQuality) return images.map { it.compressed }
        val prepared = images.mapNotNull { image ->
            try {
                pictureManager.fullQualityImage(image.original)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loggingRepository.logWarning("Full-quality image could not be prepared: ${e.message}")
                null
            }
        }
        if (prepared.size < images.size) {
            errorChannel.sendError(UiText.StringResourceText(Res.string.image_full_quality_failed))
        }
        return prepared
    }

    /**
     * An in-app camera photo goes into the gallery as the original file (full quality, all
     * metadata) right away, whether it gets sent or not; the message itself sends a smaller copy.
     */
    private fun saveCameraPhotosToGallery(photos: List<GalleryPhotoResult>) {
        val takenAt = Clock.System.now().toEpochMilliseconds()
        applicationScope.launch {
            photos.forEachIndexed { index, photo ->
                val savedTo = pictureManager.saveToGallery(fileUriToPath(photo.uri), "Schneaggchat_${takenAt}_$index")
                if (savedTo.isEmpty()) loggingRepository.logWarning("Camera photo: gallery copy failed")
            }
        }
    }

    private fun createPollMessage(poll: NewPoll) {

        val ownId = SessionCache.requireLoggedIn()?.userId ?: return
        val replyMessage = state.value.replyMessage

        applicationScope.launchOnMain {
            messageRepository.sendMessage(
                empfaenger = chatId,
                gruppe = isGroup,
                content = MessageContent.PollContent(poll),
                answerid = replyMessage?.id,
                messageId = null,
                ownId = ownId
            ).reportLastingErrorTo(errorChannel)

            updateReplyMessage(null)
        }
    }

    private fun saveEditedPoll(poll: NewPoll) {
        val ownId = SessionCache.requireLoggedIn()?.userId ?: return
        val message = state.value.editPollMessage ?: return
        val messageId = message.id ?: return
        val original = message.poll ?: return
        if (state.value.isSavingPollEdit) return

        _state.update { it.copy(isSavingPollEdit = true) }

        viewModelScope.launch {
            val result = messageRepository.editPoll(ownId, messageId, original, poll).reportErrorTo(errorChannel)

            //A network error keeps the popup open for another try. If the server rejected the edit
            //the poll may have changed meanwhile: the popup closes, the next edit opens the stored poll
            val canRetry = result is DataResult.Error && result.error.isTransient()
            _state.update { it.copy(isSavingPollEdit = false, editPollMessage = it.editPollMessage.takeIf { canRetry }) }
        }
    }

    private fun deleteMessage(message: Message) {
        viewModelScope.launch {
            if (message.id == null) {
                messageRepository.deleteLocalMessage(message.localPK)
                println("Offline message deleted")
            } else {
                messageRepository.deleteMessage(message.id!!).reportErrorTo(errorChannel)
                println("Remote message deleted")
            }
        }
    }

    /**
     * Handler for message-level user interactions (reply/react/edit/delete/copy/details/poll/
     * audio playback/...), reached via ChatAction.OnMessageAction.
     */
    private fun onMessageAction(action: MessageAction) {

        val ownId = SessionCache.requireLoggedIn()?.userId ?: return

        when (action) {
            is MessageAction.VotePoll -> {
                viewModelScope.launch {
                    messageRepository.votePoll(
                        pollVote = PollVote(
                            messageId = action.messageId,
                            id = action.optionId,
                            text = null,
                            maxAllowedAnswers = null,
                            selected = action.checked
                        ),
                        ownId = ownId
                    ).reportErrorTo(errorChannel)
                }
            }

            is MessageAction.AddCustomPollOption -> {
                viewModelScope.launch {
                    messageRepository.votePoll(
                        ownId = ownId,
                        pollVote = PollVote(
                            messageId = action.messageId,
                            id = null,
                            text = action.text,
                            maxAllowedAnswers = action.maxAnswers,
                            selected = true,
                            parentOptionId = action.parentOptionId,
                        )
                    ).reportErrorTo(errorChannel)
                }
            }
            is MessageAction.DeletePollOption -> {
                viewModelScope.launch {
                    messageRepository.deletePollOption(
                        ownId = ownId,
                        messageId = action.messageId,
                        optionId = action.optionId
                    ).reportErrorTo(errorChannel)
                }
            }
            is MessageAction.StartEditPoll -> _state.update { it.copy(editPollMessage = action.message) }
            MessageAction.CancelEditPoll -> _state.update { it.copy(editPollMessage = null) }
            MessageAction.PollMessageNotSent -> {
                viewModelScope.launch {
                    errorChannel.sendError(UiText.StringResourceText(Res.string.poll_cannot_vote_on_unsent))
                }
            }
            is MessageAction.PlayAudio -> playAudio(messageId = action.messageId, fileName = action.audioPath)
            is MessageAction.PauseAudio -> voicePlayback.pause()
            is MessageAction.SeekAudio -> voicePlayback.seekTo(action.messageId, action.position)
            is MessageAction.DownloadImage -> {
                viewModelScope.launch {
                    val savePath = pictureManager.downloadImage(action.pictureUrl, action.filename + ".jpeg")
                    //SnackbarManager.showMessage("Image saved as ${action.filename}")
                    println("Image saved to $savePath")
                }
            }


            is MessageAction.DeleteMessage -> deleteMessage(action.message)
            is MessageAction.StartEditMessage -> {
                updateEditMessage(action.message)
                updateSendContent(TextContent(TextFieldValue(action.message.content)))
            }
            MessageAction.CancelEditMessage -> {
                updateEditMessage(null)
                updateSendContent(TextContent(TextFieldValue("")))
                //println("Update message sendtext to empty")
            }

            is MessageAction.ReplyToMessage -> updateReplyMessage(action.message, action.sender)
            is MessageAction.ToggleReaction -> {
                viewModelScope.launch {
                    messageRepository.reactToMessage(
                        action.messageId,
                        action.reaction
                    ).reportErrorTo(errorChannel)
                }
            }
            is MessageAction.OnSenderClick -> openUnknownUserPopup(action.senderId, action.senderName, ownId)
        }
    }

    /**
     * Opens the unknown-user popup for a group sender who is not a friend. A tap on a friend's or a
     * blocked user's name does nothing (user, 2026-11-11).
     */
    private fun openUnknownUserPopup(userId: String, name: String, ownId: String) {
        if (userId == ownId) return
        viewModelScope.launch {
            if (FriendRequestState.of(userRepository.getUserById(userId), ownId) == null) return@launch
            navigator.navigate(Route.UnknownUser(userId = userId, name = name))
        }
    }

    private fun confirmEditMessage() {
        editMessage(message = state.value.editMessage, content = state.value.sendContent)
    }

    private fun editMessage(message: Message?, content: SendMessageContent) {
        viewModelScope.launch {

            if (content !is TextContent) return@launch
            if (message == null) return@launch

            //Block empty edit
            if (content.textMessage.text.isBlank()) return@launch

            val saved = messageRepository.editMessage(
                message = message,
                newContent = content.textMessage.text
            ).reportErrorTo(errorChannel).isSuccess

            println("Edit: Newcontent: ${content.textMessage}")

            //Clear text after editing message - a failed edit keeps it, so it can be sent again
            if (saved) onMessageAction(MessageAction.CancelEditMessage)
        }
    }




    private fun showMessageTooLong() {
        viewModelScope.launch {
            errorChannel.sendError(UiText.StringResourceText(Res.string.message_too_long))
        }
    }

    private fun onBackClick() {
        saveDraft()

        viewModelScope.launch {
            navigator.navigateBack(navigationOptions = Navigator.NavigationOptions(
                removeAllScreensByClass = listOf(Route.ChatDetails::class, Route.Chat::class)
            ))
        }
    }

    private fun onChatDetailsClick() {
        viewModelScope.launch {
            navigator.navigate(Route.ChatDetails(chatId = chatId, isGroup = isGroup))
        }
    }

    // Voice messages: the kmp-voice-messages controllers do the work, the ViewModel only wires them up
    private fun startRecording() {
        viewModelScope.launch {
            if (!hasMicrophonePermission()) return@launch
            // Refused during a phone call (AudioBusy); nothing records then
            voiceRecording.start().onFailure { logVoiceFailure("start the recording", it) }
        }
    }

    private suspend fun hasMicrophonePermission(): Boolean =
        permissionsManager.checkMicrophonePermission() == PermissionState.GRANTED ||
            permissionsManager.requestMicrophonePermission() == PermissionState.GRANTED

    private fun stopRecording() {
        viewModelScope.launch {
            voiceRecording.stop().onFailure { logVoiceFailure("stop the recording", it) }
        }
    }

    private fun discardRecording() {
        viewModelScope.launch {
            voiceRecording.discard().onFailure { logVoiceFailure("discard the recording", it) }
        }
    }

    /** [fileName] is the bare stored name (an old absolute path works too), [messageId] the playback id. */
    private fun playAudio(messageId: String, fileName: String) {
        if (fileName.isBlank()) return // not downloaded yet
        voicePlayback.play(messageId, voiceFiles.path(fileName))
            .onFailure { logVoiceFailure("play a voice message", it) }
    }

    private fun logVoiceFailure(what: String, error: Throwable) {
        viewModelScope.launch {
            loggingRepository.logWarning("Voice message: could not $what (${error::class.simpleName}: ${error.message})")
        }
    }

    /**
     * Mirrors the recording into the input bar: a running or recorded draft shows the voice bar,
     * and Idle (discarded or sent) switches back to an empty text field.
     */
    private fun observeVoiceRecording() {
        viewModelScope.launch {
            voiceRecording.state.collect { recording ->
                if (recording != VoiceRecordingState.Idle) {
                    updateSendContent(SendMessageContent.AudioContent(recording))
                } else if (state.value.sendContent is SendMessageContent.AudioContent) {
                    updateSendContent(TextContent(TextFieldValue("")))
                }
            }
        }
    }




    /**
     * How many of the newest messages the screen has loaded. Null until [initialMessageLimit] is
     * known, so the very first emission already holds every unread message - the new-messages
     * divider is captured from that first emission only (see [captureNewMessagesBoundary]).
     */
    private val messageLimit = MutableStateFlow<Int?>(null)

    // Size of the last emitted window. Below messageLimit means the whole chat is loaded, or a
    // bigger window is still on its way - either way there is nothing more to ask for yet.
    private val loadedMessageCount = MutableStateFlow(0)

    /** One page, or more when the unread messages alone don't fit into one. */
    private val initialMessageLimit: Deferred<Int> = viewModelScope.async {
        val unreadWindow = try {
            messageRepository.getOldestUnreadSendDate(chatId, isGroup)
                ?.let { messageRepository.getMessageCountSince(chatId, isGroup, it) + WINDOW_MARGIN }
                ?: 0
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loggingRepository.logWarning("ChatViewModel: Problem counting unread messages: ${e.message}")
            0
        }
        maxOf(PAGE_SIZE, unreadWindow)
    }

    private fun loadOlderMessages() {
        val limit = messageLimit.value ?: return
        if (loadedMessageCount.value < limit) return
        messageLimit.value = limit + PAGE_SIZE
    }

    /** Grows the window until [messageId] is inside it - for a search hit or a reply's original. */
    private fun loadMessagesUntil(messageId: String) {
        viewModelScope.launch {
            val sendDate = messageRepository.getMessageById(messageId)?.sendDate?.toLongOrNull() ?: return@launch
            val needed = messageRepository.getMessageCountSince(chatId, isGroup, sendDate) + WINDOW_MARGIN
            val initial = initialMessageLimit.await()
            messageLimit.update { maxOf(it ?: initial, needed) }
        }
    }

    /**
     * The loaded window plus the replied-to messages that fall outside it, so a reply to an old
     * message keeps its preview. Fetched on every window emission, which fires on any write to
     * messages - so an edited or deleted original still shows up in the preview.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val windowedMessagesFlow: Flow<Pair<List<Message>, Map<String, Message>>> =
        messageLimit
            .filterNotNull()
            .flatMapLatest { limit ->
                messageRepository.getMessagesByUserIdFlow(userId = chatId, gruppe = isGroup, limit = limit)
            }
            .map { messages ->
                loadedMessageCount.value = messages.size
                val loadedIds = messages.mapNotNullTo(HashSet()) { it.id }
                val missingReplyIds = messages.mapNotNull { it.answerId }.filter { it !in loadedIds }.distinct()
                val olderReplyTargets = if (missingReplyIds.isEmpty()) {
                    emptyMap()
                } else {
                    messageRepository.getMessagesByIds(missingReplyIds)
                        .mapNotNull { message -> message.id?.let { id -> id to message } }
                        .toMap()
                }
                messages to olderReplyTargets
            }
            // Read by the display items and by the shown users below, but queried once
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

    private val messageDisplayMapper = MessageDisplayMapper()

    /**
     * Name and picture of every user the loaded messages name. Subscribes again only when that set
     * of ids changes, not on every new message; the repository emits only on a real change.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val shownUsersFlow: Flow<Map<String, UserIdentity>> = windowedMessagesFlow
        .map { (messages, olderReplyTargets) ->
            (messages + olderReplyTargets.values).flatMapTo(mutableSetOf()) { it.shownUserIds() }
        }
        .distinctUntilChanged()
        .flatMapLatest { userIds -> userIdentityRepository.observeUsers(userIds) }

    /**
     * Transform message flow to display items with pre-resolved sender names.
     */
    private val messageDisplayItemsFlow: Flow<List<MessageDisplayItem>> =
        combine(
            windowedMessagesFlow,
            shownUsersFlow,
            flow {
                emit(if (isGroup) groupRepository.getGroupMembers(chatId) else emptyList())
            }
        ) { (messages, olderReplyTargets), users, groupMembers ->
            captureNewMessagesBoundary(messages)
            messageDisplayMapper.map(
                messages = messages,
                olderReplyTargets = olderReplyTargets,
                users = users,
                groupMembers = groupMembers,
                newMessagesBoundaryId = newMessagesBoundaryId.value,
                ownId = SessionCache.requireLoggedIn()?.userId.orEmpty(),
                nameText = userIdentityNameText(),
            )
        }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    /**
     * The people of this chat, suggested first when typing `@`: the group members and everyone the
     * group's event shows (creator, invited, answered), or the partner of a 1:1 chat. Never the own user.
     */
    private suspend fun loadMentionContextIds(): Set<String> {
        val ids = if (isGroup) {
            val memberIds = groupRepository.getGroupMembers(chatId).map { it.userId }
            val eventUserIds = eventRepository.getEventFlowForGroup(chatId).first()?.shownUserIds().orEmpty()
            (memberIds + eventUserIds).toSet()
        } else {
            setOf(chatId)
        }
        return ids - SessionCache.requireLoggedIn()?.userId.orEmpty()
    }

    private fun captureNewMessagesBoundary(messages: List<Message>) {
        if (newMessagesBoundaryComputed || messages.isEmpty()) return
        newMessagesBoundaryComputed = true
        val idx = messages.indexOfLast { !it.myMessage && !it.readByMe }
        newMessagesBoundaryId.value = if (idx >= 0) messages[idx].id else null
    }




    init {
        // Start loading messages once the initial window size is known. max() because a
        // loadMessagesUntil() (search jump) may already have asked for a bigger one.
        viewModelScope.launch {
            val initial = initialMessageLimit.await()
            messageLimit.update { maxOf(it ?: 0, initial) }
        }

        // Chat partner (user or group) for the top bar, kept up to date from the database.
        viewModelScope.launch {
            val chatPartnerFlow = if (isGroup) {
                combine(
                    groupRepository.getGroupByIdFlow(chatId),
                    eventRepository.getEventFlowForGroup(chatId)
                ) { group, event ->
                    group?.toChatListItem(hasEvent = event != null)
                }
            } else {
                combine(
                    userRepository.getUserByIdFlow(chatId),
                    userRepository.onlineFriendIdsFlow
                ) { user, onlineFriendIds ->
                    user?.toChatListItem(isOnline = chatId in onlineFriendIds)
                }
            }
            chatPartnerFlow.collectLatest { partner ->
                _state.update { it.copy(chatPartner = partner) }
            }
        }

        viewModelScope.launch {
            val contextIds = loadMentionContextIds()
            _state.update { it.copy(mentionContextIds = contextIds) }
        }

        viewModelScope.launch {
            settingsRepository.getUseMdFlow()
                .catch { exception ->
                    loggingRepository.logWarning("ChatViewModel: Problem getting MD preference: ${exception.message}")
                }
                .collect { value ->
                    _state.update { it.copy(markdownEnabled = value) }
                }
        }

        viewModelScope.launch {
            settingsRepository.getQuickReactionsFlow()
                .catch { exception ->
                    loggingRepository.logWarning("ChatViewModel: Problem getting quick reactions: ${exception.message}")
                }
                .collect { value ->
                    _state.update { it.copy(quickReactions = value) }
                }
        }

        // load draft
        viewModelScope.launch {
            settingsRepository.getDraft(
                chatId = chatId,
                group = isGroup
            )
                .catch { exception ->
                    loggingRepository.logWarning("ChatViewModel: Problem getting draft: ${exception.message}")
                }
                .collect { value ->
                    if(value != null && (state.value.sendContent as? TextContent)?.textMessage?.text.isNullOrEmpty()){
                        updateSendContent(TextContent(TextFieldValue(value)))
                    }
                }
        }


        //Set messages read on start
        //setAllMessagesRead() Automatically on list change

        //Set all messages read on app resumed. Also the retry hook for a read the server never
        //accepted: nothing is written locally any more, so a failed attempt leaves the list
        //unchanged and the messages collector below has no new emission to react to.
        viewModelScope.launch {
            AppLifecycleManager.appResumedEvent.collectLatest {
                if (SessionCache.isLoggedIn() && hasUnreadMessages()) {
                    setAllMessagesRead()
                }
            }
        }

        //Set all messages read when this chat comes back on screen (e.g. back from ChatDetails).
        //The collector below only marks read while the chat is visible, so messages that arrived
        //meanwhile would otherwise stay unread until the next list change or app resume.
        viewModelScope.launch {
            OpenChatTracker.current.collectLatest { openChat ->
                if (openChat != OpenChatTracker.OpenChat(chatId, isGroup)) return@collectLatest
                if (!OpenChatTracker.isSeenAfterConfirmDelay(chatId = chatId, isGroup = isGroup)) return@collectLatest
                if (SessionCache.isLoggedIn() && hasUnreadMessages()) {
                    setAllMessagesRead()
                }
            }
        }

        // Messages: keep displayItems in ChatState up to date, and set all messages read on change
        viewModelScope.launch {
            messageDisplayItemsFlow.collectLatest { displayItems ->
                _state.update { it.copy(displayItems = displayItems) }

                if (AppLifecycleManager.isAppInForeground) {
                    // A message landing while the user is leaving the app (home swipe still
                    // running, activity not yet paused) must not be marked read unseen - wait,
                    // then require the chat to still be on screen. A newer emission cancels the
                    // wait (collectLatest), and an abandoned read is retried by appResumedEvent.
                    delay(OpenChatTracker.SEEN_CONFIRM_DELAY)
                    if (!OpenChatTracker.isSeen(chatId = chatId, isGroup = isGroup)) return@collectLatest

                    val messageItems = displayItems.filterIsInstance<MessageDisplayItem.MessageItem>()
                    // Messages come back newest-first (see MessageDao.getMessagesByUserIdFlow),
                    // so the newest is the first MessageItem, not the last.
                    val newestMessageId = messageItems.firstOrNull()?.message?.id
                    val hasUnread = messageItems.any { !it.message.readByMe }
                    // lastMarkedReadMessageId is advanced by setAllMessagesRead() itself, and only
                    // if the server accepted - so a sync that brings this chat back unread retries
                    // instead of being swallowed by the guard.
                    if (hasUnread && newestMessageId != markReadGuard.lastMarkedReadMessageId) {
                        setAllMessagesRead()
                    }
                }
            }
        }
        observeVoiceRecording()

        //println("ChatViewModel Incoming Data: ${IncomingDataManager.sharedText.value}")
        if(IncomingDataManager.isNewDataAvailable()){
            if (IncomingDataManager.isNewImageDataAvailable()) {
                val sharedImages = IncomingDataManager.sharedImages.value ?: emptyList()
                viewModelScope.launch {
                    stageImages(sharedImages, text = TextFieldValue(IncomingDataManager.sharedText.value ?: ""))
                    IncomingDataManager.clearAllData()
                }
            } else {
                updateSendContent(TextContent(TextFieldValue(IncomingDataManager.sharedText.value ?: "")))
                IncomingDataManager.updateText(null)
            }
        }

    }

    override fun onCleared() {
        super.onCleared()
        saveDraft()
        voiceRecording.release()
        voicePlayback.release()
    }

}

/** Size of the copy a chat image is sent as by default (without full quality). */
private const val CHAT_IMAGE_TARGET_BYTES = 700_000

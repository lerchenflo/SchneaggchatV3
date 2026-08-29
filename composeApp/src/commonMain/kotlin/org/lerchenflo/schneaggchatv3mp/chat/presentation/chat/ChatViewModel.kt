package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ismoy.imagepickerkmp.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.picker.GalleryPhotoResult
import io.github.lerchenflo.voicemessages.VoiceRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.VOICEMSG_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.toChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageDisplayItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.SenderInfo
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.SendMessageContent.TextContent
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.PollVoteRequest
import org.lerchenflo.schneaggchatv3mp.settings.data.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.utilities.AudioManager
import org.lerchenflo.schneaggchatv3mp.utilities.AudioPlayer
import org.lerchenflo.schneaggchatv3mp.utilities.IncomingDataManager
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.PlaybackProgress
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.getAudioBytes
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.message_too_long
import kotlin.time.TimeSource

class ChatViewModel(
    val chatId: String,
    val isGroup: Boolean,
    private val appRepository: AppRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val settingsRepository: SettingsRepository,
    private val navigator: Navigator,
    private val loggingRepository: LoggingRepository,
    private val pictureManager: PictureManager,
    private val permissionsManager: PermissionManager,
    private val audioManager: AudioManager,
    private val applicationScope: ApplicationScope
): ViewModel() {

    companion object {
        private const val MAX_VOICE_MSG_TIME = 2*60*1000L
    }

    private var voiceRecorder: VoiceRecorder? = null // Object for Audio Recording
    private var recordingTickerJob: Job? = null // live elapsed-time updates + auto-stop while recording
    private var audioPlayer: AudioPlayer = AudioPlayer(isDesktop = isDesktop())

    private val _state = MutableStateFlow(
        ChatState(
            chatId = chatId,
            isGroup = isGroup,
            isDesktop = isDesktop(),
            maxVoiceMsgTime = MAX_VOICE_MSG_TIME
        )
    )
    val state: StateFlow<ChatState> = _state.asStateFlow()

    /**
     * Stable flow reference, kept OUT of ChatState on purpose: playback position ticks ~5x/sec
     * while a voice message plays, and folding that into the state value would re-emit (and
     * recompose) the whole screen on every tick instead of just the audio player.
     */
    val playbackProgress: StateFlow<PlaybackProgress> = audioPlayer.playbackProgress

    private var newMessagesBoundaryComputed = false
    private val newMessagesBoundaryId = MutableStateFlow<String?>(null)

    // Guards setAllMessagesRead(): without this it re-runs (and writes to the DB) on every
    // single messageDisplayItemsFlow emission, including ones with nothing unread - which then
    // re-emits the messages flow and reprocesses the whole list again.
    private var lastMarkedReadMessageId: String? = null

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

            is ChatAction.OnImagesSelected -> onImagesSelected(action.results)
            is ChatAction.OnCreatePoll -> createPollMessage(action.poll)

            ChatAction.OnStartRecording -> startRecording()
            ChatAction.OnStopRecording -> stopRecording()
            ChatAction.OnDiscardRecording -> {
                stopRecording()
                updateSendContent(TextContent(TextFieldValue("")))
            }

            is ChatAction.OnMessageAction -> onMessageAction(action.action)
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

    fun setAllMessagesRead() {

        val userId = SessionCache.requireLoggedIn()?.userId ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val messageIds = state.value.displayItems
                .filterIsInstance<MessageDisplayItem.MessageItem>()
                .filter { item -> !item.message.readByMe }
                .mapNotNull { it.message.id }
                .map { NotificationManager.NotiId.HexString(it).asInt }

            NotificationManager.removeMessageNotifications(messageIds)
        }

        CoroutineScope(Dispatchers.IO).launch {
            appRepository.setAllChatMessagesRead(
                ownId = userId,
                chatId,
                isGroup,
                getCurrentTimeMillisString()
            )
        }
    }

    private fun saveDraft(){
        CoroutineScope(Dispatchers.IO).launch {
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

                require(message.textMessage.text.length < 10000) {
                    runBlocking {
                        SnackbarManager.showMessage(getString(Res.string.message_too_long))
                    }
                    return
                }
            }
            is SendMessageContent.ImageContent -> {
                if (message.images.isEmpty()) return

                require(message.text.text.length < 10000) {
                    runBlocking {
                        SnackbarManager.showMessage(getString(Res.string.message_too_long))
                    }
                    return
                }

                if (message.text.text.isBlank()) {
                    message.text = message.text.copy(text = "") //Clear string content if only linebreak / tab
                }
            }
            is SendMessageContent.AudioContent -> {
                require(message.audioPath.isNotEmpty()) {return}
                require(!message.isRecording) {return}
            }

        }


        // Sends run in the application scope (not viewModelScope) so an in-flight send survives
        // leaving the chat screen
        when (message) {
            is TextContent -> {
                applicationScope.launch {
                    appRepository.sendMessage(
                        empfaenger = chatId,
                        gruppe = isGroup,
                        content = AppRepository.MessageContent.TextContent(message.textMessage.text),
                        answerid = replyTo?.id,
                        messageId = null,
                        ownId = ownId,
                    )
                }
            }

            is SendMessageContent.ImageContent -> {
                message.images.forEachIndexed { index, image ->
                    applicationScope.launch {
                        appRepository.sendMessage(
                            empfaenger = chatId,
                            gruppe = isGroup,
                            content = AppRepository.MessageContent.ImageContent(
                                image = image,
                                text = if (index == 0) message.text.text else ""
                            ),
                            answerid = replyTo?.id,
                            messageId = null,
                            ownId = ownId
                        )
                    }
                }
            }

            is SendMessageContent.AudioContent -> {
                applicationScope.launch {
                    appRepository.sendMessage(
                        empfaenger = chatId,
                        gruppe = isGroup,
                        content = AppRepository.MessageContent.AudioContent(
                            audio = getAudioBytes(
                                audioManager.getRecordingPath(message.audioPath.substringAfterLast('/'))
                            )
                        ),
                        answerid = replyTo?.id,
                        messageId = null,
                        ownId = ownId
                    )
                }
            }
        }

        updateReplyMessage(null)
        updateSendContent(TextContent(TextFieldValue("")))
    }

    private fun onImagesSelected(results: List<GalleryPhotoResult>) {

        CoroutineScope(Dispatchers.Default).launch {
            val byteArrays = results.map { it.loadBytes() }
            val downscaledImages = byteArrays.map {
                pictureManager.downscaleImage(it)
            }

            updateSendContent(SendMessageContent.ImageContent(
                images = downscaledImages,
                text = (state.value.sendContent as? TextContent)?.textMessage ?: TextFieldValue("")
            ))
        }

    }

    private fun createPollMessage(poll: NetworkUtils.PollCreateRequest) {

        val ownId = SessionCache.requireLoggedIn()?.userId ?: return
        val replyMessage = state.value.replyMessage

        applicationScope.launch {
            appRepository.sendMessage(
                empfaenger = chatId,
                gruppe = isGroup,
                content = AppRepository.MessageContent.PollContent(poll),
                answerid = replyMessage?.id,
                messageId = null,
                ownId = ownId
            )

            updateReplyMessage(null)
        }
    }

    private fun deleteMessage(message: Message) {
        viewModelScope.launch {
            if (message.id == null) {
                appRepository.deleteLocalMessage(message.localPK)
                println("Offline message deleted")
            } else {
                appRepository.deleteMessage(message.id!!)
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
                    appRepository.votePoll(
                        pollVoteRequest = PollVoteRequest(
                            messageId = action.messageId,
                            id = action.optionId,
                            text = null,
                            maxAllowedAnswers = null,
                            selected = action.checked
                        ),
                        ownId = ownId
                    )
                }
            }

            is MessageAction.AddCustomPollOption -> {
                viewModelScope.launch {
                    appRepository.votePoll(
                        ownId = ownId,
                        pollVoteRequest = PollVoteRequest(
                            messageId = action.messageId,
                            id = null,
                            text = action.text,
                            maxAllowedAnswers = action.maxAnswers,
                            selected = true
                        )
                    )
                }
            }
            is MessageAction.DeletePollOption -> {
                viewModelScope.launch {
                    appRepository.deletePollOption(
                        ownId = ownId,
                        messageId = action.messageId,
                        optionId = action.optionId
                    )
                }
            }
            is MessageAction.PlayAudio -> {
                val filePath = action.audioPath
                //val playPath = (if (filePath.startsWith("/")) "file:/$filePath" else filePath).trim()
                //println("DEBUG: Checking file permissions for path (viewmodel)")
                //val playPath = audioManager.copyToCache(filePath)
                playAudio(
                    messageId = action.messageId,
                    path = filePath
                )
                //action.playbackProgress = audioPlayer.playbackProgress
            }
            is MessageAction.PauseAudio -> {
                pauseAudio()
            }
            is MessageAction.SeekAudio -> {
                seekAudio(action.position)
            }
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
                    appRepository.reactToMessage(
                        action.messageId,
                        action.reaction
                    )
                }
            }
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

            appRepository.editMessage(
                message = message,
                newContent = content.textMessage.text
            )

            println("Edit: Newcontent: ${content.textMessage}")

            //Clear text after editing message
            onMessageAction(MessageAction.CancelEditMessage)
        }
    }




    private fun onBackClick() {
        saveDraft()

        runBlocking {
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

    // Audio Recording / Playback
    private fun startRecording() {
        viewModelScope.launch {
            try {
                // Stop any playing audio before starting recording to avoid audio session conflicts
                audioPlayer.stopAudio()
                // Give the audio session a moment to fully release
                delay(100)

                val permission = permissionsManager.checkMicrophonePermission()
                println("startRecording - Permission: $permission")
                if (permission != PermissionState.GRANTED) {
                    println("Microphone permission not granted; requesting now.")
                    val result = permissionsManager.requestMicrophonePermission()
                    if (result != PermissionState.GRANTED) {
                        return@launch
                    }
                }

                val filename = getCurrentTimeMillisString() + VOICEMSG_FILE_NAME
                val path = audioManager.getRecordingPath(filename)

                println("Starting recording at path: $path")
                val recorder = VoiceRecorder()
                voiceRecorder = recorder
                recorder.start(path)
                // Store the bare filename (not the absolute path) so it stays valid across app
                // launches; it is resolved back to an absolute path at play/read time.
                updateSendContent(SendMessageContent.AudioContent(
                    audioPath = filename,
                    duration =  0L,
                    isRecording = true
                ))

                // Live elapsed-time updates + auto-stop, replacing the old push-based recording listener
                val startMark = TimeSource.Monotonic.markNow()
                recordingTickerJob = viewModelScope.launch {
                    while (isActive) {
                        delay(200)
                        val elapsedMs = startMark.elapsedNow().inWholeMilliseconds
                        (state.value.sendContent as? SendMessageContent.AudioContent)?.let { audio ->
                            updateSendContent(audio.copy(duration = elapsedMs))
                        }
                        if (elapsedMs > MAX_VOICE_MSG_TIME) { // maximum of [2 min] for audio recordings
                            stopRecording()
                        }
                    }
                }
            } catch (e: Exception) {
                // log full details to help debugging
                loggingRepository.logWarning("Failed to start recording: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        viewModelScope.launch {
            try {
                recordingTickerJob?.cancel()
                recordingTickerJob = null
                voiceRecorder?.stop()
                voiceRecorder = null
                (state.value.sendContent as? SendMessageContent.AudioContent)?.let { audio ->
                    // Measure the actual recorded duration from the file instead of trusting the
                    // ticker's elapsed time, which can drift slightly from the real audio length.
                    val measuredDuration = try {
                        getAudioDuration(audio.audioPath)
                    } catch (e: Exception) {
                        audio.duration
                    }
                    updateSendContent(audio.copy(isRecording = false, duration = measuredDuration))
                }
                println("Recording stopped")
            } catch (e: Exception) {
                loggingRepository.logWarning("Failed to stop recording: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun playAudio(messageId: String, path: String) {
        viewModelScope.launch {
            // Callers pass a bare filename; resolve it to the current absolute path here so
            // playback is immune to iOS container-UUID changes (tolerant of legacy absolute
            // paths via substringAfterLast).
            audioPlayer.playAudio(
                messageId = messageId,
                path = audioManager.getRecordingPath(path.substringAfterLast('/'))
            )
        }
    }

    private fun pauseAudio() {
        viewModelScope.launch {
            audioPlayer.pauseAudio()
        }
    }

    private fun seekAudio(position: Long) {
        viewModelScope.launch {
            audioPlayer.seekTo(position)
        }
    }

    private suspend fun getAudioDuration(path: String): Long {
        // Use the same path normalization as playAudio
        val normalizedPath = audioManager.getRecordingPath(path.substringAfterLast('/'))
        return audioManager.getMediaDuration(normalizedPath)
    }




    private val messageDisplayMapper = MessageDisplayMapper(
        getProfilePicFilePath = { userId -> pictureManager.getProfilePicFilePath(userId, false) }
    )

    // Only the id -> displayName projection, not the full User list: getAllUsersFlow() emits on
    // ANY row change in `users` - including presence/lastSeen writes, which arrive constantly
    // over the socket and have nothing to do with this chat. distinctUntilChanged() on the
    // narrowed projection means only an actual display-name change reprocesses the message list.
    private val senderNamesFlow: Flow<Map<String, String>> = userRepository.getAllUsersFlow()
        .map { users -> users.associate { it.id to it.displayName } }
        .distinctUntilChanged()

    /**
     * Transform message flow to display items with pre-resolved sender names.
     */
    private val messageDisplayItemsFlow: Flow<List<MessageDisplayItem>> =
        combine(
            messageRepository.getMessagesByUserIdFlow(
                userId = chatId,
                gruppe = isGroup
            ),
            senderNamesFlow,
            flow {
                emit(if (isGroup) groupRepository.getGroupMembers(chatId) else emptyList())
            }
        ) { messages, senderNames, groupMembers ->
            captureNewMessagesBoundary(messages)
            messageDisplayMapper.map(messages, senderNames, groupMembers, newMessagesBoundaryId.value)
        }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    private fun captureNewMessagesBoundary(messages: List<Message>) {
        if (newMessagesBoundaryComputed || messages.isEmpty()) return
        newMessagesBoundaryComputed = true
        val idx = messages.indexOfLast { !it.myMessage && !it.readByMe }
        newMessagesBoundaryId.value = if (idx >= 0) messages[idx].id else null
    }




    init {
        // Chat partner (user or group) for the top bar, kept up to date from the database.
        viewModelScope.launch {
            val chatPartnerFlow = if (isGroup) {
                groupRepository.getGroupFlow(chatId).map { group ->
                    group?.toChatListItem()
                }
            } else {
                combine(
                    userRepository.getUserFlow(chatId),
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
            settingsRepository.getUsemd()
                .catch { exception ->
                    loggingRepository.logWarning("ChatViewModel: Problem getting MD preference: ${exception.message}")
                }
                .collect { value ->
                    _state.update { it.copy(markdownEnabled = value) }
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
                    if(value != null && (state.value.sendContent as TextContent).textMessage.text.isNotEmpty()){
                        updateSendContent(TextContent(TextFieldValue(value)))
                    }
                }
        }


        //Set messages read on start
        //setAllMessagesRead() Automatically on list change

        //Set all messages read on app resumed
        viewModelScope.launch {
            AppLifecycleManager.appResumedEvent.collectLatest {
                if (SessionCache.isLoggedIn()) {
                    setAllMessagesRead()
                }
            }
        }

        // Messages: keep displayItems in ChatState up to date, and set all messages read on change
        viewModelScope.launch {
            messageDisplayItemsFlow.collectLatest { displayItems ->
                _state.update { it.copy(displayItems = displayItems) }

                if (AppLifecycleManager.isAppInForeground) {
                    val messageItems = displayItems.filterIsInstance<MessageDisplayItem.MessageItem>()
                    // Messages come back newest-first (ORDER BY sendDate DESC), so the newest is
                    // the first MessageItem, not the last.
                    val newestMessageId = messageItems.firstOrNull()?.message?.id
                    val hasUnread = messageItems.any { !it.message.readByMe }
                    if (hasUnread && newestMessageId != lastMarkedReadMessageId) {
                        lastMarkedReadMessageId = newestMessageId
                        setAllMessagesRead()
                    }
                }
            }
        }
        // Initialize audio
        viewModelScope.launch {
            audioManager.initializeAudio()
        }

        //println("ChatViewModel Incoming Data: ${IncomingDataManager.sharedText.value}")
        if(IncomingDataManager.isNewDataAvailable()){
            if (IncomingDataManager.isNewImageDataAvailable()) {
                val sharedImages = IncomingDataManager.sharedImages.value ?: emptyList()
                viewModelScope.launch {
                    val downscaledImages = sharedImages.map { imageBytes ->
                        pictureManager.downscaleImage(imageBytes)
                    }
                    updateSendContent(SendMessageContent.ImageContent(
                        images = downscaledImages,
                        text = TextFieldValue(IncomingDataManager.sharedText.value ?: "")
                    ))
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
    }

    private fun isDesktop(): Boolean{
        return appRepository.appVersion.isDesktop()
    }

}

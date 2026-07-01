package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.domain.models.GalleryPhotoResult
import io.github.lerchenflo.voicemessages.VoiceRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.VOICEMSG_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageDisplayItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ChatViewModel.SendMessageContent.TextContent
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
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.TimeSource

class ChatViewModel(
    private val appRepository: AppRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val settingsRepository: SettingsRepository,
    private val globalViewModel: GlobalViewModel,
    private val navigator: Navigator,
    private val loggingRepository: LoggingRepository,
    private val pictureManager: PictureManager,
    private val permissionsManager: PermissionManager,
    private val audioManager: AudioManager
): ViewModel() {

    companion object {
        private const val INITIAL_MESSAGE_COUNT = 12
        private const val MAX_VOICE_MSG_TIME = 2*60*1000L
    }

    private var voiceRecorder: VoiceRecorder? = null // Object for Audio Recording
    private var recordingTickerJob: Job? = null // live elapsed-time updates + auto-stop while recording
    private var audioPlayer: AudioPlayer = AudioPlayer(isDesktop = isDesktop())

    // chat id und group bool wörrend im init oamol glada
    // Damit leabt des o solang wie es viewmodel o wenn des im globalviewmodel scho tötet worra isch
    var chatId: String = ""
    var isGroup: Boolean = false


    var markdownEnabled by mutableStateOf(false)
        private set



    var editMessage by mutableStateOf<Message?>(null)
        private set
    fun updateEditMessage(newValue: Message?) {
        editMessage = newValue
    }


    sealed class SendMessageContent {
        data class TextContent(val textMessage: TextFieldValue) : SendMessageContent()
        data class ImageContent(val images: List<ByteArray>, var text: TextFieldValue) : SendMessageContent()
        data class AudioContent(
            val audioPath: String,
            val duration: Long,
            val isRecording: Boolean = false
        ) : SendMessageContent()
    }

    // In your ViewModel
    private val _currentSendContent = MutableStateFlow<SendMessageContent>(SendMessageContent.TextContent(TextFieldValue("")))
    val currentSendContent: StateFlow<SendMessageContent> = _currentSendContent.asStateFlow()

    fun updateSendContent(content: SendMessageContent) {
        _currentSendContent.value = content
    }

    var replyMessage by mutableStateOf<Message?>(null)
        private set

    fun updateReplyMessage(message: Message?) {
        replyMessage = message
    }

    private val _isLoadingOlderMessages = MutableStateFlow(false)
    val isLoadingOlderMessages: StateFlow<Boolean> = _isLoadingOlderMessages

    private val _shouldLoadAllMessages = MutableStateFlow(false)

    private var newMessagesBoundaryComputed = false
    private val newMessagesBoundaryId = MutableStateFlow<String?>(null)

    fun setAllMessagesRead() {

        val userId = SessionCache.requireLoggedIn()?.userId ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val messageIds = messageDisplayState.value
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

    fun saveDraft(){
        CoroutineScope(Dispatchers.IO).launch {
            // todo wenn bild oder sprachnachricht oder so künnt ma des speichera
            if(currentSendContent.value is SendMessageContent.TextContent) { // schoua ob es textfeld leer isch
                settingsRepository.saveDraft(
                    chatId = chatId,
                    group = isGroup,
                    string = (currentSendContent.value as SendMessageContent.TextContent).textMessage.text
                )
            }
        }
    }

    fun sendMessage(message: SendMessageContent, replyTo: Message? = null) {

        val ownId = SessionCache.requireLoggedIn()?.userId ?: return

        //Validation of message
        when (message) {
            is SendMessageContent.TextContent -> {
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


        when (message) {
            is SendMessageContent.TextContent -> {
                globalViewModel.viewModelScope.launch {
                    appRepository.sendMessage(
                        empfaenger = globalViewModel.selectedChat.value.id,
                        gruppe = globalViewModel.selectedChat.value.isGroup,
                        content = AppRepository.MessageContent.TextContent(message.textMessage.text),
                        answerid = replyTo?.id,
                        messageId = null,
                        ownId = ownId,
                    )
                }
            }

            is SendMessageContent.ImageContent -> {
                message.images.forEachIndexed { index, image ->
                    globalViewModel.viewModelScope.launch {
                        appRepository.sendMessage(
                            empfaenger = globalViewModel.selectedChat.value.id,
                            gruppe = globalViewModel.selectedChat.value.isGroup,
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
                globalViewModel.viewModelScope.launch {
                    appRepository.sendMessage(
                        empfaenger = globalViewModel.selectedChat.value.id,
                        gruppe = globalViewModel.selectedChat.value.isGroup,
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
        updateSendContent(SendMessageContent.TextContent(TextFieldValue("")))
    }

    fun onImagesSelected(results: List<GalleryPhotoResult>) {

        CoroutineScope(Dispatchers.Default).launch {
            val byteArrays = results.map { it.loadBytes() }
            val downscaledImages = byteArrays.map {
                pictureManager.downscaleImage(it)
            }

            updateSendContent(SendMessageContent.ImageContent(
                images = downscaledImages,
                text = (currentSendContent.value as? SendMessageContent.TextContent)?.textMessage ?: TextFieldValue("")
            ))
        }

    }

    fun createPollMessage(poll: NetworkUtils.PollCreateRequest) {

        val ownId = SessionCache.requireLoggedIn()?.userId ?: return

        globalViewModel.viewModelScope.launch {
            appRepository.sendMessage(
                empfaenger = chatId,
                gruppe = isGroup,
                content = AppRepository.MessageContent.PollContent(poll),
                answerid = replyMessage?.id,
                messageId = null,
                ownId = ownId
            )

            replyMessage = null
        }
    }

    fun deleteMessage(message: Message) {
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
     * Centralized action handler for all message-level user interactions.
     * Composables only need a single `onAction: (MessageAction) -> Unit` callback.
     */
    fun onAction(action: MessageAction) {

        val ownId = SessionCache.requireLoggedIn()?.userId ?: return

        when (action) {
            is MessageAction.VotePoll -> {
                viewModelScope.launch {
                    appRepository.votePoll(
                        pollVoteRequest = PollVoteRequest(
                            messageId = action.messageId,
                            id = action.optionId,
                            text = null,
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
                            selected = true
                        )
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
                editMessage = action.message
                updateSendContent(TextContent(TextFieldValue(action.message.content)))
            }
            MessageAction.CancelEditMessage -> {
                editMessage = null
                updateSendContent(TextContent(TextFieldValue("")))
                //println("Update message sendtext to empty")
            }

            is MessageAction.ReplyToMessage -> updateReplyMessage(action.message)
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

    fun editMessage(message: Message?, content: SendMessageContent) {
        viewModelScope.launch {

            if (content !is SendMessageContent.TextContent) return@launch
            if (message == null) return@launch

            //Block empty edit
            if (content.textMessage.text.isBlank()) return@launch

            appRepository.editMessage(
                message = message,
                newContent = content.textMessage.text
            )

            println("Edit: Newcontent: ${content.textMessage}")

            //Clear text after editing message
            onAction(MessageAction.CancelEditMessage)
        }
    }




    fun onBackClick() {
        saveDraft()
        viewModelScope.launch {
            /*

navigator.navigate(Route.ChatSelector, Navigator.NavigationOptions(
                removeAllScreensByRoute = listOf(Route.ChatDetails, Route.Chat)))
            */

        }

        runBlocking {
            navigator.navigateBack(navigationOptions = Navigator.NavigationOptions(
                removeAllScreensByRoute = listOf(Route.ChatDetails, Route.Chat)
            ))
        }
    }

    fun onChatDetailsClick() {
        viewModelScope.launch {
            navigator.navigate(Route.ChatDetails)
        }
    }

    // Audio Recording / Playback
    fun startRecording() {
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
                        (currentSendContent.value as? SendMessageContent.AudioContent)?.let { audio ->
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

    fun stopRecording() {
        viewModelScope.launch {
            try {
                recordingTickerJob?.cancel()
                recordingTickerJob = null
                voiceRecorder?.stop()
                voiceRecorder = null
                (currentSendContent.value as? SendMessageContent.AudioContent)?.let { audio ->
                    updateSendContent(audio.copy(isRecording = false))
                }
                println("Recording stopped")
            } catch (e: Exception) {
                loggingRepository.logWarning("Failed to stop recording: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun playAudio(messageId: String, path: String) {
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

    fun pauseAudio() {
        viewModelScope.launch {
            audioPlayer.pauseAudio()
        }
    }

    fun seekAudio(position: Long) {
        viewModelScope.launch {
            audioPlayer.seekTo(position)
        }
    }

    fun getPlaybackProgress(): StateFlow<PlaybackProgress>{
        return audioPlayer.playbackProgress
    }

    suspend fun getAudioDuration(path: String): Long {
        // Use the same path normalization as playAudio
        val normalizedPath = audioManager.getRecordingPath(path.substringAfterLast('/'))
        return audioManager.getMediaDuration(normalizedPath)
    }

    fun getMAX_VOICE_MSG_TIME(): Long{
        return MAX_VOICE_MSG_TIME
    }





    private fun formatDate(date: LocalDate): String {
        return "${date.day}.${date.month.ordinal}.${date.year}"
    }

    @OptIn(ExperimentalTime::class)
    private fun Long.toLocalDate(): LocalDate {
        val instant = Instant.fromEpochMilliseconds(this)
        return instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    }




    /**
     * Transform message flow to display items with pre-resolved sender names.
     * Initially loads only recent messages, then switches to all messages.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val messageDisplayItemsFlow: Flow<List<MessageDisplayItem>> =
        globalViewModel.selectedChat
            .flatMapLatest { chat ->
                // Combine the load-all trigger with messages
                combine(
                    _shouldLoadAllMessages,
                    userRepository.getAllUsersFlow(),
                    if (chat.isGroup) {
                        flowOf(groupRepository.getGroupMembers(chat.id))
                    } else {
                        flowOf(emptyList())
                    }
                ) { shouldLoadAll, users, groupMembers ->
                    Triple(shouldLoadAll, users, groupMembers)
                }.flatMapLatest { (shouldLoadAll, users, groupMembers) ->
                    // Choose which message flow to use based on loading state
                    val messagesFlow = if (shouldLoadAll) {
                        messageRepository.getMessagesByUserIdFlow(
                            userId = chat.id,
                            gruppe = chat.isGroup
                        )
                    } else {
                        messageRepository.getMessagesByUserIdFlowPaged(
                            userId = chat.id,
                            gruppe = chat.isGroup,
                            pageSize = INITIAL_MESSAGE_COUNT,
                            offset = 0
                        )
                    }

                    // Combine with users and group members
                    combine(
                        messagesFlow,
                        flowOf(users),
                        flowOf(groupMembers)
                    ) { messages, userList, groupList ->
                        // Mark loading as complete once all messages are loaded
                        if (shouldLoadAll && _isLoadingOlderMessages.value) {
                            _isLoadingOlderMessages.value = false
                        }

                        captureNewMessagesBoundary(messages)
                        Triple(messages, userList, groupList)
                    }
                }.flowOn(Dispatchers.Default)
                    .flatMapLatest { (messages, users, groupMembers) ->
                        flowOf(processMessages(messages, users, groupMembers, newMessagesBoundaryId.value))
                    }
            }
            .flowOn(Dispatchers.Default)

    private fun captureNewMessagesBoundary(messages: List<Message>) {
        if (newMessagesBoundaryComputed || messages.isEmpty()) return
        newMessagesBoundaryComputed = true
        val idx = messages.indexOfLast { !it.myMessage && !it.readByMe }
        newMessagesBoundaryId.value = if (idx >= 0) messages[idx].id else null
    }

    private fun processMessages(
        messages: List<Message>,
        users: List<User>,
        groupMembers: List<GroupMember>,
        newMessagesBoundaryId: String?,
    ): List<MessageDisplayItem> {
        val userMap = users.associateBy { it.id }
        val groupMap = groupMembers.associateBy { it.userId }

        // Find the LATEST message each user has read
        // Map<ReaderId, MessageReader>
        val latestReadMap = mutableMapOf<String, MessageReader>()
        messages.forEach { message ->
            message.readers.forEach { reader ->
                val existing = latestReadMap[reader.readerId]
                // We update the map if this is the first time we see the user
                // or if this reader entry has a newer timestamp
                if (existing == null || reader.getReadDateAsLong() > existing.getReadDateAsLong()) {
                    latestReadMap[reader.readerId] = reader
                }
            }
        }

        // Group those latest readers by the Message ID where they stopped
        // Map<MessageId, List<MessageReader>>
        val readersToDisplayPerMessage = latestReadMap.values.groupBy { it.messageId }

        val displayItems = mutableListOf<MessageDisplayItem>()

        messages.forEachIndexed { index, message ->
            val currentDate = message.sendDate.toLongOrNull()?.toLocalDate()
            val nextDate = if (index + 1 < messages.size) {
                messages[index + 1].sendDate.toLongOrNull()?.toLocalDate()
            } else null

            val user = userMap[message.senderId]
            val senderName = user?.displayName ?: groupMap[message.senderId]?.memberName ?: "Unresolved Username"
            message.senderAsString = senderName
            val resolvedColor = groupMap[message.senderId]?.color ?: 0
            message.senderColor = resolvedColor

            val resolvedReaders = message.readers.associate { reader ->
                val readerUser = userMap[reader.readerId]
                reader.readerName = readerUser?.displayName ?: groupMap[reader.readerId]?.memberName ?: "Unknown"
                reader.readerPicture = pictureManager.getProfilePicFilePath(reader.readerId, false)
                reader.readerId to (reader.readerName ?: "Unknown")
            }

            val resolvedReactions = message.reactions.associate { reaction ->
                val reactUser = userMap[reaction.userId]
                reaction.userId to (reactUser?.displayName ?: groupMap[reaction.userId]?.memberName ?: "Unknown")
            }

            // Use the message's unique ID to look up the grouped readers
            val readersAtThisMessage = readersToDisplayPerMessage[message.id]
            if (!readersAtThisMessage.isNullOrEmpty()) {
                displayItems.add(
                    MessageDisplayItem.ReaderBar(
                        id = "readers_${message.id}",
                        readerList = readersAtThisMessage
                    )
                )
            }

            displayItems.add(
                MessageDisplayItem.MessageItem(
                    id = "msg_${message.localPK}",
                    message = message,
                    senderName = senderName,
                    senderColor = resolvedColor,
                    resolvedReaders = resolvedReaders,
                    resolvedReactions = resolvedReactions
                )
            )

            if (newMessagesBoundaryId != null && message.id == newMessagesBoundaryId) {
                displayItems.add(MessageDisplayItem.NewMessagesDivider)
            }

            if (currentDate != nextDate && currentDate != null) {
                displayItems.add(
                    MessageDisplayItem.DateDivider(
                        id = "divider_${currentDate}",
                        dateMillis = message.sendDate.toLong(),
                        dateString = formatDate(currentDate)
                    )
                )
            }
        }

        return displayItems
    }

    val messageDisplayState: StateFlow<List<MessageDisplayItem>> = messageDisplayItemsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )




    //Beim call vom init sind alle values initialisiert
    init {
        viewModelScope.launch {
            chatId = globalViewModel.selectedChat.value.id
            isGroup = globalViewModel.selectedChat.value.isGroup
        }

        viewModelScope.launch {
            settingsRepository.getUsemd()
                .catch { exception ->
                    loggingRepository.logWarning("ChatViewModel: Problem getting MD preference: ${exception.message}")
                }
                .collect { value ->
                    markdownEnabled = value
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
                    if(value != null && (currentSendContent.value as SendMessageContent.TextContent).textMessage.text.isNotEmpty()){
                        updateSendContent(SendMessageContent.TextContent(TextFieldValue(value)))
                    }
                }
        }

        // Trigger background loading after a short delay
        viewModelScope.launch {
            delay(700) // Give initial messages time to load and render
            _isLoadingOlderMessages.value = true
            _shouldLoadAllMessages.value = true
            println("loading all messages")
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

        //Set all messages read on message change
        viewModelScope.launch {
            messageDisplayState.collectLatest { displayItems ->
                if (displayItems.isNotEmpty() && AppLifecycleManager.isAppInForeground) {
                    setAllMessagesRead()
                }
            }
        }
        // Initialize audio
        viewModelScope.launch {
            audioManager.initializeAudio()
        }

        println("ChatViewModel Incoming Data: ${IncomingDataManager.sharedText.value}")
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
                updateSendContent(SendMessageContent.TextContent(TextFieldValue(IncomingDataManager.sharedText.value ?: "")))
                IncomingDataManager.updateText(null)
            }
        }

    }

    override fun onCleared() {
        super.onCleared()
        saveDraft()
        globalViewModel.onLeaveChat()
    }

    fun isDesktop(): Boolean{
        return appRepository.appVersion.isDesktop()
    }

}

package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hyochan.audio.AudioRecorderPlayer
import io.github.hyochan.audio.createAudioRecorderPlayer
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.domain.models.GalleryPhotoResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
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
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.settings.data.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.utilities.AudioManager
import org.lerchenflo.schneaggchatv3mp.utilities.AudioPlayer
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.PlaybackProgress
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisString
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
    }

    private var audioRecorderPlayer: AudioRecorderPlayer? = null // Object for Audio Recording / Playback
    private var audioPlayer: AudioPlayer = AudioPlayer()

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
        data class TextContent(val textMessage: String) : SendMessageContent()
        data class ImageContent(val imageMessage: ByteArray, val text: String) : SendMessageContent()
        data class AudioContent(
            val audioPath: String,
            val duration: Long,
            val isRecording: Boolean = false
        ) : SendMessageContent()
    }

    // In your ViewModel
    private val _currentSendContent = MutableStateFlow<SendMessageContent>(SendMessageContent.TextContent(""))
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

    fun setAllMessagesRead() {

        val userId = SessionCache.requireLoggedIn()?.userId ?: return

        CoroutineScope(Dispatchers.IO).launch {
            // Get message IDs from current chat that need notification removal
            val messageIdsString = messageDisplayState.value
                .filterIsInstance<MessageDisplayItem.MessageItem>()
                .filter { item -> !item.message.readByMe }
                .mapNotNull {
                    it.message.id
                }

            val messageIds = messageIdsString.map {
                NotificationManager.NotiId.HexString(it).asInt
            }

            if (messageIds.isNotEmpty()) {
                NotificationManager.removeNotifications(messageIds)
            }
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
                    string = (currentSendContent.value as SendMessageContent.TextContent).textMessage
                )
            }
        }
    }

    fun sendMessage(message: SendMessageContent, replyTo: Message? = null) {

        val ownId = SessionCache.requireLoggedIn()?.userId ?: return

        if (message is SendMessageContent.TextContent && message.textMessage.isBlank()) return
        if (message is SendMessageContent.ImageContent && message.images.isEmpty()) return

        when (message) {
            is SendMessageContent.TextContent -> {
                globalViewModel.viewModelScope.launch {
                    appRepository.sendMessage(
                        empfaenger = globalViewModel.selectedChat.value.id,
                        gruppe = globalViewModel.selectedChat.value.isGroup,
                        content = AppRepository.MessageContent.TextContent(message.textMessage),
                        answerid = replyTo?.id,
                        messageId = null,
                        ownId = ownId,
                    )
                    is SendMessageContent.TextContent -> AppRepository.MessageContent.TextContent(message.textMessage)
                    is SendMessageContent.AudioContent -> AppRepository.MessageContent.TextContent("Audio message not implemented") // TODO
                },
                answerid = replyTo?.id,
                messageId = null,
            )

            is SendMessageContent.ImageContent -> {
                message.images.forEachIndexed { index, image ->
                    globalViewModel.viewModelScope.launch {
                        appRepository.sendMessage(
                            empfaenger = globalViewModel.selectedChat.value.id,
                            gruppe = globalViewModel.selectedChat.value.isGroup,
                            content = AppRepository.MessageContent.ImageContent(
                                image = image,
                                text = if (index == 0) message.text else ""
                            ),
                            answerid = replyTo?.id,
                            messageId = null,
                            ownId = ownId
                        )
                    }
                }
            }
        }

        updateReplyMessage(null)
        updateSendContent(SendMessageContent.TextContent(TextFieldValue("")))
    }

    fun onImageSelected(results: List<GalleryPhotoResult>) {

        CoroutineScope(Dispatchers.Default).launch {
            val byteArrays = results.map { it.loadBytes() }
            val downscaledImages = byteArrays.map {
                pictureManager.downscaleImage(it)
            }

            updateSendContent(SendMessageContent.ImageContent(
                imageMessage = downscaled,
                text = (currentSendContent.value as? SendMessageContent.TextContent)?.textMessage ?: ""
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
                        pollVoteRequest = NetworkUtils.PollVoteRequest(
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
                        pollVoteRequest = NetworkUtils.PollVoteRequest(
                            messageId = action.messageId,
                            id = null,
                            text = action.text,
                            selected = true
                        )
                    )
                }
            }
            is MessageAction.DeleteMessage -> deleteMessage(action.message)
            is MessageAction.StartEditMessage -> {
                editMessage = action.message
                updateSendContent(SendMessageContent.TextContent(TextFieldValue(action.message.content)))
            }
            MessageAction.CancelEditMessage -> {
                editMessage = null
                updateSendContent(SendMessageContent.TextContent(TextFieldValue("")))
                //println("Update message sendtext to empty")
            }

            is MessageAction.ReplyToMessage -> updateReplyMessage(action.message)

        }
    }

    fun editMessage(message: Message?, content: SendMessageContent) {
        viewModelScope.launch {

            if (content !is SendMessageContent.TextContent) return@launch
            if (message == null) return@launch

            //Block empty edit
            if (content.textMessage.isBlank()) return@launch

            appRepository.editMessage(
                message = message,
                newContent = content.textMessage
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
            navigator.navigateBack(navigationOptions = Navigator.NavigationOptions(
                removeAllScreensByRoute = listOf(Route.ChatDetails, Route.Chat)
            ))

            */
            navigator.navigate(Route.ChatSelector, Navigator.NavigationOptions(
                removeAllScreensByRoute = listOf(Route.ChatDetails, Route.Chat)))
        }
    }

    fun onChatDetailsClick() {
        viewModelScope.launch {
            navigator.navigate(Route.ChatDetails)
        }
    }

    // Audio Recording / Playback
    fun initAudioRecorderPlayer() {
        // Try to create the recorder only when permission is already granted.
        viewModelScope.launch {
            audioRecorderPlayer = createAudioRecorderPlayer()
            //set not null audioRecoderPlayer for the audioPlayer
            audioPlayer.audioRecorderPlayer = audioRecorderPlayer


            /* only request microphone permission when actually starting a recording
            val permission = permissionsManager.checkMicrophonePermission()
            println("initAudioRecorderPlayer - Permission: $permission")
            if (permission == PermissionState.GRANTED) {
                if (audioRecorderPlayer == null) {
                    println("AudioRecorderPlayer created")
                }
            } else {
                println("Requesting microphone permission (init)")
                permissionsManager.requestMicrophonePermission()
                // don't block here waiting for UI; we'll re-check permission in startRecording()
            }

             */
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            try {
                val permission = permissionsManager.checkMicrophonePermission()
                println("startRecording - Permission: $permission")
                if (permission != PermissionState.GRANTED) {
                    println("Microphone permission not granted; requesting now.")
                    val result = permissionsManager.requestMicrophonePermission()
                    if (result != PermissionState.GRANTED) {
                        return@launch
                    }
                }

                // lazy-init the recorder if not present
                if (audioRecorderPlayer == null) {
                    audioRecorderPlayer = createAudioRecorderPlayer()
                    println("AudioRecorderPlayer lazy-created in startRecording()")
                }

                val filename = "audioRec_" + getCurrentTimeMillisString() + ".m4a"
                val path = audioManager.getRecordingPath(filename)
                
                println("Starting recording at path: $path")
                val result = audioRecorderPlayer!!.startRecording(path)
                println("Recording start result: $result")
                //updateSendContent(SendMessageContent.AudioContent(path, 0L))

                audioRecorderPlayer!!.addRecordingListener { recordBack ->
                    println("Current record time: ${recordBack.currentPosition}")
                    updateSendContent(SendMessageContent.AudioContent(
                        audioPath = path,
                        duration =  recordBack.currentPosition,
                        isRecording = true
                    ))
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
                val result = audioRecorderPlayer?.stopRecording()
                audioRecorderPlayer?.removeListeners()
                updateSendContent(SendMessageContent.AudioContent(
                    audioPath = (currentSendContent.value as SendMessageContent.AudioContent).audioPath,
                    duration =  (currentSendContent.value as SendMessageContent.AudioContent).duration,
                    isRecording = false
                ))
                println("Recording stopped: $result")
            } catch (e: Exception) {
                loggingRepository.logWarning("Failed to stop recording: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun playAudio() {
        viewModelScope.launch {
            if(currentSendContent.value is SendMessageContent.AudioContent){
                audioPlayer.playAudio(
                    messageId = "audio_record_tmp",
                    path = (currentSendContent.value as SendMessageContent.AudioContent).audioPath
                )
            }

        }
    }

    fun getPlaybackProgress(): StateFlow<PlaybackProgress>{
        return audioPlayer.playbackProgress
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

                        Triple(messages, userList, groupList)
                    }
                }.flowOn(Dispatchers.Default)
                    .flatMapLatest { (messages, users, groupMembers) ->
                        flowOf(processMessages(messages, users, groupMembers))
                    }
            }
            .flowOn(Dispatchers.Default)

    private fun processMessages(messages: List<Message>, users: List<User>, groupMembers: List<GroupMember>
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

            val senderName = userMap[message.senderId]?.name ?: groupMap[message.senderId]?.memberName ?: "Unresolved Username"
            message.senderAsString = senderName
            val resolvedColor = groupMap[message.senderId]?.color ?: 0
            message.senderColor = resolvedColor

            val resolvedReaders = message.readers.associate { reader ->
                reader.readerName = userMap[reader.readerId]?.name ?: groupMap[reader.readerId]?.memberName ?: "Unknown"
                reader.readerPicture = pictureManager.getProfilePicFilePath(reader.readerId, false)
                reader.readerId to (reader.readerName ?: "Unknown")
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
                    resolvedReaders = resolvedReaders
                )
            )

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
                    updateSendContent(SendMessageContent.TextContent(TextFieldValue(value?: "")))
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
        // Initialize audio recorder player
        viewModelScope.launch {
            audioManager.initializeAudio()
            initAudioRecorderPlayer()
        }
    }


}

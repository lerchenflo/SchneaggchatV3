package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageDisplayItem
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.settings.data.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisString
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager

class ChatViewModel(
    private val appRepository: AppRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val settingsRepository: SettingsRepository,
    private val globalViewModel: GlobalViewModel,
    private val navigator: Navigator,
    private val loggingRepository: LoggingRepository
): ViewModel() {

    companion object {
        private const val INITIAL_MESSAGE_COUNT = 12
    }

    var markdownEnabled by mutableStateOf(false)
        private set

    var editMessageId by mutableStateOf<String?>(null)

    var sendText by mutableStateOf(TextFieldValue(""))
        private set

    fun updatesendText(newValue: TextFieldValue) {
        sendText = newValue
    }

    var replyMessage by mutableStateOf<Message?>(null)
        private set

    fun updateReplyMessage(newValue: Message?) {
        replyMessage = newValue
    }

    // Track loading state
    private val _isLoadingOlderMessages = MutableStateFlow(false)
    val isLoadingOlderMessages: StateFlow<Boolean> = _isLoadingOlderMessages

    // Track if we should load all messages or just initial batch
    private val _shouldLoadAllMessages = MutableStateFlow(false)


    fun setAllMessagesRead() {

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
                globalViewModel.selectedChat.value.id,
                globalViewModel.selectedChat.value.isGroup,
                getCurrentTimeMillisString()
            )
        }
    }

    fun sendMessage(){
        if (sendText.text.isEmpty()) return

        if(editMessageId == null) {
            val content = sendText.text
            updatesendText(TextFieldValue(""))

            globalViewModel.viewModelScope.launch {
                appRepository.sendTextMessage(
                    empfaenger = globalViewModel.selectedChat.value.id,
                    gruppe = globalViewModel.selectedChat.value.isGroup,
                    content = content,
                    answerid = replyMessage?.id,
                )

                replyMessage = null
            }
        } else {
            editMessage()
            updatesendText(TextFieldValue(""))
            editMessageId = null
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

    fun editMessage() {
        viewModelScope.launch {
            appRepository.editMessage(
                messageId = editMessageId!!,
                newContent = sendText.text
            )
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

    fun onChatDetailsClick() {
        viewModelScope.launch {
            navigator.navigate(Route.ChatDetails)
        }
    }

    private fun formatDate(date: LocalDate): String {
        return "${date.dayOfMonth}.${date.monthNumber}.${date.year}"
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

    private fun processMessages(
        messages: List<Message>,
        users: List<User>,
        groupMembers: List<GroupMember>
    ): List<MessageDisplayItem> {
        val userMap = users.associateBy { it.id }
        val groupMap = groupMembers.associateBy { it.userId }

        val displayItems = mutableListOf<MessageDisplayItem>()

        messages.forEachIndexed { index, message ->
            val currentDate = message.sendDate.toLongOrNull()?.toLocalDate()
            val nextDate = if (index + 1 < messages.size) {
                messages[index + 1].sendDate.toLongOrNull()?.toLocalDate()
            } else null

            val senderName = userMap[message.senderId]?.name ?: groupMap[message.senderId]?.memberName ?: "Unresolved Username"
            val resolvedColor = groupMap[message.senderId]?.color ?: 0
            message.senderColor = resolvedColor

            displayItems.add(
                MessageDisplayItem.MessageItem(
                    id = "msg_${message.localPK}",
                    message = message,
                    senderName = senderName,
                    senderColor = resolvedColor
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
            settingsRepository.getUsemd()
                .catch { exception ->
                    loggingRepository.logWarning("ChatViewModel: Problem getting MD preference: ${exception.message}")
                }
                .collect { value ->
                    markdownEnabled = value
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
                if (SessionCache.isLoggedInValue()) {
                    setAllMessagesRead()
                }
            }
        }

        //Set all messages read on message change
        viewModelScope.launch {
            messageDisplayState.collectLatest { displayItems ->
                if (displayItems.isNotEmpty()) {
                    setAllMessagesRead()
                }
            }
        }
    }


}
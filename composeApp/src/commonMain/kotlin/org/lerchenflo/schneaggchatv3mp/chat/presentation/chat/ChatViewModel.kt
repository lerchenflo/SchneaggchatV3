package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
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

class ChatViewModel(
    private val appRepository: AppRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val settingsRepository: SettingsRepository,
    private val globalViewModel: GlobalViewModel,
    private val navigator: Navigator
): ViewModel() {


    var markdownEnabled by mutableStateOf(false)
        private set

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

    init {
        viewModelScope.launch {
            settingsRepository.getUsemd()
                .catch { exception ->
                    println("Problem getting MD preference: ${exception.printStackTrace()}")
                }
                .collect { value ->
                    markdownEnabled = value
                }

        }

        setAllMessagesRead()
    }

    fun setAllMessagesRead() {
        CoroutineScope(Dispatchers.IO).launch {
            appRepository.setAllChatMessagesRead(globalViewModel.selectedChat.value.id, globalViewModel.selectedChat.value.isGroup, getCurrentTimeMillisString())
        }
    }



    fun sendMessage(){

        if (sendText.text.isEmpty()) return

        //TODO: Do wechla bild und sunschwas
        val content = sendText.text
        updatesendText(TextFieldValue(""))

        //Im sharedviewmodel dassas ewig leabig isch
        globalViewModel.viewModelScope.launch {
            appRepository.sendTextMessage(
                empfaenger = globalViewModel.selectedChat.value.id,
                gruppe = globalViewModel.selectedChat.value.isGroup,
                content = content,
                answerid = replyMessage?.id,
            )

            replyMessage = null
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


    /**
     * Helper function to format a date for display.
     */
    private fun formatDate(date: LocalDate): String {
        return "${date.dayOfMonth}.${date.monthNumber}.${date.year}"
    }

    /**
     * Extension function to convert milliseconds to LocalDate.
     */
    @OptIn(ExperimentalTime::class)
    private fun Long.toLocalDate(): LocalDate {
        val instant = Instant.fromEpochMilliseconds(this)
        return instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    /**
     * Transform message flow to display items with pre-resolved sender names.
     * This uses combine() similar to ChatSelector's pattern for efficient data access.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val messageDisplayItemsFlow: Flow<List<MessageDisplayItem>> =
        globalViewModel.selectedChat
            .flatMapLatest { chat ->
                // Combine messages with users and optionally group members
                combine(
                    messageRepository.getMessagesByUserIdFlow(chat.id, chat.isGroup),
                    userRepository.getallusers(),
                    // Only load group members if this is a group chat
                    if (chat.isGroup) {
                        flowOf(groupRepository.getGroupMembers(chat.id))
                    } else {
                        flowOf(emptyList())
                    }
                ) { messages, users, groupMembers ->
                    // Build user lookup map (O(1) access) - similar to ChatSelector pattern
                    val userMap = users.associateBy { it.id }
                    val groupMap = groupMembers.associateBy { it.userId }

                    // Transform messages to display items
                    val displayItems = mutableListOf<MessageDisplayItem>()

                    // Process messages in order (Newest -> Oldest)
                    messages.forEachIndexed { index, message ->
                        val currentDate = message.sendDate.toLongOrNull()?.toLocalDate()
                        val nextDate = if (index + 1 < messages.size) {
                            messages[index + 1].sendDate.toLongOrNull()?.toLocalDate()
                        } else null

                        // Resolve sender name from user map
                        val senderName = userMap[message.senderId]?.name

                        // 1. Add message display item first (lower index = closer to bottom)
                        val resolvedColor = groupMap[message.senderId]?.color ?: 0
                        message.senderColor = resolvedColor // Set on message too for reply previews
                        
                        displayItems.add(
                            MessageDisplayItem.MessageItem(
                                id = "msg_${message.localPK}",
                                message = message,
                                senderName = senderName,
                                senderColor = resolvedColor
                            )
                        )

                        // 2. Add date divider if we've crossed into a new day (or it's the last message)
                        // Higher index = closer to top
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

                    displayItems
                }
            }
            .flowOn(Dispatchers.Default)

    /**
     * Expose as StateFlow so UI can collect easily and get a current value.
     * This replaces the old messagesState.
     */
    val messageDisplayState: StateFlow<List<MessageDisplayItem>> = messageDisplayItemsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Keep the old messagesState for backward compatibility during transition
    @OptIn(ExperimentalCoroutinesApi::class)
    val messagesFlow: Flow<List<Message>> =
        globalViewModel.selectedChat
            .flatMapLatest { chat ->
                messageRepository.getMessagesByUserIdFlow(chat.id, chat.isGroup)
            }
            .flowOn(Dispatchers.Default)

    // Expose as StateFlow so UI can collect easily and get a current value
    val messagesState: StateFlow<List<Message>> = messagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )




}
package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.uielements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageDisplayItem
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.DayDivider
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.NewMessagesDivider
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.ReaderBar
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.SystemMessageItem
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.systemEventText
import org.lerchenflo.schneaggchatv3mp.sharedUi.DATE_CHIP_FORMAT
import org.lerchenflo.schneaggchatv3mp.sharedUi.DateChip
import org.lerchenflo.schneaggchatv3mp.utilities.PlaybackProgress
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.new_messages
import kotlin.time.Duration.Companion.milliseconds

// How close (in items) to the oldest loaded item the user may scroll before older ones are loaded.
private const val LOAD_OLDER_THRESHOLD = 30

private fun List<MessageDisplayItem>.indexOfMessage(messageId: String): Int =
    indexOfFirst { it is MessageDisplayItem.MessageItem && it.message.id == messageId }

/**
 * The scrollable message list: opens scrolled to the unread divider (or a searched-for message
 * when [highlightMessageId] is set), and handles reply-preview jump-and-glow.
 */
@Composable
fun ChatMessageList(
    displayItems: List<MessageDisplayItem>,
    highlightMessageId: String?,
    ownId: String,
    chatId: String,
    useMarkdown: Boolean,
    quickReactions: List<String>,
    playbackProgress: StateFlow<PlaybackProgress>,
    onAction: (MessageAction) -> Unit,
    onLoadOlderMessages: () -> Unit,
    onLoadMessagesUntil: (messageId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var initialScrollDone by remember { mutableStateOf(false) }

    // Close enough to the newest message (the reversed list's index 0) to count as "at the bottom".
    val isAtBottom by remember { derivedStateOf { listState.firstVisibleItemIndex < 5 } }
    var newMessagesAvailable by remember { mutableStateOf(false) }
    var previousFirstItemId by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    if (displayItems.isNotEmpty()) {
        LaunchedEffect(displayItems.first()) {
            val newFirstItemId = displayItems.first().id

            if (!initialScrollDone) {
                //Opened from the message search - the jump effect below scrolls to the
                //searched message instead of the unread divider.
                if (highlightMessageId != null) {
                    initialScrollDone = true
                    previousFirstItemId = newFirstItemId
                    return@LaunchedEffect
                }

                val dividerIndex = displayItems.indexOfFirst { it is MessageDisplayItem.NewMessagesDivider }
                if (dividerIndex != -1) {
                    listState.scrollToItem(dividerIndex)
                    // Nudge the divider up from the very bottom edge towards the center of the screen.
                    val viewportHeight = listState.layoutInfo.viewportSize.height
                    listState.scrollToItem(dividerIndex, scrollOffset = -viewportHeight / 2)
                } else {
                    listState.animateScrollToItem(0)
                }
                initialScrollDone = true
            } else if (newFirstItemId != previousFirstItemId) {
                // A new item landed at the newest end of the list.
                if (isAtBottom) {
                    listState.animateScrollToItem(0)
                } else {
                    newMessagesAvailable = true
                }
            }
            previousFirstItemId = newFirstItemId
        }
    }

    // Clear the "new messages" fab once the user scrolls back down themselves.
    LaunchedEffect(Unit) {
        snapshotFlow { isAtBottom }.collect { atBottom ->
            if (atBottom) newMessagesAvailable = false
        }
    }

    // The list only holds the newest messages: ask for the next older batch when the user nears
    // the top (the reversed list's last index).
    val nearOldestLoaded by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val topIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            topIndex >= layoutInfo.totalItemsCount - LOAD_OLDER_THRESHOLD
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { nearOldestLoaded }.collect { nearTop ->
            if (nearTop) onLoadOlderMessages()
        }
    }

    // Id of the message that should briefly glow after jumping to it via a reply preview
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }

    // Runs in the composable's scope, not in an effect: the effect below restarts whenever
    // displayItems changes, which would cancel the glow before it is cleared again.
    fun jumpAndGlow(targetIndex: Int, messageId: String, animate: Boolean) {
        scope.launch {
            if (animate) listState.animateScrollToItem(targetIndex) else listState.scrollToItem(targetIndex)
            highlightedMessageId = messageId
            delay(1500.milliseconds)
            highlightedMessageId = null
        }
    }

    // Message to jump to once it is loaded: the searched message when opened from the chat
    // selector's message search, or a reply's original that lies above the loaded window. Keyed on
    // displayItems because messages stream in asynchronously - it may take a bigger window first.
    var pendingJumpId by remember(highlightMessageId) { mutableStateOf(highlightMessageId) }
    var loadRequestedFor by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pendingJumpId, displayItems) {
        val messageId = pendingJumpId ?: return@LaunchedEffect

        val targetIndex = displayItems.indexOfMessage(messageId)
        if (targetIndex == -1) {
            if (loadRequestedFor != messageId) {
                loadRequestedFor = messageId
                onLoadMessagesUntil(messageId)
            }
            return@LaunchedEffect
        }

        pendingJumpId = null
        jumpAndGlow(targetIndex, messageId, animate = false)
    }

    // Day of the topmost (partially) visible item, shown in the floating date chip while
    // scrolling. In the reversed list the topmost item is the LAST entry of visibleItemsInfo.
    // A real LazyColumn stickyHeader can't be used here: with reverseLayout the foundation
    // sticky logic pins headers to the wrong edge and expects headers to precede their items.
    // Derived as the formatted day string so derivedStateOf only invalidates on day changes,
    // not on every new topmost message.
    val topVisibleDateString by remember(displayItems) {
        derivedStateOf {
            val millis = listState.layoutInfo.visibleItemsInfo.asReversed().firstNotNullOfOrNull { info ->
                when (val item = displayItems.getOrNull(info.index)) {
                    is MessageDisplayItem.MessageItem -> item.message.getSendDateAsLong()
                    is MessageDisplayItem.DateDivider -> item.dateMillis
                    else -> null // ReaderBar / NewMessagesDivider / SystemMessage carry no date
                }
            }
            millis?.let { millisToString(it, DATE_CHIP_FORMAT) }
        }
    }

    // Chip fades in while scrolling away from the bottom and fades out shortly after scrolling
    // stops. The isAtBottom guard keeps it hidden during the programmatic scrolls at the very
    // bottom (initial open, incoming-message auto-scroll), where the date is obvious anyway.
    var dateChipVisible by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress, isAtBottom) {
        if (listState.isScrollInProgress) {
            if (!isAtBottom) dateChipVisible = true
        } else {
            delay(1000.milliseconds)
            dateChipVisible = false
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            state = listState
        ) {
            items(displayItems, key = { it.id }) { item ->
                when (item) {
                    is MessageDisplayItem.MessageItem -> {
                        val message = item.message
                        //println("Message read by: ${message.readers}")

                        ChatMessageItem(
                            item = item,
                            replyMessage = item.replyMessage,
                            replyMessageSender = item.replySender,
                            isHighlighted = message.id != null && message.id == highlightedMessageId,
                            ownId = ownId,
                            chatId = chatId,
                            useMarkdown = useMarkdown,
                            quickReactions = quickReactions,
                            playbackProgress = playbackProgress,
                            onReplyPreviewClick = {
                                val answerId = message.answerId
                                if (answerId != null) {
                                    val targetIndex = displayItems.indexOfMessage(answerId)
                                    if (targetIndex != -1) {
                                        jumpAndGlow(targetIndex, answerId, animate = true)
                                    } else {
                                        // Above the loaded window: load down to it, then jump.
                                        pendingJumpId = answerId
                                    }
                                }
                            },
                            onAction = onAction
                        )
                    }
                    is MessageDisplayItem.DateDivider -> {
                        // Render date divider using pre-formatted string
                        DayDivider(item.dateMillis)
                    }
                    is MessageDisplayItem.ReaderBar -> {
                        // show readers as small Profile pictures
                        ReaderBar(item.readerList)
                    }
                    is MessageDisplayItem.NewMessagesDivider -> {
                        NewMessagesDivider()
                    }
                    is MessageDisplayItem.SystemMessage -> {
                        // Deliberately not wrapped in MessageViewWithActions/MessageOptionPopup -
                        // no reply/react/edit/delete/copy/long-press for a system event line.
                        SystemMessageItem(systemEventText(item.event))
                    }
                }
            }
        }

        // Floating sticky-style date chip at the top of the list
        val chipText = topVisibleDateString
        if (chipText != null) {
            AnimatedVisibility(
                visible = dateChipVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                DateChip(chipText)
            }
        }

        AnimatedVisibility(
            visible = newMessagesAvailable || !isAtBottom,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            if (newMessagesAvailable) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(Res.string.new_messages)) },
                    icon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(0, scrollOffset = 2)
                        }
                        newMessagesAvailable = false
                    }
                )
            } else {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(0, scrollOffset = 2)
                        }
                    }
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }
        }
    }
}

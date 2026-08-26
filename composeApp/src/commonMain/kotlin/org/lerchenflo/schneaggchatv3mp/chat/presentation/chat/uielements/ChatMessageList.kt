package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.uielements

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageDisplayItem
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.DayDivider
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.NewMessagesDivider
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.ReaderBar
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.SystemMessageItem
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.systemEventText
import org.lerchenflo.schneaggchatv3mp.utilities.PlaybackProgress
import kotlin.time.Duration.Companion.milliseconds

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
    playbackProgress: StateFlow<PlaybackProgress>,
    onAction: (MessageAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var initialScrollDone by remember { mutableStateOf(false) }

    if (displayItems.isNotEmpty()) {
        LaunchedEffect(displayItems.first()) {
            if (!initialScrollDone) {
                //Opened from the message search - the jump effect below scrolls to the
                //searched message instead of the unread divider.
                if (highlightMessageId != null) {
                    initialScrollDone = true
                    return@LaunchedEffect
                }

                val dividerIndex = displayItems.indexOfFirst { it is MessageDisplayItem.NewMessagesDivider }
                if (dividerIndex != -1) {
                    listState.scrollToItem(dividerIndex)
                    // Nudge the divider up from the very bottom edge towards the center of the screen.
                    val viewportHeight = listState.layoutInfo.viewportSize.height
                    listState.scrollToItem(dividerIndex, scrollOffset = -viewportHeight / 2)
                } else {
                    listState.animateScrollToItem(0, scrollOffset = 2)
                }
                initialScrollDone = true
            } else {
                listState.animateScrollToItem(0, scrollOffset = 2)
            }
        }
    }

    val scope = rememberCoroutineScope()

    // Id of the message that should briefly glow after jumping to it via a reply preview
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }

    // Opened from the chat selector's message search: scroll the searched message into view
    // and glow it, the same way a reply preview jump does. Keyed on displayItems because the
    // messages stream in asynchronously - the first emission may not contain it yet.
    //Guarded inside the effect rather than around it: flipping the flag must not remove the
    //effect from composition, which would cancel the glow before it is cleared again.
    var messageJumpDone by remember(highlightMessageId) { mutableStateOf(false) }
    LaunchedEffect(highlightMessageId, displayItems) {
        if (highlightMessageId == null || messageJumpDone) return@LaunchedEffect

        val targetIndex = displayItems.indexOfFirst {
            it is MessageDisplayItem.MessageItem && it.message.id == highlightMessageId
        }
        if (targetIndex == -1) return@LaunchedEffect

        messageJumpDone = true
        listState.scrollToItem(targetIndex)
        highlightedMessageId = highlightMessageId
        delay(1500.milliseconds)
        highlightedMessageId = null
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
            .fillMaxWidth(),
        reverseLayout = true,
        state = listState
    ) {
        items(displayItems, key = { it.id }) { item ->
            when (item) {
                is MessageDisplayItem.MessageItem -> {
                    val message = item.message
                    //println("Message read by: ${message.readers}")

                    var replyMessage: Message? = null
                    if (message.answerId != null) {
                        // Find answer message from display items
                        replyMessage = displayItems
                            .filterIsInstance<MessageDisplayItem.MessageItem>()
                            .firstOrNull { it.message.id == message.answerId }
                            ?.message
                    }

                    ChatMessageItem(
                        item = item,
                        replyMessage = replyMessage,
                        isHighlighted = message.id != null && message.id == highlightedMessageId,
                        ownId = ownId,
                        chatId = chatId,
                        useMarkdown = useMarkdown,
                        playbackProgress = playbackProgress,
                        onReplyPreviewClick = {
                            val targetIndex =
                                displayItems.indexOfFirst {
                                    it is MessageDisplayItem.MessageItem && it.message.id == message.answerId
                                }
                            if (targetIndex != -1) {
                                scope.launch {
                                    listState.animateScrollToItem(targetIndex)
                                    highlightedMessageId = message.answerId
                                    delay(1500.milliseconds)
                                    highlightedMessageId = null
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
}

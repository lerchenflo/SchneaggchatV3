package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stevdza_san.swipeable.Swipeable
import com.stevdza_san.swipeable.domain.ActionCustomization
import com.stevdza_san.swipeable.domain.SwipeAction
import com.stevdza_san.swipeable.domain.SwipeBackground
import com.stevdza_san.swipeable.domain.SwipeBehavior
import com.stevdza_san.swipeable.domain.SwipeDirection
import kotlinx.coroutines.flow.StateFlow
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.Reaction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.utilities.PlaybackProgress

@Composable
fun MessageViewWithActions(
    ownId: String,
    useMD: Boolean = false,
    selectedChatId: String = "",
    message: Message,
    senderName: String? = null,
    senderColor: Int = 0,
    readerMap: Map<String, String> = emptyMap(),
    replyMessage: Message? = null,
    replyMessageOnClick: () -> Unit = {},
    isHighlighted: Boolean = false,
    onReplyCall: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onAction: (MessageAction) -> Unit = {},
    playbackProgress: StateFlow<PlaybackProgress>? = null,
    modifier: Modifier = Modifier
        .fillMaxWidth()
){


    //Swipe right to reply, same library usage as LocationAttributeView on the map
    Swipeable(
        modifier = modifier
            .fillMaxWidth(),
        behavior = SwipeBehavior.DISMISS,
        direction = SwipeDirection.LEFT,
        leftDismissAction = SwipeAction(
            customization = ActionCustomization(
                icon = Icons.AutoMirrored.Filled.Reply,
                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            onAction = onReplyCall
        ),
        leftBackground = SwipeBackground.solid(MaterialTheme.colorScheme.background)
    ){
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val viewConfig = this@pointerInput.viewConfiguration

                        val down = awaitFirstDown(requireUnconsumed = false)

                        // ── Desktop right-click → long press ──────────────────────────
                        if (currentEvent.buttons.isSecondaryPressed) {
                            onLongPress()
                            down.consume()
                            return@awaitEachGesture
                        }

                        // ── Long press detection ───────────────────────────────────────
                        var gestureCancelled = false

                        val firstUp = withTimeoutOrNull(viewConfig.longPressTimeoutMillis) {
                            val up = waitForUpOrCancellation()
                            if (up == null) gestureCancelled = true  // stolen by another composable
                            up
                        }

                        when {
                            // Another composable consumed the event — bail out silently
                            gestureCancelled -> return@awaitEachGesture

                            // withTimeoutOrNull timed out → genuine long press
                            firstUp == null -> {
                                onLongPress()
                                down.consume()
                                return@awaitEachGesture
                            }
                        }

                        // ── Double-tap detection ───────────────────────────────────────
                        val secondDown = withTimeoutOrNull(viewConfig.doubleTapTimeoutMillis) {
                            awaitFirstDown(requireUnconsumed = false)
                        }

                        if (secondDown != null) {
                            secondDown.consume()
                            waitForUpOrCancellation()
                            onAction(MessageAction.ToggleReaction(message.id ?: "", "❤\uFE0F"))
                        } else {
                            // single tap — handle if needed
                        }
                    }
                }
        ) {
            MessageView(
                message = message,
                useMD = useMD,
                selectedChatId = selectedChatId,
                senderName = senderName,
                senderColor = senderColor,
                readerMap = readerMap,
                replyMessage = replyMessage,
                replyMessageOnClick = replyMessageOnClick,
                isHighlighted = isHighlighted,
                onAction = onAction,
                playbackProgress = playbackProgress,
                ownId = ownId
            )
        }
    }
}


@Preview(
    showBackground = true,
    showSystemUi = true,
    heightDp = 2000
)
@Composable
private fun MessageViewWithActions_Text_Reactions_Preview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Short message with multiple same reactions from different users
        MessageViewWithActions(
            ownId = "user1",
            useMD = false,
            selectedChatId = "user2",
            message = Message(
                id = "msg1",
                msgType = MessageType.TEXT,
                content = "Great idea!",
                senderId = "user2",
                receiverId = "user1",
                sendDate = "1704067200000",
                myMessage = false,
                readByMe = true,
                senderAsString = "Alice",
                readers = listOf(
                    MessageReader(
                        messageId = "msg1",
                        readerId = "user1",
                        readDate = "1704067205000"
                    )
                ),
                reactions = listOf(
                    Reaction(userId = "user1", content = "👍"),
                    Reaction(userId = "user3", content = "👍"),
                    Reaction(userId = "user4", content = "👍"),
                    Reaction(userId = "user5", content = "👍"),
                    Reaction(userId = "user6", content = "👍")
                )
            ),
            senderName = "Alice",
            senderColor = 0xFF4CAF50.toInt(),
            onAction = {},
            onLongPress = {},
            onReplyCall = {},
            replyMessageOnClick = {}
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Long message with multiple same reactions from different users
        MessageViewWithActions(
            ownId = "user1",
            useMD = false,
            selectedChatId = "user2",
            message = Message(
                id = "msg2",
                msgType = MessageType.TEXT,
                content = "Hey everyone! I just wanted to share this amazing news with all of you. After months of hard work and dedication, I finally completed my project and it's been approved by the team. This is a huge milestone for me and I couldn't have done it without your support and encouragement throughout this journey.",
                senderId = "user3",
                receiverId = "user1",
                sendDate = "1704067260000",
                myMessage = false,
                readByMe = true,
                senderAsString = "Bob",
                readers = listOf(
                    MessageReader(
                        messageId = "msg2",
                        readerId = "user1",
                        readDate = "1704067265000"
                    )
                ),
                reactions = listOf(
                    Reaction(userId = "user1", content = "❤️"),
                    Reaction(userId = "user2", content = "❤️"),
                    Reaction(userId = "user4", content = "❤️"),
                    Reaction(userId = "user5", content = "❤️"),
                    Reaction(userId = "user6", content = "❤️"),
                    Reaction(userId = "user7", content = "❤️")
                )
            ),
            senderName = "Bob",
            senderColor = 0xFFFF9800.toInt(),
            onAction = {},
            onLongPress = {},
            onReplyCall = {},
            replyMessageOnClick = {}
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Short message with 10 different reactions
        MessageViewWithActions(
            ownId = "user1",
            useMD = false,
            selectedChatId = "user2",
            message = Message(
                id = "msg3",
                msgType = MessageType.TEXT,
                content = "Wow!",
                senderId = "user4",
                receiverId = "user1",
                sendDate = "1704067320000",
                myMessage = false,
                readByMe = true,
                senderAsString = "Charlie",
                readers = listOf(
                    MessageReader(
                        messageId = "msg3",
                        readerId = "user1",
                        readDate = "1704067325000"
                    )
                ),
                reactions = listOf(
                    Reaction(userId = "user1", content = "👍"),
                    Reaction(userId = "user2", content = "❤️"),
                    Reaction(userId = "user3", content = "😂"),
                    Reaction(userId = "user5", content = "😮"),
                    Reaction(userId = "user6", content = "😢"),
                    Reaction(userId = "user7", content = "😡"),
                    Reaction(userId = "user8", content = "👏"),
                    Reaction(userId = "user9", content = "🎉"),
                    Reaction(userId = "user10", content = "🔥"),
                    Reaction(userId = "user11", content = "💯")
                )
            ),
            senderName = "Charlie",
            senderColor = 0xFF9C27B0.toInt(),
            onAction = {},
            onLongPress = {},
            onReplyCall = {},
            replyMessageOnClick = {}
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Long message with 10 different reactions
        MessageViewWithActions(
            ownId = "user1",
            useMD = false,
            selectedChatId = "user2",
            message = Message(
                id = "msg4",
                msgType = MessageType.TEXT,
                content = "I just wanted to take a moment to express my gratitude for this incredible community. Over the past year, we've grown together, faced challenges, celebrated victories, and supported each other through thick and thin. Every conversation, every shared experience, and every moment of connection has made this place special. Thank you all for being part of this journey and for making this community what it is today. Here's to many more years of growth, friendship, and shared success!",
                senderId = "user5",
                receiverId = "user1",
                sendDate = "1704067380000",
                myMessage = false,
                readByMe = true,
                senderAsString = "David",
                readers = listOf(
                    MessageReader(
                        messageId = "msg4",
                        readerId = "user1",
                        readDate = "1704067385000"
                    )
                ),
                reactions = listOf(
                    Reaction(userId = "user1", content = "👍"),
                    Reaction(userId = "user2", content = "❤️"),
                    Reaction(userId = "user3", content = "😂"),
                    Reaction(userId = "user4", content = "😮"),
                    Reaction(userId = "user6", content = "😢"),
                    Reaction(userId = "user7", content = "😡"),
                    Reaction(userId = "user8", content = "👏"),
                    Reaction(userId = "user9", content = "🎉"),
                    Reaction(userId = "user10", content = "🔥"),
                    Reaction(userId = "user11", content = "💯")
                )
            ),
            senderName = "David",
            senderColor = 0xFF607D8B.toInt(),
            onAction = {},
            onLongPress = {},
            onReplyCall = {},
            replyMessageOnClick = {}
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Own message with mixed reactions
        MessageViewWithActions(
            ownId = "user1",
            useMD = false,
            selectedChatId = "user2",
            message = Message(
                id = "msg5",
                msgType = MessageType.TEXT,
                content = "Just finished the presentation!",
                senderId = "user1",
                receiverId = "user2",
                sendDate = "1704067440000",
                myMessage = true,
                readByMe = true,
                senderAsString = "Me",
                readers = emptyList(),
                reactions = listOf(
                    Reaction(userId = "user2", content = "👍"),
                    Reaction(userId = "user3", content = "👍"),
                    Reaction(userId = "user4", content = "🎉"),
                    Reaction(userId = "user5", content = "🎉"),
                    Reaction(userId = "user6", content = "�"),
                    Reaction(userId = "user7", content = "�")
                )
            ),
            senderName = "Me",
            senderColor = 0xFF2196F3.toInt(),
            onAction = {},
            onLongPress = {},
            onReplyCall = {},
            replyMessageOnClick = {}
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Short message with mixed duplicate reactions
        MessageViewWithActions(
            ownId = "user1",
            useMD = false,
            selectedChatId = "user2",
            message = Message(
                id = "msg6",
                msgType = MessageType.TEXT,
                content = "Meeting at 3pm",
                senderId = "user6",
                receiverId = "user1",
                sendDate = "1704067500000",
                myMessage = false,
                readByMe = true,
                senderAsString = "Eve",
                readers = listOf(
                    MessageReader(
                        messageId = "msg6",
                        readerId = "user1",
                        readDate = "1704067505000"
                    )
                ),
                reactions = listOf(
                    Reaction(userId = "user1", content = "👍"),
                    Reaction(userId = "user2", content = "👍"),
                    Reaction(userId = "user3", content = "👍"),
                    Reaction(userId = "user4", content = "👍"),
                    Reaction(userId = "user5", content = "❤️"),
                    Reaction(userId = "user7", content = "❤️"),
                    Reaction(userId = "user8", content = "❤️"),
                    Reaction(userId = "user9", content = "😂"),
                    Reaction(userId = "user10", content = "😂")
                )
            ),
            senderName = "Eve",
            senderColor = 0xFFE91E63.toInt(),
            onAction = {},
            onLongPress = {},
            onReplyCall = {},
            replyMessageOnClick = {}
        )
    }
}
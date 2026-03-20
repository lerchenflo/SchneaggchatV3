package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.utilities.PlaybackProgress
import kotlin.math.roundToInt

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
    onReplyCall: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onAction: (MessageAction) -> Unit = {},
    playbackProgress: StateFlow<PlaybackProgress>? = null,
    modifier: Modifier = Modifier
        .fillMaxWidth()
){


    var contextMenuWidth by remember {
        mutableFloatStateOf(0f)
    }

    val offset = remember {
        Animatable(initialValue = 0f)
    }

    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .fillMaxWidth()
            //.height(IntrinsicSize.Min)
    ){
        Row(
            modifier = Modifier
                .onSizeChanged {
                    contextMenuWidth = it.width.toFloat()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReplyArrow()
        }

        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    // custom tap logic to only consume long press
                    awaitEachGesture {
                        val viewConfig = this@pointerInput.viewConfiguration
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // Check for Desktop Right Click (Secondary Button)
                        if (currentEvent.buttons.isSecondaryPressed) { // doesn't work on my machine but does not affect other functionality
                            //println("right click")
                            onLongPress()
                            down.consume()
                        } else {
                            // Handle Long Press for Touch/Left Click
                            val longPress = withTimeoutOrNull(viewConfig.longPressTimeoutMillis) {
                                waitForUpOrCancellation()
                                false
                            }

                            if (longPress == null) {
                                // Long press detected → consume
                                onLongPress() // call callback
                                down.consume()
                            }
                        }


                    }
                }
                .pointerInput(contextMenuWidth) {
                    // detect swipe for reply
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val newOffset = (offset.value + dragAmount)
                                    .coerceIn(0f, contextMenuWidth)
                                offset.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            if(offset.value > contextMenuWidth * 0.9) onReplyCall() // call Reply when swiped 90% of the way

                            scope.launch {
                                offset.animateTo(0f)
                            }
                        }
                    )
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
                onAction = onAction,
                playbackProgress = playbackProgress,
                ownId = ownId
            )
        }
    }
}
package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.MessageContent
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options.ReactionView
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options.RepliedMessagePreview
import org.lerchenflo.schneaggchatv3mp.utilities.PlaybackProgress


@Composable
fun MessageView(
    ownId: String,
    modifier: Modifier = Modifier,
    message: Message,
    useMD: Boolean = false,
    selectedChatId: String,
    senderName: String? = null,
    senderColor: Int = 0,
    readerMap: Map<String,String> = emptyMap(),
    replyMessage: Message? = null,
    replyMessageOnClick: () -> Unit = {},
    isHighlighted: Boolean = false,
    onAction: (MessageAction) -> Unit = {},
    playbackProgress: StateFlow<PlaybackProgress>? = null,
)
{

    val spaceAfterMessage = 6.dp


    val mymessage = message.myMessage

    // Briefly tints the message when the user jumps here from a reply preview.
    // Fade-in is quick so the highlight is noticeable right away; fade-out is slower
    // so it eases away instead of disappearing abruptly.
    val highlightColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = if (isHighlighted) 200 else 900)
    )

    Column {
        if (replyMessage != null) {

            RepliedMessagePreview(
                message = replyMessage,
                onClick = replyMessageOnClick,
                modifier = Modifier
                    .padding(
                        start = if (mymessage) 40.dp else 0.dp,
                        end = if (mymessage) 0.dp else 40.dp,
                        top = 0.dp,
                        bottom = 0.dp
                    )
            )

        }

        //Ganze breite
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = highlightColor,
                    shape = RoundedCornerShape(15.dp)
                )
                .padding(
                    start = if (mymessage) 40.dp else 0.dp,
                    end = if (mymessage) 0.dp else 40.dp,
                    top = 0.dp,
                    bottom = if (message.reactions.isEmpty()) spaceAfterMessage else 1.dp //Small space for reactions
                )
                ,
            horizontalArrangement = if (mymessage) Arrangement.End else Arrangement.Start
        ) {

            MessageContent(
                modifier = Modifier

                    //.wrapContentSize()
                    .background(
                        color = if (mymessage) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(15.dp)
                    )
                    .padding(6.dp),
                message = message,
                useMD = useMD,
                mymessage = mymessage,
                selectedChatId = selectedChatId,
                senderName = senderName,
                senderColor = senderColor,
                readerMap = readerMap,
                onAction = onAction,
                playbackProgress = playbackProgress,
                ownId = ownId
            )
        }

        ReactionView(
            reactions = message.reactions,
            myMessage = mymessage,
            messageId = message.id ?: "",
            onAction = onAction,
            modifier = Modifier.padding(bottom = spaceAfterMessage)
        )

    }
}










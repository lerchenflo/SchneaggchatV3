package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.uielements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ChatAction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.SendMessageContent
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.ComboInputField
import org.lerchenflo.schneaggchatv3mp.utilities.PlaybackProgress
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.message

/**
 * The staged-content editor in the chat input row: a plain text field, an image caption field
 * with an attachment preview row above it, or the audio recording/preview bar - whichever
 * [content] currently holds.
 */
@Composable
fun RowScope.SendContentInput(
    content: SendMessageContent,
    isEditing: Boolean,
    maxVoiceMsgTime: Long,
    playbackProgress: StateFlow<PlaybackProgress>,
    onAction: (ChatAction) -> Unit,
) {
    when (content) {
        is SendMessageContent.TextContent -> {
            ComboInputField(
                value = content.textMessage,
                onValueChange = { newValue ->
                    onAction(ChatAction.OnSendContentChange(SendMessageContent.TextContent(newValue)))
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .sendOnEnter(
                        currentText = content.textMessage,
                        onInsertNewline = { newValue ->
                            onAction(ChatAction.OnSendContentChange(SendMessageContent.TextContent(newValue)))
                        },
                        isEditing = isEditing,
                        onSend = { onAction(ChatAction.OnSendClick) },
                        onConfirmEdit = { onAction(ChatAction.OnConfirmEditClick) },
                    ),
                placeholder = { Text(stringResource(Res.string.message) + " ...") }
            )
        }

        is SendMessageContent.ImageContent -> {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Multi-image preview row
                ImageAttachmentRow(
                    images = content.images,
                    onRemoveImage = { imageBytes ->
                        val remaining = content.images - imageBytes
                        onAction(ChatAction.OnSendContentChange(
                            if (remaining.isEmpty())
                                SendMessageContent.TextContent(content.text)
                            else
                                content.copy(images = remaining)
                        ))
                    }
                )

                ComboInputField(
                    value = content.text,
                    onValueChange = { newValue ->
                        onAction(ChatAction.OnSendContentChange(content.copy(text = newValue)))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .sendOnEnter(
                            currentText = content.text,
                            onInsertNewline = { newValue ->
                                onAction(ChatAction.OnSendContentChange(content.copy(text = newValue)))
                            },
                            isEditing = isEditing,
                            onSend = { onAction(ChatAction.OnSendClick) },
                            onConfirmEdit = { onAction(ChatAction.OnConfirmEditClick) },
                        ),
                    placeholder = { Text(stringResource(Res.string.message) + " ...") }
                )
            }
        }

        is SendMessageContent.AudioContent -> {
            AudioRecordingBar(
                content = content,
                maxVoiceMsgTime = maxVoiceMsgTime,
                playbackProgress = playbackProgress,
                onPlay = { messageId, path ->
                    onAction(ChatAction.OnMessageAction(MessageAction.PlayAudio(messageId, path)))
                },
                onPause = { onAction(ChatAction.OnMessageAction(MessageAction.PauseAudio())) },
                onSeek = { position -> onAction(ChatAction.OnMessageAction(MessageAction.SeekAudio(position))) },
                onDiscard = { onAction(ChatAction.OnDiscardRecording) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

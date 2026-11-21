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
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.audio.voiceMessagePlayerTexts
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.ComboInputField
import io.github.lerchenflo.voicemessages.VoicePlaybackState
import io.github.lerchenflo.voicemessages.VoiceRecordingController
import io.github.lerchenflo.voicemessages.VoiceRecordingState
import io.github.lerchenflo.voicemessages.ui.VoiceMessageRecordingBar
import io.github.lerchenflo.voicemessages.ui.VoiceMessageRecordingBarTexts
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.chat_discard_recording
import schneaggchatv3mp.composeapp.generated.resources.message
import schneaggchatv3mp.composeapp.generated.resources.voice_message_recording_level

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
    mentionContextIds: Set<String>,
    playbackProgress: StateFlow<VoicePlaybackState>,
    recordingLevels: StateFlow<List<Float>>,
    onAction: (ChatAction) -> Unit,
) {
    when (content) {
        is SendMessageContent.TextContent -> {
            ComboInputField(
                value = content.textMessage,
                onValueChange = { newValue ->
                    onAction(ChatAction.OnSendContentChange(SendMessageContent.TextContent(newValue)))
                },
                contextIds = mentionContextIds,
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
                    contextIds = mentionContextIds,
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
            val draftId = VoiceRecordingController.DRAFT_PLAYBACK_ID
            val recorded = content.recording as? VoiceRecordingState.Recorded
            VoiceMessageRecordingBar(
                state = content.recording,
                levels = recordingLevels,
                playback = playbackProgress,
                maxDurationMs = maxVoiceMsgTime,
                texts = VoiceMessageRecordingBarTexts(
                    recording = stringResource(Res.string.voice_message_recording_level),
                    discard = stringResource(Res.string.chat_discard_recording),
                    player = voiceMessagePlayerTexts(),
                ),
                onPlay = {
                    recorded?.let { onAction(ChatAction.OnMessageAction(MessageAction.PlayAudio(draftId, it.fileName))) }
                },
                onPause = { onAction(ChatAction.OnMessageAction(MessageAction.PauseAudio())) },
                onSeek = { position -> onAction(ChatAction.OnMessageAction(MessageAction.SeekAudio(draftId, position))) },
                onDiscard = { onAction(ChatAction.OnDiscardRecording) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

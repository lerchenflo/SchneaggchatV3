package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.uielements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ChatAction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.SendMessageContent
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.poll.PollDialog
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options.ReplyPreview
import org.lerchenflo.schneaggchatv3mp.utilities.PlaybackProgress
import kotlinx.coroutines.flow.StateFlow
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add

/**
 * The chat screen's bottom input bar: reply preview, attach/poll menu, staged content editor and
 * trailing send/edit/record controls. Pure orchestration - actual content rendering lives in
 * SendContentInput / AudioRecordingBar / SendActionButtons.
 */
@Composable
fun ChatInputBar(
    sendContent: SendMessageContent,
    replyMessage: Message?,
    editMessage: Message?,
    useMarkdown: Boolean,
    chatId: String,
    ownId: String,
    isDesktop: Boolean,
    maxVoiceMsgTime: Long,
    playbackProgress: StateFlow<PlaybackProgress>,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var addMediaDropdownExpanded by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showImagePickerDialog by remember { mutableStateOf(false) }

    ImageSourcePickerDialog(
        visible = showImagePickerDialog,
        onImagesSelected = { results ->
            onAction(ChatAction.OnImagesSelected(results))
            showImagePickerDialog = false
        },
        onDismiss = { showImagePickerDialog = false }
    )

    Column(modifier = modifier) { //Column that the reply preview does not take the remaining space

        // Reply view
        replyMessage?.let { reply ->
            ReplyPreview(
                ownId = ownId,
                message = reply,
                useMD = useMarkdown,
                selectedChatId = chatId,
                onDismiss = { onAction(ChatAction.OnCancelReply) },
            )
        }

        //Inputrow for sending messages
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // button zum züg addden

            val hasContent = sendContent !is SendMessageContent.TextContent
                    || (sendContent as? SendMessageContent.TextContent)?.textMessage?.text?.isNotEmpty() == true
            val isRecording = (sendContent as? SendMessageContent.AudioContent)?.isRecording ?: false

            if (!hasContent) {
                IconButton(
                    onClick = { addMediaDropdownExpanded = true },
                    modifier = Modifier
                        .padding(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = stringResource(Res.string.add)
                    )
                }
            }

            AddMediaMenu(
                expanded = addMediaDropdownExpanded,
                onDismiss = { addMediaDropdownExpanded = false },
                onImageClick = { showImagePickerDialog = true },
                onPollClick = { showPollDialog = true },
            )

            if (showPollDialog) {
                PollDialog(
                    onDismiss = { showPollDialog = false },
                    onCreatePoll = { poll ->
                        println("Poll created: $poll")
                        onAction(ChatAction.OnCreatePoll(poll))
                    }
                )
            }

            //sendinput (This is a rowscope)
            SendContentInput(
                content = sendContent,
                isEditing = editMessage != null,
                maxVoiceMsgTime = maxVoiceMsgTime,
                playbackProgress = playbackProgress,
                onAction = onAction,
            )

            SendActionButtons(
                isEditing = editMessage != null,
                isRecording = isRecording,
                hasContent = hasContent,
                isDesktop = isDesktop,
                onSend = { onAction(ChatAction.OnSendClick) },
                onConfirmEdit = { onAction(ChatAction.OnConfirmEditClick) },
                onCancelEdit = { onAction(ChatAction.OnMessageAction(MessageAction.CancelEditMessage)) },
                onStartRecording = { onAction(ChatAction.OnStartRecording) },
                onStopRecording = { onAction(ChatAction.OnStopRecording) },
            )
        }

    }
}

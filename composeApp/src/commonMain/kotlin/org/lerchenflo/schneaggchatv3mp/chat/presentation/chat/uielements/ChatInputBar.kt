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
import io.github.lerchenflo.taptarget.tapTarget
import io.github.lerchenflo.voicemessages.VoicePlaybackState
import io.github.lerchenflo.voicemessages.VoiceRecordingState
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.SenderInfo
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ChatAction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.SendMessageContent
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.poll.PollDialog
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options.ReplyPreview
import org.lerchenflo.schneaggchatv3mp.sharedUi.pickers.ImagePickPurpose
import org.lerchenflo.schneaggchatv3mp.sharedUi.pickers.ImageSourcePickerDialog
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add

/**
 * The chat screen's bottom input bar: reply preview, attach/poll menu, staged content editor and
 * trailing send/edit/record controls. Pure orchestration - actual content rendering lives in
 * SendContentInput (with the voice recording bar) / SendActionButtons.
 */
@Composable
fun ChatInputBar(
    sendContent: SendMessageContent,
    replyMessage: Message?,
    replyMessageSender: SenderInfo?,
    editMessage: Message?,
    editPollMessage: Message?,
    isSavingPollEdit: Boolean,
    useMarkdown: Boolean,
    chatId: String,
    ownId: String,
    maxVoiceMsgTime: Long,
    mentionContextIds: Set<String>,
    playbackProgress: StateFlow<VoicePlaybackState>,
    recordingLevels: StateFlow<List<Float>>,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var addMediaDropdownExpanded by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showImagePickerDialog by remember { mutableStateOf(false) }

    ImageSourcePickerDialog(
        visible = showImagePickerDialog,
        purpose = ImagePickPurpose.MESSAGE_IMAGES,
        onImagesPicked = { photos, fromCamera -> onAction(ChatAction.OnImagesSelected(photos, fromCamera)) },
        onDismiss = { showImagePickerDialog = false }
    )

    Column(modifier = modifier) { //Column that the reply preview does not take the remaining space

        // Reply view
        replyMessage?.let { reply ->
            ReplyPreview(
                ownId = ownId,
                message = reply,
                sender = replyMessageSender,
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

            val hasContent = sendContent !is SendMessageContent.TextContent || (sendContent).textMessage.text.isNotEmpty()
            val isRecording = (sendContent as? SendMessageContent.AudioContent)?.recording is VoiceRecordingState.Recording

            if (!hasContent) {
                IconButton(
                    onClick = { addMediaDropdownExpanded = true },
                    modifier = Modifier
                        .padding(5.dp)
                        .tapTarget("chat_add_media")
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
                    onSubmit = { poll ->
                        println("Poll created: $poll")
                        onAction(ChatAction.OnCreatePoll(poll))
                    },
                    mentionContextIds = mentionContextIds,
                )
            }

            editPollMessage?.poll?.let { poll ->
                PollDialog(
                    onDismiss = { onAction(ChatAction.OnMessageAction(MessageAction.CancelEditPoll)) },
                    onSubmit = { onAction(ChatAction.OnSaveEditedPoll(it)) },
                    editingPoll = poll,
                    isSaving = isSavingPollEdit,
                    mentionContextIds = mentionContextIds,
                )
            }

            //sendinput (This is a rowscope)
            SendContentInput(
                content = sendContent,
                isEditing = editMessage != null,
                maxVoiceMsgTime = maxVoiceMsgTime,
                mentionContextIds = mentionContextIds,
                playbackProgress = playbackProgress,
                recordingLevels = recordingLevels,
                onAction = onAction,
            )

            SendActionButtons(
                isEditing = editMessage != null,
                isRecording = isRecording,
                hasContent = hasContent,
                onSend = { onAction(ChatAction.OnSendClick) },
                onConfirmEdit = { onAction(ChatAction.OnConfirmEditClick) },
                onCancelEdit = { onAction(ChatAction.OnMessageAction(MessageAction.CancelEditMessage)) },
                onStartRecording = { onAction(ChatAction.OnStartRecording) },
                onStopRecording = { onAction(ChatAction.OnStopRecording) },
            )
        }

    }
}

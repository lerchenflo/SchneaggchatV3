package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.uielements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.flow.StateFlow
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageDisplayItem
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.MessageViewWithActions
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options.DeleteMessageAlert
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options.MessageDetailsDialog
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options.MessageOptionPopup
import org.lerchenflo.schneaggchatv3mp.utilities.PlaybackProgress
import org.lerchenflo.schneaggchatv3mp.utilities.copyToClipboard

/**
 * A single message bubble plus its long-press options popup and the delete/details dialogs it
 * can open. One instance per MessageDisplayItem.MessageItem in the chat's LazyColumn.
 */
@Composable
fun ChatMessageItem(
    item: MessageDisplayItem.MessageItem,
    replyMessage: Message?,
    isHighlighted: Boolean,
    ownId: String,
    chatId: String,
    useMarkdown: Boolean,
    playbackProgress: StateFlow<PlaybackProgress>,
    onReplyPreviewClick: () -> Unit,
    onAction: (MessageAction) -> Unit,
) {
    val message = item.message
    val clipboard = LocalClipboard.current.nativeClipboard

    var showMessageOptionPopup by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()){
        MessageViewWithActions(
            useMD = useMarkdown,
            selectedChatId = chatId,
            message = message,
            senderName = item.senderName,  // Pre-resolved sender name!
            senderColor = item.senderColor, // Pre-resolved sender color!
            minimal = item.messageMinimal,
            modifier = Modifier,
            replyMessage = replyMessage,
            replyMessageOnClick = onReplyPreviewClick,
            isHighlighted = isHighlighted,
            onReplyCall = {
                onAction(MessageAction.ReplyToMessage(message))
            },
            onLongPress = {
                showMessageOptionPopup = true
            },
            onAction = onAction,
            playbackProgress = playbackProgress,
            readerMap = item.resolvedReaders,
            ownId = ownId
        )

        var showDeleteAlert by remember { mutableStateOf(false) }
        var showDetailsDialog by remember { mutableStateOf(false) }

        MessageOptionPopup(
            expanded = showMessageOptionPopup,
            message = message,
            onDismissRequest = { showMessageOptionPopup = false },
            onReply = { onAction(MessageAction.ReplyToMessage(message)) },
            onCopy = {
                copyToClipboard(message.content, clipboard)
                showMessageOptionPopup = false
            },
            onDelete = {
                showDeleteAlert = true
                showMessageOptionPopup = false
            },
            onEdit = {
                onAction(MessageAction.StartEditMessage(message))
                showMessageOptionPopup = false
            },
            onDetails = {
                showDetailsDialog = true
                showMessageOptionPopup = false
            },
            onReact = { reaction ->
                onAction(MessageAction.ToggleReaction(message.id ?: "", reaction))
                showMessageOptionPopup = false
            },
            modifier = Modifier.align(
                if (message.myMessage) Alignment.TopEnd else Alignment.TopStart
            )
        )

        if(showDeleteAlert) {
            DeleteMessageAlert(
                onDismiss = { showDeleteAlert = false },
                onConfirm = {
                    onAction(MessageAction.DeleteMessage(message))
                    showDeleteAlert = false
                },
                message = message,
                selectedChatId = chatId,
                ownId = ownId
            )
        }

        if(showDetailsDialog) {
            MessageDetailsDialog(
                onDismiss = { showDetailsDialog = false },
                message = message,
                selectedChatId = chatId,
                ownId = ownId,
                resolvedReactions = item.resolvedReactions
            )
        }
    }
}

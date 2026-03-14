package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ReaderRow
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.MessageContent
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.copy
import schneaggchatv3mp.composeapp.generated.resources.delete
import schneaggchatv3mp.composeapp.generated.resources.edit
import schneaggchatv3mp.composeapp.generated.resources.message_delete_info
import schneaggchatv3mp.composeapp.generated.resources.message_details
import schneaggchatv3mp.composeapp.generated.resources.no_readers
import schneaggchatv3mp.composeapp.generated.resources.readers
import schneaggchatv3mp.composeapp.generated.resources.reply
import schneaggchatv3mp.composeapp.generated.resources.yes

@Composable
fun MessageOptionPopup(
    expanded: Boolean,
    message: Message,
    onDismissRequest: () -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        // contentAlignment = if (myMessage) Alignment.TopEnd else Alignment.TopStart
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.reply)) },
                onClick = {
                    onReply()
                    onDismissRequest()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null
                    )
                }
            )

            if (message.msgType == MessageType.TEXT || message.msgType == MessageType.IMAGE) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.copy)) },
                    onClick = {
                        onCopy()
                        onDismissRequest()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null
                        )
                    }
                )
            }

            if (message.myMessage) {

                if (message.msgType == MessageType.TEXT || message.msgType == MessageType.IMAGE) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.edit)) },
                        onClick = {
                            onEdit()
                            onDismissRequest()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null
                            )
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.delete)) },
                    onClick = {
                        onDelete()
                        onDismissRequest()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null
                        )
                    }
                )
            }

            DropdownMenuItem(
                text = { Text(stringResource(Res.string.message_details)) },
                onClick = {
                    onDetails()
                    onDismissRequest()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
fun DeleteMessageAlert(
    ownId: String,
    onDismiss:() -> Unit,
    onConfirm:() -> Unit,
    message: Message,
    selectedChatId: String,
){
    AlertDialog(
        onDismissRequest = {onDismiss()},
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(stringResource(Res.string.yes))
            }
        },
        dismissButton =
            {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        //icon = { Icon(Icons.Default.Palette, contentDescription = null) },
        title = { Text(text = stringResource(Res.string.message_delete_info)) },
        text = {
            val alphaValue = 0.8f
            MessageContent(
                modifier = Modifier
                    //.wrapContentSize()
                    .background(
                        color = if (message.myMessage) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = alphaValue)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = alphaValue)
                        },
                        shape = RoundedCornerShape(15.dp)
                    )
                    .padding(6.dp),
                message = message,
                useMD = false, // fertig mit markdown
                selectedChatId = selectedChatId,
                ownId = ownId,
            )
        },
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
fun MessageDetailsDialog(
    ownId: String,
    onDismiss: () -> Unit,
    message: Message,
    selectedChatId: String,
) {
    Dialog(onDismissRequest = onDismiss) {
        // Use a Surface to provide background and elevation to the Dialog content
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Header
                Text(
                    text = stringResource(Res.string.message_details),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 2. Message Preview
                val alphaValue = 0.9f
                MessageContent(
                    modifier = Modifier
                        .background(
                            color = if (message.myMessage) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = alphaValue)
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = alphaValue)
                            },
                            shape = RoundedCornerShape(15.dp)
                        )
                        .padding(12.dp),
                    message = message,
                    useMD = false,
                    selectedChatId = selectedChatId,
                    ownId = ownId,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 3. Readers Section
                Text(
                    text = stringResource(Res.string.readers),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (message.readers.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.no_readers),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false), // Grow up to a point, then scroll
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(message.readers) { reader ->
                            ReaderRow(reader)
                        }
                    }
                }
            }
        }
    }
}
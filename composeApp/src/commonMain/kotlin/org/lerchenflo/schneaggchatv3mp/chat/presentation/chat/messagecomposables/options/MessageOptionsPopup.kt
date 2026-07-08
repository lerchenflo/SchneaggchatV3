package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ReaderRow
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.MessageContent
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.copy
import schneaggchatv3mp.composeapp.generated.resources.custom_reaction
import schneaggchatv3mp.composeapp.generated.resources.delete
import schneaggchatv3mp.composeapp.generated.resources.edit
import schneaggchatv3mp.composeapp.generated.resources.enter_reaction
import schneaggchatv3mp.composeapp.generated.resources.message_delete_info
import schneaggchatv3mp.composeapp.generated.resources.message_details
import schneaggchatv3mp.composeapp.generated.resources.no_reactions
import schneaggchatv3mp.composeapp.generated.resources.no_readers
import schneaggchatv3mp.composeapp.generated.resources.reactions
import schneaggchatv3mp.composeapp.generated.resources.readers
import schneaggchatv3mp.composeapp.generated.resources.reply
import schneaggchatv3mp.composeapp.generated.resources.sent_at
import schneaggchatv3mp.composeapp.generated.resources.yes

// Desktop mouse wheels only report scroll input on the y axis, but horizontalScroll only
// reacts to its own (x) axis — so without this, the quick-reactions row can't be scrolled
// with a mouse wheel on desktop, only by dragging.
private val MOUSE_WHEEL_SCROLL_STEP = 48.dp

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
    onReact: (String) -> Unit,
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

            //reply
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

            //Add reaction
            DropdownMenuItem(
                text = {

                    //Quick reactions
                    val quickReactionsScrollState = rememberScrollState()
                    val density = LocalDensity.current
                    val mouseWheelScrollStepPx = with(density) { MOUSE_WHEEL_SCROLL_STEP.toPx() }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .horizontalScroll(quickReactionsScrollState)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Scroll) {
                                            val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                            if (scrollDelta != 0f) {
                                                quickReactionsScrollState.dispatchRawDelta(scrollDelta * mouseWheelScrollStepPx)
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                                }
                            }
                    ) {

                        /* Default heart is with double tap
                        NormalButton(
                            text = "❤\uFE0F",
                            onClick = { onReact("❤\uFE0F"); onDismissRequest() },
                            primary = false
                        )

                         */

                        NormalButton(
                            text = "👍",
                            onClick = { onReact("👍"); onDismissRequest() },
                            primary = false
                        )

                        NormalButton(
                            text = "👎",
                            onClick = { onReact("👎"); onDismissRequest() },
                            primary = false
                        )

                        NormalButton(
                            text = "\uD83D\uDE02",
                            onClick = { onReact("\uD83D\uDE02"); onDismissRequest() },
                            primary = false
                        )

                        NormalButton(
                            text = "🍻",
                            onClick = { onReact("🍻"); onDismissRequest() },
                            primary = false
                        )

                        var showCustomReactionDialog by remember { mutableStateOf(false) }
                        var customReactionInput by remember { mutableStateOf("") }
                        val maxChars = 10

                        // Show custom input dialog with max 10 chars
                        NormalButton(
                            text = "✏️",
                            onClick = { showCustomReactionDialog = true },
                            primary = false
                        )

                        if (showCustomReactionDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    showCustomReactionDialog = false
                                    customReactionInput = ""
                                },
                                title = { Text(stringResource(Res.string.custom_reaction)) },
                                text = {
                                    Column {
                                        OutlinedTextField(
                                            value = customReactionInput,
                                            onValueChange = { if (it.length <= maxChars) customReactionInput = it },
                                            label = { Text(stringResource(Res.string.enter_reaction)) },
                                            singleLine = true,
                                            supportingText = {
                                                Text(
                                                    text = "${customReactionInput.length} / $maxChars",
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.End
                                                )
                                            },
                                            isError = customReactionInput.length == maxChars
                                        )
                                    }
                                },
                                confirmButton = {
                                    NormalButton(
                                        text = stringResource(Res.string.add),
                                        onClick = {
                                            if (customReactionInput.isNotBlank()) {
                                                onReact(customReactionInput)
                                                onDismissRequest()
                                                showCustomReactionDialog = false
                                                customReactionInput = ""
                                            }
                                        },
                                        primary = true
                                    )
                                },
                                dismissButton = {
                                    NormalButton(
                                        text = stringResource(Res.string.cancel),
                                        onClick = {
                                            showCustomReactionDialog = false
                                            customReactionInput = ""
                                        },
                                        primary = false
                                    )
                                }
                            )
                        }

                    }
                },
                onClick = {
                    onDismissRequest()
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
    resolvedReactions: Map<String, String> = emptyMap(),
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.message_details),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.cancel))
                    }
                }

                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Message preview
                    item {
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
                    }

                    // Send time
                    item {
                        val sendMillis = message.getSendDateAsLong()
                        if (sendMillis > 0L) {
                            Text(
                                text = stringResource(Res.string.sent_at, millisToTimeDateOrYesterday(sendMillis)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Reactions section
                    item {
                        HorizontalDivider()
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = stringResource(Res.string.reactions),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (message.reactions.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.no_reactions),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    } else {
                        val ownUserId = SessionCache.requireLoggedIn()?.userId
                        val grouped = message.reactions
                            .groupBy { it.content }
                            .entries.sortedBy { it.key }

                        items(grouped) { (emoji, reactors) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                ReactionBadgeItem(
                                    reactionBadge = ReactionBadge(
                                        reaction = emoji,
                                        count = reactors.size,
                                        hasReacted = reactors.any { it.userId == ownUserId }
                                    )
                                )
                                Spacer(Modifier.size(10.dp))
                                Column {
                                    reactors.forEach { reaction ->
                                        Text(
                                            text = resolvedReactions[reaction.userId] ?: reaction.userId,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Readers section
                    item {
                        HorizontalDivider()
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = stringResource(Res.string.readers),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (message.readers.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.no_readers),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    } else {
                        items(message.readers) { reader ->
                            ReaderRow(reader)
                        }
                    }

                    item { Spacer(Modifier.size(8.dp)) }
                }
            }
        }
    }
}
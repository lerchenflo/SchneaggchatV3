package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageDisplayItem
import org.lerchenflo.schneaggchatv3mp.sharedUi.DayDivider
import org.lerchenflo.schneaggchatv3mp.sharedUi.MessageContent
import org.lerchenflo.schneaggchatv3mp.sharedUi.MessageViewWithActions
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.audio
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.copied_to_clipboard
import schneaggchatv3mp.composeapp.generated.resources.copy
import schneaggchatv3mp.composeapp.generated.resources.delete
import schneaggchatv3mp.composeapp.generated.resources.edit
import schneaggchatv3mp.composeapp.generated.resources.go_back
import schneaggchatv3mp.composeapp.generated.resources.image
import schneaggchatv3mp.composeapp.generated.resources.message
import schneaggchatv3mp.composeapp.generated.resources.message_delete_info
import schneaggchatv3mp.composeapp.generated.resources.poll
import schneaggchatv3mp.composeapp.generated.resources.reply
import schneaggchatv3mp.composeapp.generated.resources.unknown_error
import schneaggchatv3mp.composeapp.generated.resources.yes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Preview()
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier
        .fillMaxSize()
){
    val globalViewModel = koinInject<GlobalViewModel>()
    val viewModel = koinViewModel<ChatViewModel>()
    val displayItems by viewModel.messageDisplayState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { focusManager.clearFocus() } // only pointer taps
            },
        topBar = {
            // Obere Zeile (Backbutton, profilbild, name, ...)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp, 0.dp, 10.dp, 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ){

                // Backbutton
                IconButton(
                    onClick = {
                        viewModel.onBackClick()
                    },
                    modifier = Modifier
                        .padding(start = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.go_back),
                    )
                }

                UserButton(
                    selectedChat = globalViewModel.selectedChat.value,
                    onClickGes = {
                        viewModel.onChatDetailsClick()
                    },
                )
            }
        },
        bottomBar = {

        },
        contentWindowInsets = WindowInsets.ime
    ) {innerPadding ->
        // The innerPadding contains the height of the topBar
        Column(
            modifier = modifier
                .padding(innerPadding)
        ) {

            // Messages
            val listState = rememberLazyListState()

            //Wenn sich die letzte nachricht ändert denn abescrollen
            if (displayItems.isNotEmpty()) {
                LaunchedEffect(displayItems.first()) {
                    listState.animateScrollToItem(0, scrollOffset = 2)
                }
            }

            val scope = rememberCoroutineScope()
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                reverseLayout = true,
                state = listState
            ) {
                items(displayItems, key = { it.id }) { item ->
                    when (item) {
                        is MessageDisplayItem.MessageItem -> {
                            val message = item.message

                            var answerMessage: Message? = null
                            if (message.answerId != null) {
                                // Find answer message from display items
                                answerMessage = displayItems
                                    .filterIsInstance<MessageDisplayItem.MessageItem>()
                                    .firstOrNull { it.message.id == message.answerId }
                                    ?.message
                            }

                            var showMessageOptionPopup by remember { mutableStateOf(false) }

                            Box(modifier = Modifier.fillMaxWidth()){
                                MessageViewWithActions(
                                    useMD = viewModel.markdownEnabled,
                                    selectedChatId = globalViewModel.selectedChat.value.id,
                                    message = message,
                                    senderName = item.senderName,  // Pre-resolved sender name!
                                    senderColor = item.senderColor, // Pre-resolved sender color!
                                    modifier = Modifier,
                                    replyMessage = answerMessage,
                                    replyMessageOnClick = {
                                        val targetIndex =
                                            displayItems.indexOfFirst {
                                                it is MessageDisplayItem.MessageItem && it.message.id == message.answerId
                                            }
                                        if (targetIndex != -1) {
                                            scope.launch {
                                                listState.animateScrollToItem(targetIndex)
                                            }
                                        }
                                    },
                                    onReplyCall = {
                                        viewModel.updateReplyMessage(message)
                                    },
                                    onLongPress = {
                                        showMessageOptionPopup = true
                                    }
                                )

                                val copiedToClipboardString = stringResource(Res.string.copied_to_clipboard)

                                var showDeleteAlert by remember { mutableStateOf(false) }

                                MessageOptionPopup(
                                    expanded = showMessageOptionPopup,
                                    message = message,
                                    onDismissRequest = { showMessageOptionPopup = false },
                                    onReply = { viewModel.updateReplyMessage(message) },
                                    onCopy = {
                                        copyToClipboard(message.content, clipboardManager)
                                        SnackbarManager.showMessage(copiedToClipboardString)
                                        showMessageOptionPopup = false
                                    },
                                    onDelete = {

                                        showDeleteAlert = true
                                        showMessageOptionPopup = false
                                    },
                                    onEdit = {
                                        viewModel.editMessageId = message.id
                                        viewModel.updatesendText(TextFieldValue(message.content))
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

                                            viewModel.deleteMessage(item.message)
                                            showDeleteAlert = false
                                        },
                                        message = message,
                                        selectedChatId = globalViewModel.selectedChat.value.id
                                    )
                                }
                            }
                        }
                        is MessageDisplayItem.DateDivider -> {
                            // Render date divider using pre-formatted string
                            DayDivider(item.dateMillis)
                        }

                    }
                }
            }

                // Reply view
                if (viewModel.replyMessage != null) {
                    ReplyPreview(viewModel, globalViewModel)
                }

                InputFieldRow(viewModel)

        }
    }
}

@Composable
fun InputFieldRow(viewModel: ChatViewModel){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // button zum züg addden
        var addMediaDropdownExpanded by remember { mutableStateOf(false) }

/*

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

 */
        if (addMediaDropdownExpanded) {
            DropdownMenu(
                expanded = addMediaDropdownExpanded,
                onDismissRequest = { addMediaDropdownExpanded = false }
            ) {
                AddMediaOptions.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row {
                                Icon(
                                    imageVector = option.getIcon(),
                                    contentDescription = option.toUiText().asString(),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = option.toUiText().asString()
                                )
                            }
                        },
                        onClick = {
                            addMediaDropdownExpanded = false
                            option.getAction()
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = viewModel.sendText,
            onValueChange = { newValue ->
                viewModel.updatesendText(newValue)
            },
            modifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { event ->
                    // Check if the key is 'Enter' and it's a 'KeyDown' event
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                        if (event.isShiftPressed) {
                            // ACTION: Shift + Enter
                            // add a newline
                            val newValue = viewModel.sendText.text.replaceRange(
                                viewModel.sendText.selection.min,
                                viewModel.sendText.selection.max,
                                "\n"
                            )
                            val newCursorPos = viewModel.sendText.selection.min + 1
                            viewModel.updatesendText(
                                viewModel.sendText.copy(
                                    text = newValue,
                                    selection = TextRange(newCursorPos)
                                )
                            )
                            return@onPreviewKeyEvent false // Let the system handle the newline
                        } else {
                            // ACTION: Enter (only)
                            // Send the message
                            viewModel.sendMessage()
                            return@onPreviewKeyEvent true // Consume the event (no newline added)
                        }
                    }
                    false // Pass all other events (letters, backspace, etc.) to the TextField
                },
            placeholder = { Text(stringResource(Res.string.message) + " ...") }
        )

        if(viewModel.editMessageId == null){ // schoua ob mir gad a nachricht bearbeitend
            // send button
            IconButton(
                onClick = { viewModel.sendMessage() },
                modifier = Modifier
                    .padding(5.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(Res.string.add),
                )
            }
        }else{
            IconButton(
                onClick = {
                    viewModel.editMessageId = null
                    viewModel.updatesendText(TextFieldValue(""))
                },
                modifier = Modifier
                    .padding(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = stringResource(Res.string.cancel),
                )
            }
            IconButton(
                onClick = {
                    viewModel.editMessage()

                },
                modifier = Modifier
                    .padding(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.edit),
                )
            }
            // mdtodo
        }



    }
}

@Composable
fun ReplyPreview(viewModel: ChatViewModel, globalViewModel: GlobalViewModel){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            val alphaValue = 0.8f
            MessageContent(
                modifier = Modifier
                    //.wrapContentSize()
                    .background(
                        color = if (viewModel.replyMessage!!.myMessage) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = alphaValue)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = alphaValue)
                        },
                        shape = RoundedCornerShape(15.dp)
                    )
                    .padding(6.dp),
                message = viewModel.replyMessage!!,
                useMD = viewModel.markdownEnabled,
                selectedChatId = globalViewModel.selectedChat.value.id,
                senderColor = viewModel.replyMessage!!.senderColor
            )
        }
        Column(

        ) {
            IconButton(
                onClick = {
                    viewModel.updateReplyMessage(null)
                },
                modifier = Modifier
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.close)
                )
            }
        }


    }
}

@Composable
fun MessageOptionPopup(
    expanded: Boolean,
    message: Message,
    onDismissRequest: () -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
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

            if (message.myMessage) {

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
        }
    }
}

@Composable
fun DeleteMessageAlert(
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
                selectedChatId = selectedChatId
            )
        },
        shape = MaterialTheme.shapes.large,
    )
}

fun copyToClipboard(text: String, clipboardManager: ClipboardManager) {
    clipboardManager.setText(AnnotatedString(text))
}
@OptIn(ExperimentalTime::class)
fun Long.toLocalDate(): LocalDate {
    val instant = Instant.fromEpochMilliseconds(this)
    return instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
}

enum class AddMediaOptions{
    IMAGE,
    POLL,
    AUDIO;

    fun toUiText(): UiText = when (this) {
        IMAGE -> UiText.StringResourceText(Res.string.image)
        POLL -> UiText.StringResourceText(Res.string.poll)
        AUDIO   -> UiText.StringResourceText(Res.string.audio)
        else -> UiText.StringResourceText(Res.string.unknown_error)
    }
    fun getIcon(): ImageVector = when (this) {
        IMAGE -> Icons.Default.Image
        POLL -> Icons.Default.Poll
        AUDIO   -> Icons.Default.Headphones
        else -> Icons.Default.Menu
    }

    fun getAction(): Unit = when (this) {
        IMAGE -> {
            SnackbarManager.showMessage("to be done (image)")
        }
        POLL -> {
            SnackbarManager.showMessage("to be done (poll)")
        }
        AUDIO   -> {
            SnackbarManager.showMessage("to be done (audio)")
        }
    }
}
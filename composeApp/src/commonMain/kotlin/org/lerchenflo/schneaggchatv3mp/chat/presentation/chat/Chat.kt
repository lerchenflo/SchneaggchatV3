package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import org.lerchenflo.schneaggchatv3mp.sharedUi.DayDivider
import org.lerchenflo.schneaggchatv3mp.sharedUi.MessageContent
import org.lerchenflo.schneaggchatv3mp.sharedUi.MessageViewWithActions
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.audio
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.copied_to_clipboard
import schneaggchatv3mp.composeapp.generated.resources.copy
import schneaggchatv3mp.composeapp.generated.resources.delete
import schneaggchatv3mp.composeapp.generated.resources.edit
import schneaggchatv3mp.composeapp.generated.resources.go_back
import schneaggchatv3mp.composeapp.generated.resources.image
import schneaggchatv3mp.composeapp.generated.resources.message
import schneaggchatv3mp.composeapp.generated.resources.poll
import schneaggchatv3mp.composeapp.generated.resources.unknown_error
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
    val messages by viewModel.messagesState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { focusManager.clearFocus() } // only pointer taps
            }
    ){
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


        // Messages
        val listState = rememberLazyListState()

        //Wenn sich die letzt nachricht ändert denn abescrollen TODO: Testa ob des automatisch abescrollt
        if (messages.isNotEmpty()){
            LaunchedEffect(messages.first()) {
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
            itemsIndexed(messages, key = { _, msg ->
                msg.localPK
            })  { index, message ->
                // Cache date calculations to avoid repeated conversions
                val currentDate = remember(message.sendDate) {
                    val millis = message.sendDate.toLongOrNull()
                    millis?.toLocalDate()
                }
                val nextDate = remember(index, messages) {
                    if (index + 1 < messages.size) {
                        messages[index + 1].sendDate.toLongOrNull()?.toLocalDate()
                    } else {
                        null
                    }
                }

                var answerMessage: Message? = null
                if(message.answerId !=null){
                    answerMessage = messages.find { it.id == message.answerId }
                }

                var showMessageOptionPopup by remember { mutableStateOf(false) }

                Box {

                    MessageViewWithActions(
                        useMD = viewModel.markdownEnabled,
                        selectedChatId = globalViewModel.selectedChat.value.id,
                        message = message,
                        modifier = Modifier,
                        replyMessage = answerMessage,
                        replyMessageOnClick = {
                            val targetIndex = messages.indexOfFirst { it.id == message.answerId }
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
                    MessageOptionPopup(
                        expanded = showMessageOptionPopup,
                        myMessage = message.myMessage,
                        onDismissRequest = { showMessageOptionPopup = false },
                        onReply = {viewModel.updateReplyMessage(message)},
                        onCopy = {
                            copyToClipboard(message.content, clipboardManager)
                            SnackbarManager.showMessage(copiedToClipboardString)
                            showMessageOptionPopup = false
                        },
                        onDelete = {
                            SnackbarManager.showMessage("todo")
                            showMessageOptionPopup = false
                        },
                        onEdit = {
                            SnackbarManager.showMessage("todo")
                            showMessageOptionPopup = false
                        },
                        modifier = Modifier
                    )
                }

                // Show divider only if this message starts a new day
                if (currentDate != nextDate && currentDate != null) {
                    val currentDateMillis = remember(message.sendDate) {
                        message.sendDate.toLongOrNull()
                    }
                    currentDateMillis?.let { DayDivider(it) }
                }
            }
        }

        // Reply view
        if(viewModel.replyMessage != null){
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ){
                Column(
                    modifier = Modifier
                        .weight(1f)
                ){
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
                        selectedChatId = globalViewModel.selectedChat.value.id
                    )
                }
                Column(

                ){
                    IconButton(
                        onClick = {
                            viewModel.updateReplyMessage(null)
                        },
                        modifier = Modifier
                    ){
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.close)
                        )
                    }
                }


            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ){
            // button zum züg addden
            var addMediaDropdownExpanded by remember { mutableStateOf(false) }

            IconButton(
                onClick = {addMediaDropdownExpanded = true},
                modifier = Modifier
                    .padding(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = stringResource(Res.string.add)
                    )
            }
            if(addMediaDropdownExpanded){
                DropdownMenu(
                    expanded = addMediaDropdownExpanded,
                    onDismissRequest = { addMediaDropdownExpanded = false }
                ) {
                    AddMediaOptions.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Row{
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

            // send button
            IconButton(
                onClick = {viewModel.sendMessage()},
                modifier = Modifier
                    .padding(5.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(Res.string.add),
                )
            }

        }

    }
}

@Composable
fun MessageOptionPopup(
    expanded: Boolean,
    myMessage: Boolean,
    onDismissRequest: () -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        DropdownMenuItem(
            text = { Text("Reply") },
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

        if(myMessage) {

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
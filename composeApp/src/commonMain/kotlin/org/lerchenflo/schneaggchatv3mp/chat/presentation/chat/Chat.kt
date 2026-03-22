package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ismoy.imagepickerkmp.domain.config.CameraCaptureConfig
import io.github.ismoy.imagepickerkmp.domain.models.CapturePhotoPreference
import io.github.ismoy.imagepickerkmp.domain.models.CompressionLevel
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageDisplayItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.chat.domain.NotSelected
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.isNotSelected
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ChatViewModel.SendMessageContent
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.DayDivider
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.MessageViewWithActions
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.ReaderBar
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.audio.AudioPlayerView
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.poll.PollDialog
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options.DeleteMessageAlert
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options.MessageDetailsDialog
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options.MessageOptionPopup
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options.ReplyPreview
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.UserButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import org.lerchenflo.schneaggchatv3mp.utilities.formatMillis
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.copied_to_clipboard
import schneaggchatv3mp.composeapp.generated.resources.edit
import schneaggchatv3mp.composeapp.generated.resources.go_back
import schneaggchatv3mp.composeapp.generated.resources.image
import schneaggchatv3mp.composeapp.generated.resources.message
import schneaggchatv3mp.composeapp.generated.resources.poll
import schneaggchatv3mp.composeapp.generated.resources.read_at_time
import schneaggchatv3mp.composeapp.generated.resources.unknown_user


@Composable
fun ChatScreen(
    modifier: Modifier = Modifier
        .fillMaxSize()
){
    val globalViewModel = koinInject<GlobalViewModel>()
    val viewModel = koinViewModel<ChatViewModel>()
    val loggingRepository = koinInject<LoggingRepository>()
    val displayItems by viewModel.messageDisplayState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboard.current.nativeClipboard
    val selectedChat by globalViewModel.selectedChat.collectAsStateWithLifecycle()

    //Leave chat when not logged in
    val ownId = SessionCache.requireLoggedIn()?.userId ?: return

    // Leave chat when not selected
    if (selectedChat.isNotSelected()) {
        println("Unselected chat, navigating back")
        viewModel.onBackClick()
    }

    var addMediaDropdownExpanded by remember { mutableStateOf(false) }


    var showPollDialog by remember { mutableStateOf(false) }
    var showImagePickerDialog by remember { mutableStateOf(false) }


    if (showImagePickerDialog) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showImagePickerDialog ) {

                GalleryPickerLauncher(
                    onPhotosSelected = {
                        viewModel.onImageSelected(it)
                        showImagePickerDialog = false
                    },
                    onError = {
                        showImagePickerDialog = false
                    },
                    onDismiss = {
                        showImagePickerDialog = false
                    },
                    enableCrop = false,
                    allowMultiple = true,
                    cameraCaptureConfig = CameraCaptureConfig(
                        compressionLevel = CompressionLevel.HIGH,
                        preference = CapturePhotoPreference.FAST, //No flash
                        /*
                        cropConfig = CropConfig(
                            enabled = true,
                            aspectRatioLocked = false,
                            circularCrop = true,
                            squareCrop = true,
                            freeformCrop = true
                        ),

                         */

                    )
                )
            }
        }
    }

    if (!showImagePickerDialog) {
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
                    .padding(10.dp, 0.dp, 10.dp, 0.dp),
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


                val userButtonselectedChat by derivedStateOf {
                    when (val chat = selectedChat) {
                        is UserChat -> chat.copy(unreadMessageCount = 0, unsentMessageCount = 0)
                        is GroupChat -> chat.copy(unreadMessageCount = 0, unsentMessageCount = 0)
                        is NotSelected -> chat.copy(unreadMessageCount = 0, unsentMessageCount = 0)
                        else -> chat
                    }

                }

                UserButton(
                    selectedChat = userButtonselectedChat,
                    onClickGes = {
                        viewModel.onChatDetailsClick()
                    },
                    ownId = ownId,
                )
            }
        },
    ) {innerPadding ->
        // The innerPadding contains the height of the topBar
        Column(
            modifier = modifier
                .padding(top = innerPadding.calculateTopPadding())
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
                            //println("Message read by: ${message.readers}")

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
                                        viewModel.onAction(MessageAction.ReplyToMessage(message))
                                    },
                                    onLongPress = {
                                        showMessageOptionPopup = true
                                    },
                                    onAction = viewModel::onAction,
                                    playbackProgress = viewModel.getPlaybackProgress(),
                                    readerMap = item.resolvedReaders,
                                    ownId = ownId
                                )

                                val copiedToClipboardString = stringResource(Res.string.copied_to_clipboard)

                                var showDeleteAlert by remember { mutableStateOf(false) }
                                var showDetailsDialog by remember { mutableStateOf(false) }

                                MessageOptionPopup(
                                    expanded = showMessageOptionPopup,
                                    message = message,
                                    onDismissRequest = { showMessageOptionPopup = false },
                                    onReply = { viewModel.onAction(MessageAction.ReplyToMessage(message)) },
                                    onCopy = {

                                        copyToClipboard(message.content, clipboard)
                                        SnackbarManager.showMessage(copiedToClipboardString)
                                        showMessageOptionPopup = false
                                    },
                                    onDelete = {
                                        showDeleteAlert = true
                                        showMessageOptionPopup = false
                                    },
                                    onEdit = {
                                        viewModel.onAction(MessageAction.StartEditMessage(message))
                                        showMessageOptionPopup = false
                                    },
                                    onDetails = {
                                        showDetailsDialog = true
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
                                            viewModel.onAction(MessageAction.DeleteMessage(item.message))
                                            showDeleteAlert = false
                                        },
                                        message = message,
                                        selectedChatId = globalViewModel.selectedChat.value.id,
                                        ownId = ownId
                                    )
                                }

                                if(showDetailsDialog) {
                                    MessageDetailsDialog(
                                        onDismiss = { showDetailsDialog = false },
                                        message = message,
                                        selectedChatId = globalViewModel.selectedChat.value.id,
                                        ownId = ownId
                                    )
                                }
                            }
                        }
                        is MessageDisplayItem.DateDivider -> {
                            // Render date divider using pre-formatted string
                            DayDivider(item.dateMillis)
                        }
                        is MessageDisplayItem.ReaderBar -> {
                            // show readers as small Profile pictures
                            ReaderBar(item.readerList)
                        }

                    }
                }
            }

            // Reply view
            if (viewModel.replyMessage != null) {
                ReplyPreview(ownId, viewModel, globalViewModel)
            }

            //Inputrow for sending messages
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // button zum züg addden

                val currentContent by viewModel.currentSendContent.collectAsState()
                val currentContentNotEmpty = currentContent !is SendMessageContent.TextContent
                        || (currentContent as? SendMessageContent.TextContent)?.textMessage?.isNotEmpty() == true


                if(!currentContentNotEmpty){
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


                if (addMediaDropdownExpanded) {
                    DropdownMenu(
                        expanded = addMediaDropdownExpanded,
                        onDismissRequest = { addMediaDropdownExpanded = false }
                    ) {
                        AddMediaOptions.entries.forEach { option ->

                            /*
                            val dev = SessionCache.requireLoggedIn()?.developer ?: return@DropdownMenu
                            if(!dev) {
                                if (option == AddMediaOptions.AUDIO) return@forEach //Removes audio if no dev
                            }

                             */


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
                                    option.getAction(
                                        onPollAction = { showPollDialog = true },
                                        onImageAction = { showImagePickerDialog = true }, // todo actions übergeaba
                                    )
                                }
                            )
                        }
                    }
                }

                if(showPollDialog){
                    PollDialog(
                        onDismiss = { showPollDialog = false },
                        onCreatePoll = {
                            println("Poll created: $it")
                            viewModel.createPollMessage(it)
                        }
                    )
                }

                //sendinput (This is a rowscope)
                when (val content = currentContent) {
                    is SendMessageContent.TextContent -> {
                        OutlinedTextField(
                            value = content.textMessage,
                            onValueChange = { newValue ->
                                viewModel.updateSendContent(SendMessageContent.TextContent(newValue))
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .onPreviewKeyEvent { event ->
                                    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                        if (event.isShiftPressed) {
                                            // Todo wida vo string uf TextFieldValue Umbaua
                                            /*
                                            val text = content.textMessage
                                            val selection = content.textFieldValue.selection  // requires TextFieldValue in state
                                            val cursorPos = selection.start
                                            val newText = text.substring(0, cursorPos) + "\n" + text.substring(cursorPos)
                                            val newCursorPos = cursorPos + 1

                                            viewModel.updateSendContent(
                                                ChatViewModel.SendMessageContent.TextContent(
                                                    textFieldValue = TextFieldValue(
                                                        text = newText,
                                                        selection = TextRange(newCursorPos)
                                                    )
                                                )
                                            )

                                             */
                                            return@onPreviewKeyEvent true  // true to consume and prevent double newline
                                        } else {
                                            viewModel.sendMessage(
                                                message = viewModel.currentSendContent.value,
                                                replyTo = viewModel.replyMessage
                                            )
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                    false
                                },
                            placeholder = { Text(stringResource(Res.string.message) + " ...") }
                        )
                    }

                    is SendMessageContent.ImageContent -> {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Multi-image preview row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                content.images.forEach { imageBytes ->
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp) // fixed thumbnail size
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                    ) {
                                        Image(
                                            painter = BitmapPainter(imageBytes.decodeToImageBitmap()),
                                            contentDescription = "image to send",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        // Close button per image
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                                .clickable {
                                                    val remaining = content.images - imageBytes
                                                    viewModel.updateSendContent(
                                                        if (remaining.isEmpty())
                                                            SendMessageContent.TextContent(
                                                                content.text)
                                                        else
                                                            content.copy(images = remaining)
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove image",
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = content.text,
                                onValueChange = { newValue ->
                                    viewModel.updateSendContent(
                                        content.copy(text = newValue)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onPreviewKeyEvent { event ->
                                        if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                            if (event.isShiftPressed) {
                                                viewModel.updateSendContent(content.copy(text = content.text + "\n"))
                                                return@onPreviewKeyEvent false
                                            } else {
                                                viewModel.sendMessage(
                                                    message = viewModel.currentSendContent.value,
                                                    replyTo = viewModel.replyMessage
                                                )
                                                return@onPreviewKeyEvent true
                                            }
                                        }
                                        false
                                    },
                                placeholder = { Text(stringResource(Res.string.message) + " ...") }
                            )
                        }
                    }
                    is SendMessageContent.AudioContent -> {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp) // Match standard TextField height
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if(content.isRecording){
                                // Recording Dot (You could add an InfiniteTransition animation here for pulsing)
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )

                                Spacer(Modifier.width(8.dp))

                                // Timer
                                Text(
                                    text = formatMillis(content.duration),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Visualizer Placeholder
                                // todo actual visualizer
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Simple static bars to represent audio levels
                                    repeat(15) { index ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height((10..24).random().dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                        )
                                    }
                                }

                                val maxVoiceMsgTime = viewModel.getMAX_VOICE_MSG_TIME()

                                if(content.duration > maxVoiceMsgTime*0.8){
                                    Text(
                                        text = formatMillis(maxVoiceMsgTime - content.duration),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }


                            }else{
                                val tmpMsgId = "audio_record_tmp"
                                val progress by viewModel.getPlaybackProgress().collectAsState()
                                val isThisMessagePlaying = progress.messageId == tmpMsgId
                                val currentPosition = if (isThisMessagePlaying) progress.currentPosition else 0L
                                val duration = if (isThisMessagePlaying) progress.duration else 0L
                                val isPlaying = isThisMessagePlaying && progress.isPlaying
                                AudioPlayerView(
                                    isPlaying = isPlaying,
                                    currentPosition = currentPosition,
                                    duration = duration,
                                    onPlay = {
                                        viewModel.playAudio(
                                            messageId = tmpMsgId,
                                            path = (viewModel.currentSendContent.value as SendMessageContent.AudioContent).audioPath
                                        )
                                    },
                                    onPause = {
                                        viewModel.pauseAudio()
                                    },
                                    onSeek = {
                                        viewModel.seekAudio(it)
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                            }


                            // Delete/Discard Button
                            IconButton(
                                onClick = {
                                    // Reset to empty text or previous state
                                    viewModel.stopRecording()
                                    viewModel.updateSendContent(SendMessageContent.TextContent(""))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Discard recording",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }



                if(viewModel.editMessage == null){ // schoua ob mir gad a nachricht bearbeitend

                    // show send when content is not empty or on desktop (no microphone implementation for desktop)
                    if(currentContentNotEmpty || !(SessionCache.requireLoggedIn()?.developer ?: false) || viewModel.isDesktop()){ // todo open to public
                        if((currentContent as? SendMessageContent.AudioContent)?.isRecording ?: false){
                            IconButton(
                                onClick = {
                                    // Reset to empty text or previous state
                                    viewModel.stopRecording()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StopCircle,
                                    contentDescription = "Discard recording",
                                    //tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }else {

                            // send button
                            IconButton(
                                onClick = {
                                    viewModel.sendMessage(
                                        message = viewModel.currentSendContent.value,
                                        replyTo = viewModel.replyMessage
                                    )
                                },
                                modifier = Modifier
                                    .padding(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                )
                            }
                        }
                    }else{
                        IconButton(
                            onClick = {
                                viewModel.startRecording()
                            },
                            modifier = Modifier
                                .padding(5.dp)

                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                            )
                        }
                    }


                }else{

                    //Cancel reply button
                    IconButton(
                        onClick = {
                            viewModel.onAction(MessageAction.CancelEditMessage)
                        },
                        modifier = Modifier
                            .padding(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = stringResource(Res.string.cancel),
                        )
                    }

                    //edit message button
                    IconButton(
                        onClick = {
                            viewModel.editMessage(
                                message = viewModel.editMessage,
                                content = viewModel.currentSendContent.value
                            )
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
    }
    } // end if (!showImagePickerDialog)
}







@Composable
fun ReaderRow(reader: MessageReader) {
    val readMillis = reader.getReadDateAsLong()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder for Avatar/Icon
        ProfilePictureView(
            filepath = reader.readerPicture?:"",
            modifier = Modifier.size(32.dp).padding(end = 8.dp),
        )
        Column {
            Text(
                text = reader.readerName ?: stringResource(Res.string.unknown_user),
                style = MaterialTheme.typography.bodyLarge
            )
            if (readMillis > 0L) {
                Text(
                    text = stringResource(Res.string.read_at_time, millisToTimeDateOrYesterday(readMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
fun copyToClipboard(text: String, clipboard: NativeClipboard) {
    val shareUtils = KoinPlatform.getKoin().get<ShareUtils>()
    shareUtils.copyToClipboard(text, clipboard)
}

enum class AddMediaOptions{
    IMAGE,
    POLL;

    fun toUiText(): UiText = when (this) {
        IMAGE -> UiText.StringResourceText(Res.string.image)
        POLL -> UiText.StringResourceText(Res.string.poll)
        //AUDIO   -> UiText.StringResourceText(Res.string.audio)
    }
    fun getIcon(): ImageVector = when (this) {
        IMAGE -> Icons.Default.Image
        POLL -> Icons.Default.Poll
        //AUDIO   -> Icons.Default.Headphones
    }

    fun getAction(
        onImageAction: () -> Unit,
        onPollAction: () -> Unit,
        //onAudioAction: () -> Unit,
    ): Unit = when (this) {
        IMAGE -> onImageAction()
        POLL -> onPollAction()
        //AUDIO -> onAudioAction()
    }
}

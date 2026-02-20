package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVisibility
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoteOption
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.poll.PollMessageContentView
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.text.TextMessageContentView
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.check
import schneaggchatv3mp.composeapp.generated.resources.something_wrong_message
import kotlin.math.roundToInt

@Composable
fun MessageViewWithActions(
    useMD: Boolean = false,
    selectedChatId: String = "",
    message: Message,
    senderName: String? = null,
    senderColor: Int = 0,
    replyMessage: Message? = null,
    replyMessageOnClick: () -> Unit = {},
    onReplyCall: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onAction: (MessageAction) -> Unit = {},
    modifier: Modifier = Modifier
        .fillMaxWidth()
){


    var contextMenuWidth by remember {
        mutableFloatStateOf(0f)
    }

    val offset = remember {
        Animatable(initialValue = 0f)
    }

    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ){
        Row(
            modifier = Modifier
                .onSizeChanged {
                    contextMenuWidth = it.width.toFloat()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReplyArrow()
        }
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    // custom tap logic to only consume long press
                    awaitEachGesture {
                        val viewConfig = this@pointerInput.viewConfiguration
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // Check for Desktop Right Click (Secondary Button)
                        if (currentEvent.buttons.isSecondaryPressed) { // doesn't work on my machine but does not affect other functionality
                            //println("right click")
                            onLongPress()
                            down.consume()
                        } else {
                            // Handle Long Press for Touch/Left Click
                            val longPress = withTimeoutOrNull(viewConfig.longPressTimeoutMillis) {
                                waitForUpOrCancellation()
                                false
                            }

                            if (longPress == null) {
                                // Long press detected → consume
                                onLongPress() // call callback
                                down.consume()
                            }
                        }


                    }
                }
                .pointerInput(contextMenuWidth) {
                    // detect swipe for reply
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val newOffset = (offset.value + dragAmount)
                                    .coerceIn(0f, contextMenuWidth)
                                offset.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            if(offset.value > contextMenuWidth * 0.9) onReplyCall() // call Reply when swiped 90% of the way

                            scope.launch {
                                offset.animateTo(0f)
                            }
                        }
                    )
                }
        ) {
            MessageView(
                message = message,
                useMD = useMD,
                selectedChatId = selectedChatId,
                senderName = senderName,
                senderColor = senderColor,
                replyMessage = replyMessage,
                replyMessageOnClick = replyMessageOnClick,
                onAction = onAction
            )
        }
    }
}


@Composable
private fun ReplyArrow(
    modifier: Modifier = Modifier.fillMaxHeight()
){
    Box(
        modifier = modifier
    ){
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = "reply",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(
                    start = 16.dp,
                    end = 20.dp
                )
        )
    }
}

@Composable
private fun MessageView(
    modifier: Modifier = Modifier,
    message: Message,
    useMD: Boolean = false,
    selectedChatId: String,
    senderName: String? = null,
    senderColor: Int = 0,
    replyMessage: Message? = null,
    replyMessageOnClick: () -> Unit = {},
    onAction: (MessageAction) -> Unit = {},
)
{

    val mymessage = message.myMessage

    Column(){
        if (replyMessage != null) {

            RepliedMessagePreview(
                message = replyMessage,
                onClick = replyMessageOnClick,
                modifier = Modifier
                    .padding(
                        start = if (mymessage) 40.dp else 0.dp,
                        end = if (mymessage) 0.dp else 40.dp,
                        top = 0.dp,
                        bottom = 0.dp
                    )
            )

        }

        //Ganze breite
        Row(
            modifier = modifier
                .fillMaxWidth(), // Make sure this is here
            horizontalArrangement = if (mymessage) Arrangement.End else Arrangement.Start
        ) {

            MessageContent(
                modifier = Modifier
                    .padding(
                        start = if (mymessage) 40.dp else 0.dp,
                        end = if (mymessage) 0.dp else 40.dp,
                        top = 0.dp,
                        bottom = 5.dp
                    )
                    //.wrapContentSize()
                    .background(
                        color = if (mymessage) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(15.dp)
                    )
                    //.clickable {println(message)}
                    .padding(6.dp),
                message = message,
                useMD = useMD,
                mymessage = mymessage,
                selectedChatId = selectedChatId,
                senderName = senderName,
                senderColor = senderColor,
                onAction = onAction

            )

        }
    }
}

@Composable
fun MessageContent(
    modifier: Modifier = Modifier,
    message: Message,
    useMD: Boolean = false,
    mymessage: Boolean = false,
    selectedChatId: String,
    senderName: String? = null,
    senderColor: Int = 0,
    onAction: (MessageAction) -> Unit = {}
){
    //Farbiger kasten
    Box(
        modifier = modifier

    ){
        //Contentbox gesammt
        Column(
            modifier = Modifier // Remove the modifier parameter here
        ){
            // Show name for groups/other senders
            if (!mymessage && senderName != null && message.groupMessage) {
                Text(
                    text = senderName,
                    color = if (senderColor == 0) Color.Red else Color(senderColor.toLong() and 0xFFFFFFFFL),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            //Contentrow
            Row {
                when(message.msgType){
                    MessageType.TEXT -> TextMessageContentView(
                        useMD = useMD,
                        message = message,
                        myMessage = mymessage
                    )

                    MessageType.POLL -> PollMessageContentView(
                        message = message,
                        useMD = useMD,
                        onAction = onAction
                    )

                    else -> ErrorMessage()
                }
            }

            //Sendedatum / Gelesen row
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 6.dp)
            ) {
                Row(){
                    //zit
                    Text(
                        text = millisToString(message.sendDate.toLong(), format = "HH:mm"),
                        textAlign = TextAlign.End,
                        fontSize = 12.sp,
                    )

                    // gelesen haken
                    // Cache expensive read state calculation

                    val readState = remember(message.sent, message.readers, selectedChatId, mymessage) {
                        when {
                            !mymessage -> ReadState.None
                            !message.sent -> ReadState.NotSent
                            message.isReadById(selectedChatId) -> ReadState.Read
                            else -> ReadState.Sent
                        }
                    }

                    if (readState != ReadState.None) {
                        ReadIndicator(
                            state = readState,
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
    }
}


/**
 * Preview for the message that was replied to in an already sent message
 */
@Composable
private fun RepliedMessagePreview(
    message: Message,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .padding(bottom = 4.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min) // Matches bar height to text height
        ) {
            // Vertical accent bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(
                        color = if (message.myMessage) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                /* todo show the name of the user which is replied to currently
                Text(
                    text = if (message.myMessage) "You" else "Contact", // Replace with actual name if available
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )*/

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private enum class ReadState {
    None,      // Not your message
    NotSent,   // Message failed to send
    Sent,      // One grey check - sent but not read
    Read       // Two blue/primary checks - read
}


@Composable
private fun ReadIndicator(
    state: ReadState,
    modifier: Modifier = Modifier
) {
    when (state) {
        ReadState.None -> { /* Don't show anything */ }

        ReadState.NotSent -> {
            // Cloud off icon for not sent
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Not sent",
                modifier = modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }

        ReadState.Sent -> {
            // Single grey check - sent but not read
            Icon(
                painter = painterResource(Res.drawable.check),
                contentDescription = "Sent",
                modifier = modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }

        ReadState.Read -> {
            // Double checks - read (use onPrimaryContainer for better contrast)
            Box(modifier = modifier.size(14.dp)) {
                Icon(
                    painter = painterResource(Res.drawable.check),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .offset(x = (-2).dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
                Icon(
                    painter = painterResource(Res.drawable.check),
                    contentDescription = "Read",
                    modifier = Modifier
                        .size(14.dp)
                        .offset(x = 2.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}





@Composable
fun ImageMessage(
    messageDto: MessageDto,
    modifier: Modifier = Modifier
){
    Text(
        text = "Image message not implemented yet"
    )
}

@Composable
fun VoiceMessage(
    message: Message,
    modifier: Modifier = Modifier
){
    Text(
        text = "Voice message not implemented yet"
    )
}



@Composable
fun ErrorMessage(
    //message: Message,
    modifier: Modifier = Modifier
){
    Text(
        text = stringResource(Res.string.something_wrong_message)
    )
}

@Composable
fun DayDivider(millis: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        Text(
            text = millisToString(millis, "dd.MM.yyyy"), // you can format this
            modifier = Modifier.padding(vertical = 4.dp)
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Messagepreview(){
    val mymessage = Message(
        msgType = MessageType.TEXT,
        senderId = "1",
        receiverId = "2",
        myMessage = true,
        readByMe = true,
        content = "awdawdaWAwdawdawd",
        readers = emptyList(),
        sent = true,
        sendDate = "13"
    )

    val othermessage = Message(
        msgType = MessageType.TEXT,
        senderId = "1",
        receiverId = "2",
        myMessage = false,
        readByMe = false,
        readers = emptyList(),
        sent = false,
        content = "awdjnapiwdupie aw aiuwdia ajwd aiwudbaiw baawdbaiwudbai waw awiudiauw iauwd aw iuawudb aw",
        sendDate = "12"
    )

    val pollmessage1 = Message(
        msgType = MessageType.POLL,
        senderId = "1",
        receiverId = "2",
        myMessage = false,
        readByMe = false,
        readers = emptyList(),
        sent = false,
        content = "",
        poll = PollMessage(
            creatorId = "awd",
            title = "Titeltitel",
            description = "Descripiton description fortnite skybase description",
            maxAnswers = null,
            customAnswersEnabled = false,
            maxAllowedCustomAnswers = null,
            visibility = PollVisibility.PUBLIC,
            expiresAt = 1212111,
            voteOptions = listOf(
                PollVoteOption(
                    id = "1",
                    text = "Poll option 1",
                    custom = false,
                    creatorId = "waw",
                    voters = emptyList()
                ),

                PollVoteOption(
                    id = "2",
                    text = "Custom option jee",
                    custom = true,
                    creatorId = "awdawd",
                    voters = emptyList()
                )
            )
        ),
        sendDate = "12"
    )

    val pollmessage2 = Message(
        msgType = MessageType.POLL,
        senderId = "1",
        receiverId = "2",
        myMessage = true,
        readByMe = false,
        readers = emptyList(),
        sent = true,
        content = "",
        poll = PollMessage(
            creatorId = "awd",
            title = "Titeltitel",
            description = "Descripiton description fortnite skybase description",
            maxAnswers = 1,
            customAnswersEnabled = true,
            maxAllowedCustomAnswers = 2,
            visibility = PollVisibility.PUBLIC,
            expiresAt = 1212111,
            voteOptions = listOf(
                PollVoteOption(
                    id = "1",
                    text = "Poll option 1",
                    custom = false,
                    creatorId = "waw",
                    voters = emptyList()
                ),

                PollVoteOption(
                    id = "2",
                    text = "Custom option jee",
                    custom = true,
                    creatorId = "awdawd",
                    voters = emptyList()
                )
            )
        ),
        sendDate = "12"
    )



    Column(
        modifier = Modifier.fillMaxSize()
            .padding(16.dp)
    ) {
        MessageViewWithActions(
            message = mymessage
        )
        MessageViewWithActions(
            message = othermessage
        )

        MessageViewWithActions(
            message = pollmessage1
        )

        MessageViewWithActions(
            message = pollmessage2
        )
    }
}

package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.check
import schneaggchatv3mp.composeapp.generated.resources.read
import schneaggchatv3mp.composeapp.generated.resources.something_wrong_message

@Composable
fun MessageView(
    useMD: Boolean = false,
    selectedChatId: String = "",
    message: Message,
    modifier: Modifier = Modifier
        .fillMaxWidth()
){
    val mymessage = message.myMessage

    //Ganze breite
    Row(
        modifier = modifier
            .fillMaxWidth(), // Make sure this is here
        horizontalArrangement = if (mymessage) Arrangement.End else Arrangement.Start
    ) {
        //Farbiger kasten
        Box(
            modifier = Modifier
                .padding(
                    start = if (mymessage) 40.dp else 0.dp,
                    end = if (mymessage) 0.dp else 40.dp,
                    top = 5.dp,
                    bottom = 5.dp
                )
                //.wrapContentSize()
                .background(
                    color = if (mymessage){MaterialTheme.colorScheme.primaryContainer}else {MaterialTheme.colorScheme.secondaryContainer},
                    shape = RoundedCornerShape(15.dp)
                )
                .clickable{
                    println(message)
                }
                .padding(6.dp)

        ){
            //Contentbox gesammt
            Column(
                modifier = Modifier // Remove the modifier parameter here
            ){
                //Contentrow
                Row {
                    when(message.msgType){
                        MessageType.TEXT -> TextMessage(
                            useMD = useMD,
                            messageWithReaders = message,
                            myMessage = mymessage
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
                        // Calculate read state once
                        val readState = remember(message.sent, message.isReadById(selectedChatId), mymessage) {
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
fun TextMessage(
    useMD: Boolean = false,
    messageWithReaders: Message,
    myMessage: Boolean,
    modifier: Modifier = Modifier
){
    SelectionContainer { // damit ma text markiera und kopiera kann (künnt evnt. mit am onLongClick in zukunft interferrieren oder so)
        // ma künnt es chatViewmodel o do instanzieren aber denn würd des für jede message einzeln passiera des isch glob ned des wahre
        if(useMD){ // get setting if if md is enabled
            Markdown(
                content = messageWithReaders.content,
                modifier = modifier
            )
        }else{
            Text(
                text = messageWithReaders.content
            )
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
    messageDto: MessageDto,
    modifier: Modifier = Modifier
){
    Text(
        text = "Voice message not implemented yet"
    )
}

@Composable
fun PollMessage(
    messageDto: MessageDto,
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
private fun messagepreview(){
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

    Column(
        modifier = Modifier.fillMaxSize()
            .padding(16.dp)
    ) {
        for (i in 1..12) {
            MessageView(
                message = mymessage
            )
            MessageView(
                message = othermessage
            )
        }
    }
}

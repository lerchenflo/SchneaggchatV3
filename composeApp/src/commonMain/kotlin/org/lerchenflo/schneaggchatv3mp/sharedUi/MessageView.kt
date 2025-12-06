package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.check
import schneaggchatv3mp.composeapp.generated.resources.read
import schneaggchatv3mp.composeapp.generated.resources.something_wrong_message

@Preview
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
            .clickable{
                println(message)
                println("Read by me: $mymessage")
            }
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
                .wrapContentSize()
                .background(
                    color = if (mymessage){MaterialTheme.colorScheme.primaryContainer}else {MaterialTheme.colorScheme.secondaryContainer},
                    shape = RoundedCornerShape(15.dp)
                )
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
                        if(message.isReadById(selectedChatId) && mymessage){
                            Icon(
                                painter = painterResource(Res.drawable.check),
                                contentDescription = stringResource(Res.string.read),
                                modifier = Modifier
                                    .size(14.dp)
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
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        Text(
            text = millisToString(millis, "dd.MM.yyyy"), // you can format this
            modifier = Modifier.padding(vertical = 4.dp)
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
    }
}

package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.elements.MarkdownText
import com.mikepenz.markdown.m3.Markdown
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.database.Message
import org.lerchenflo.schneaggchatv3mp.database.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.network.GROUPTEXTMESSAGE
import org.lerchenflo.schneaggchatv3mp.network.SINGLETEXTMESSAGE
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.check
import schneaggchatv3mp.composeapp.generated.resources.go_back
import schneaggchatv3mp.composeapp.generated.resources.read

@Preview
@Composable
fun MessageView(
    messagewithreaders: MessageWithReaders,
    modifier: Modifier = Modifier
        .fillMaxWidth()

){
    val mymessage = messagewithreaders.message.isMyMessage()

    //Ganze breite
    Row(
        modifier = Modifier
            .fillMaxWidth(),
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
                    modifier = modifier
                ){
                    //Contentrow
                    Row(

                    ) {
                        when(messagewithreaders.message.msgType){
                            SINGLETEXTMESSAGE -> TextMessage(messagewithreaders, mymessage)
                            GROUPTEXTMESSAGE -> TextMessage(messagewithreaders, mymessage)

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
                                text = millisToString(messagewithreaders.message.sendDate?.toLong() ?: 0, format = "HH:mm"),
                                textAlign = TextAlign.End,
                                fontSize = 12.sp,
                                modifier = Modifier
                            )
                            // gelesen haken
                            if(true && mymessage){ // todo flos abfrage
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
    messageWithReaders: MessageWithReaders,
    myMessage: Boolean,
    modifier: Modifier = Modifier
){

        Markdown(
            content = messageWithReaders.message.content ?: "",
            modifier = modifier
        )
    
}

@Composable
fun ImageMessage(
    message: Message,
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
fun PollMessage(
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
        text = "Something went wrong with this message" //todo strings und styling
    )
}


package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.database.Message
import org.lerchenflo.schneaggchatv3mp.database.MessageWithReaders
import org.lerchenflo.schneaggchatv3mp.network.GROUPTEXTMESSAGE
import org.lerchenflo.schneaggchatv3mp.network.SINGLETEXTMESSAGE
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString

@Preview
@Composable
fun MessageView(
    messagewithreaders: MessageWithReaders,
    modifier: Modifier = Modifier
        .fillMaxWidth()

){
    val mymessage = messagewithreaders.message.isMyMessage()

    //gesamtbox
    Column(
        modifier = modifier
    ){
        //Contentrow
        Row(

        ) {  }

        //Sendedatum / Gelesen row
        Row(

        ) {  }
    }













    Box(
        modifier = modifier
            .padding(
                bottom = 10.dp
            )
            .background(Color.Transparent)

    ){
        Column(
            modifier = Modifier
                .padding(
                    start = if (mymessage){45.dp}else{15.dp},
                    end = if (mymessage){15.dp}else{45.dp},
                    top = 5.dp,
                    bottom = 5.dp
                )
                .background(
                    color = if (mymessage){MaterialTheme.colorScheme.primaryContainer}else {MaterialTheme.colorScheme.secondaryContainer},
                    shape = RoundedCornerShape(15.dp)
                )
                .wrapContentSize()
        ){
            // je nach msgtype unterschiedliche anzeigen
            when(messagewithreaders.message.msgType){
                SINGLETEXTMESSAGE -> TextMessage(messagewithreaders, mymessage)
                GROUPTEXTMESSAGE -> TextMessage(messagewithreaders, mymessage)

                else -> ErrorMessage()
            }

            // Time and Read info
            Row(
                modifier = Modifier
            ){
                Text(
                    text = millisToString(messagewithreaders.message.sendDate?.toLong() ?: 0, format = "HH:mm"),
                    textAlign = TextAlign.End
                )
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
    Text(
        text = messageWithReaders.message.content ?: "",
        modifier = modifier,

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


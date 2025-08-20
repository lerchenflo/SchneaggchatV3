package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.database.Message
import org.lerchenflo.schneaggchatv3mp.network.GROUPTEXTMESSAGE
import org.lerchenflo.schneaggchatv3mp.network.SINGLETEXTMESSAGE
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString

@Preview
@Composable
fun MessageView(
    message: Message,
    modifier: Modifier = Modifier
        .fillMaxWidth()
){
    Box(
        modifier = modifier
    ){
        Column(){
            // je nach msgtype unterschiedliche anzeigen
            when(message.msgType){
                SINGLETEXTMESSAGE -> TextMessage(message)
                GROUPTEXTMESSAGE -> TextMessage(message)

                else -> ErrorMessage()
            }

            // Time and Read info
            Row(){
                Text(
                    text = millisToString(message.sendDate?.toLong() ?: 0, format = "HH:mm")
                )
            }

        }


    }
}

@Composable
fun TextMessage(
    message: Message,
    modifier: Modifier = Modifier
){


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


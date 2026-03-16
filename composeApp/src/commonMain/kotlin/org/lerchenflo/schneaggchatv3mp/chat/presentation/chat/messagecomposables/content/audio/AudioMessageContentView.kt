package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.audio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction

@Composable
fun AudioMessageContentView(
    message: Message,
    modifier: Modifier = Modifier,
    useMD: Boolean = false,
    myMessage: Boolean = false,
    onAction: (MessageAction) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(0.67f)
    ) {

        IconButton(
            onClick = {
                onAction(MessageAction.PlayAudio(
                    messageId = message.id?:"",
                    audioPath = message.audioPath?:""
                ))
            }
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "play",
                tint = MaterialTheme.colorScheme.error
            )
        }

        /*
        Slider(
            modifier = Modifier.weight(1f),
            value = progress.currentPosition.toFloat(),
            valueRange = 0f..progress.duration.toFloat(),
            onValueChange = { /* seek logic */ }
        )

         */




        Text(
            text = "Audio Message Ui not implemented yet"
        )
        println(message.audioPath)

    }
}
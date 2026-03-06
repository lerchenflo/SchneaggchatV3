package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.check
import schneaggchatv3mp.composeapp.generated.resources.something_wrong_message

@Composable
fun ReplyArrow(
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


@Composable
fun ReaderBar(readers: List<MessageReader>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.End // Aligns the Column's children to the right
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            // This is the key line to align items to the right:
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (reader in readers) {

                val ownId by SessionCache.ownId.collectAsState()
                if(reader.readerId != ownId){
                    ProfilePictureView(
                        filepath = reader.readerPicture ?: "",
                        modifier = Modifier
                            .padding(start = 2.dp) // Adds a tiny gap between the faces
                            .size(16.dp),
                    )
                }


            }
        }
    }
}


@Composable
fun ReadIndicator(
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

enum class ReadState {
    None,      // Not your message
    NotSent,   // Message failed to send
    Sent,      // One gray check - sent but not read
    Read       // Two blue/primary checks - read
}
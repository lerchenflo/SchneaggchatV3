package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.ErrorMessage
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.ReadIndicator
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.ReadState
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.image.ImageMessageContentView
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.poll.PollMessageContentView
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.text.TextMessageContentView
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString

@Composable
fun MessageContent(
    ownId: String,
    modifier: Modifier = Modifier,
    message: Message,
    useMD: Boolean = false,
    mymessage: Boolean = false,
    selectedChatId: String,
    senderName: String? = null,
    senderColor: Int = 0,
    readerMap: Map<String, String> = emptyMap(),
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
            if (!mymessage && message.senderAsString != "" && message.groupMessage) {
                Text(
                    text = message.senderAsString,
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
                        myMessage = mymessage,
                        onAction = onAction,
                        readerMap = readerMap,
                        ownId = ownId
                    )

                    MessageType.IMAGE -> ImageMessageContentView(
                        message = message,
                        modifier = Modifier,
                        myMessage = mymessage,
                        useMD = useMD
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

                    //TODO: Readermap show pictures etc?

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
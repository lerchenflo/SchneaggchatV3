package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options.RepliedMessagePreview
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.MessageContent


@Composable
fun MessageView(
    ownId: String,
    modifier: Modifier = Modifier,
    message: Message,
    useMD: Boolean = false,
    selectedChatId: String,
    senderName: String? = null,
    senderColor: Int = 0,
    readerMap: Map<String,String> = emptyMap(),
    replyMessage: Message? = null,
    replyMessageOnClick: () -> Unit = {},
    onAction: (MessageAction) -> Unit = {},
)
{

    val mymessage = message.myMessage

    Column {
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
                .fillMaxWidth(),
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
                readerMap = readerMap,
                onAction = onAction,
                ownId = ownId
            )

        }
    }
}










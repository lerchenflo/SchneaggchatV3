package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.SenderInfo
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.MessageContent
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.close

@Composable
fun ReplyPreview(
    ownId: String,
    message: Message,
    sender: SenderInfo?,
    useMD: Boolean,
    selectedChatId: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
){


    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val maxPreviewHeight = maxHeight * 0.3f //Reply preview may use at most 30% of the available screen height, so it can't push the keyboard/input off screen

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxPreviewHeight)
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()) //Content can be scrolled if height is too high
            ) {
                val alphaValue = 0.8f
                MessageContent(
                    modifier = Modifier
                        //.wrapContentSize()
                        .background(
                            color = if (message.myMessage) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = alphaValue)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alphaValue)
                            },
                            shape = RoundedCornerShape(15.dp)
                        )
                        .padding(6.dp),
                    message = message,
                    useMD = useMD,
                    mymessage = message.myMessage,
                    selectedChatId = selectedChatId,
                    sender = sender,
                    ownId = ownId,
                )
            }
            Column {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.close)
                    )
                }
            }
        }
    }
}
package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.options

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ChatViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.MessageContent
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.close

@Composable
fun ReplyPreview(viewModel: ChatViewModel, globalViewModel: GlobalViewModel){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            val alphaValue = 0.8f
            MessageContent(
                modifier = Modifier
                    //.wrapContentSize()
                    .background(
                        color = if (viewModel.replyMessage!!.myMessage) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = alphaValue)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alphaValue)
                        },
                        shape = RoundedCornerShape(15.dp)
                    )
                    .padding(6.dp),
                message = viewModel.replyMessage!!,
                useMD = viewModel.markdownEnabled,
                selectedChatId = globalViewModel.selectedChat.value.id,
                senderColor = viewModel.replyMessage!!.senderColor
            )
        }
        Column {
            IconButton(
                onClick = {
                    viewModel.updateReplyMessage(null)
                },
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
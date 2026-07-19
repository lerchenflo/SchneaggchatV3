package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.ComboText

@Composable
fun TextMessageContentView(
    useMD: Boolean = false,
    message: Message,
    myMessage: Boolean,
    modifier: Modifier = Modifier
){

    ComboText(
        text = message.content,
        useMD = useMD,
        textColor = if (myMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

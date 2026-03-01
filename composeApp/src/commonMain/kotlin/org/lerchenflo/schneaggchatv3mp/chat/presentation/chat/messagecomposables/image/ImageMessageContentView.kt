package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.image

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message

@Composable
fun ImageMessageContentView(
    message: Message,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = message.content,
        modifier = Modifier.size(240.dp),
        contentDescription = "image",
    )
}
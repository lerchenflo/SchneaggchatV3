package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.image

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message

@Composable
fun ImageMessageContentView(
    message: Message,
    modifier: Modifier = Modifier
) {
    var showFullscreen by remember { mutableStateOf(false) }

    AsyncImage(
        model = message.content,
        modifier = modifier
            .clickable{
                showFullscreen = true
            },
        contentDescription = "image",
    )

    if (showFullscreen) {
        FullscreenImageDialog(
            imageUrl = message.content,
            onDismiss = { showFullscreen = false }
        )
    }
}
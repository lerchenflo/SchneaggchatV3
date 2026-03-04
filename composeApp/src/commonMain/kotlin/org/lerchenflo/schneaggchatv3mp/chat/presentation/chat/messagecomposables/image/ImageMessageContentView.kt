package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.image

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager

@Composable
fun ImageMessageContentView(
    message: Message,
    modifier: Modifier = Modifier
) {
    var showFullscreen by remember { mutableStateOf(false) }


    Column {
        AsyncImage(
            model = message.pictureUrl ?: "",
            modifier = modifier
                .fillMaxWidth(0.67f)
                .widthIn(min = 200.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showFullscreen = true },
                        onLongPress = {}
                    )
                },
            contentDescription = "image",
            contentScale = ContentScale.FillWidth,
        )


        if (message.content.isNotEmpty()) {
            Text(
                text = message.content
            )
        }
    }

    if (showFullscreen) {
        FullscreenImageDialog(
            imageUrl = message.content,
            onDismiss = { showFullscreen = false }
        )
    }
}
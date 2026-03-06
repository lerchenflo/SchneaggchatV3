package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.composables.content.image

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.composables.content.text.TextMessageContentView

@Composable
fun ImageMessageContentView(
    message: Message,
    modifier: Modifier = Modifier,
    useMD: Boolean = false,
    myMessage: Boolean = false
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
                }
                .clip(RoundedCornerShape(15.dp)),
            contentDescription = "image",
            contentScale = ContentScale.FillWidth,
        )

        if (message.content.isNotEmpty()) {
            TextMessageContentView(
                useMD = useMD,
                message = message,
                myMessage = myMessage,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .widthIn(min = 200.dp) // ← match image min-width so bubble doesn't collapse
            )
        }
    }

    if (showFullscreen) {
        FullscreenImageDialog(
            imageUrl = message.pictureUrl ?: "", // ← also fix this while you're here
            onDismiss = { showFullscreen = false }
        )
    }
}
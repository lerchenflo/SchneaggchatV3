package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.image

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.text.TextMessageContentView
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer

@Composable
fun ImageMessageContentView(
    message: Message,
    modifier: Modifier = Modifier,
    useMD: Boolean = false,
    myMessage: Boolean = false
) {
    var showFullscreen by remember { mutableStateOf(false) }

    val imageModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(15.dp))
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { showFullscreen = true },
                onLongPress = {}
            )
        }


    Column(
        modifier = Modifier.fillMaxWidth(0.67f)
    ) {
        if (message.pictureUrl != null) {
            AsyncImage(
                model = message.pictureUrl,
                modifier = imageModifier,
                contentDescription = "image",
                //contentScale = ContentScale.FillWidth,
            )
        }else {
            Image(
                painter = painterResource(Res.drawable.icon_nutzer),
                contentDescription = "image loading error",
                modifier = imageModifier
            )
        }

        if (message.content.isNotEmpty()) {
            TextMessageContentView(
                useMD = useMD,
                message = message,
                myMessage = myMessage,
                modifier = Modifier
                    .padding(top = 4.dp)
            )
        }
    }

    if (showFullscreen) {
        FullscreenImageDialog(
            imageUrl = message.pictureUrl ?: "",
            onDismiss = { showFullscreen = false }
        )
    }
}
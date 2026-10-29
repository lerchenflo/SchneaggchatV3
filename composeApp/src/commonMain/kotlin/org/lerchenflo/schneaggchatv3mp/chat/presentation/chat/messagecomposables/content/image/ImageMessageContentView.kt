package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import org.jetbrains.compose.resources.painterResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.MessageAction
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.text.TextMessageContentView
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer

@Composable
fun ImageMessageContentView(
    message: Message,
    modifier: Modifier = Modifier,
    useMD: Boolean = false,
    myMessage: Boolean = false,
    onAction: (MessageAction) -> Unit = {}
) {
    var showFullscreen by remember { mutableStateOf(false) }

    // Width / height of the loaded picture. Sizing the node to it makes the clip below follow the
    // picture's own edges - with a node larger than the drawn picture the rounded corners would
    // fall on empty space next to it.
    var aspectRatio by remember(message.pictureUrl) { mutableStateOf<Float?>(null) }

    val imageModifier = modifier
        .fillMaxWidth()
        .then(aspectRatio?.let { Modifier.aspectRatio(it) } ?: Modifier)
        // Display only, the picture itself stays square-cornered. 9dp = the bubble's 15dp corner minus
        // its 6dp padding, so the picture's corners run parallel to the bubble's.
        .clip(RoundedCornerShape(9.dp))
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
                contentScale = ContentScale.Crop,
                onSuccess = { state ->
                    val image = state.result.image
                    if (image.width > 0 && image.height > 0) aspectRatio = image.width.toFloat() / image.height
                },
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
            onDismiss = { showFullscreen = false },
            onDownload = {
                 onAction(MessageAction.DownloadImage(message.pictureUrl?:"", "Schneaggchat_" + message.sendDate))
            }
        )
    }
}
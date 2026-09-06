package org.lerchenflo.schneaggchatv3mp.sharedUi.picture

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer

/**
 * The placeholder every avatar falls back to. Decoding it costs a few milliseconds, so it is
 * resolved once per screen and handed down instead of once per row - inside a list row it would
 * otherwise be decoded on the main thread during measure.
 */
@Composable
fun rememberProfilePicturePlaceholder(): Painter = painterResource(Res.drawable.icon_nutzer)

/**
 * The request is deliberately left to size itself from the layout - the same composable renders a
 * 16.dp reader avatar and a 200.dp chat details header, so a fixed decode size would blur the
 * large ones and oversize the small ones.
 */
@Composable
fun ProfilePictureView(
    filepath: String, //Absolute filepath
    modifier: Modifier = Modifier
        .size(40.dp)
        .padding(end = 8.dp)
        .clip(CircleShape),
    contentDescription: String? = "Profile picture",
    placeholder: Painter = rememberProfilePicturePlaceholder()
) {

    AsyncImage(
        model = filepath,
        contentDescription = contentDescription,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        modifier = modifier.clip(CircleShape)
    )
}

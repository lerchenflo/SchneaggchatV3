package org.lerchenflo.schneaggchatv3mp.sharedUi.picture

import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.remember
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
 * The placeholder shrunk by [insetFraction] on every side. The icon fills its square edge to edge,
 * so a circular crop (and a ring around it) cuts off the head and shoulders; large avatars use this
 * to keep the whole figure inside the circle.
 */
@Composable
fun rememberInsetProfilePicturePlaceholder(insetFraction: Float): Painter {
    val placeholder = rememberProfilePicturePlaceholder()
    return remember(placeholder, insetFraction) { InsetPainter(placeholder, insetFraction) }
}

private class InsetPainter(
    private val painter: Painter,
    private val insetFraction: Float,
) : Painter() {
    override val intrinsicSize: Size get() = painter.intrinsicSize

    override fun DrawScope.onDraw() {
        inset(horizontal = size.width * insetFraction, vertical = size.height * insetFraction) {
            with(painter) { draw(size) }
        }
    }
}

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

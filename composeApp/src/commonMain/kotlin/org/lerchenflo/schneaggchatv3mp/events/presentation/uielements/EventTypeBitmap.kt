package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.events.domain.icon

/**
 * Renders [type]'s Material icon onto a filled disc, so it can be sent as an event's group
 * profile picture at creation time - there is no dedicated artwork per event type.
 */
@Composable
fun rememberEventTypeIconBitmap(type: EventType, sizePx: Int = 512): ImageBitmap {
    val painter = rememberVectorPainter(type.icon())
    val background = MaterialTheme.colorScheme.primaryContainer
    val tint = MaterialTheme.colorScheme.onPrimaryContainer

    return remember(type, background, tint, sizePx) {
        val bitmap = ImageBitmap(sizePx, sizePx, ImageBitmapConfig.Argb8888, hasAlpha = true)
        val size = Size(sizePx.toFloat(), sizePx.toFloat())
        val iconSize = size * 0.6f
        val iconOffset = Offset((size.width - iconSize.width) / 2f, (size.height - iconSize.height) / 2f)

        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(bitmap),
            size = size,
        ) {
            drawCircle(color = background, radius = size.minDimension / 2f)
            translate(left = iconOffset.x, top = iconOffset.y) {
                with(painter) {
                    draw(size = iconSize, colorFilter = ColorFilter.tint(tint))
                }
            }
        }

        bitmap
    }
}

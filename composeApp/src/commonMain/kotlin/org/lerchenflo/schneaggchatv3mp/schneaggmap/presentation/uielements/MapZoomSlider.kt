package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

//Zoom-level tiers for the thumb icon, in the same spirit as Snap Map's helicopter/bird/bee/shoe
//progression: the icon tells you roughly what scale you're looking at without reading numbers.
private val ZOOM_TIER_ICONS: List<Pair<Double, ImageVector>> = listOf(
    6.0 to Icons.Default.Public,
    11.0 to Icons.Default.Terrain,
    15.0 to Icons.Default.LocationCity,
    Double.MAX_VALUE to Icons.AutoMirrored.Filled.DirectionsWalk,
)

private fun zoomTierIcon(zoom: Double): ImageVector =
    ZOOM_TIER_ICONS.first { zoom < it.first }.second

/**
 * A vertical zoom scrollbar for the map, modeled after Snap Map's: it stays invisible until the
 * user actually starts zooming (by dragging this handle, or pinch-zooming the map itself), then
 * fades back out shortly after - the same "disappearing control" idiom this app already uses for
 * [org.maplibre.compose.material3.DisappearingScaleBar] and [org.maplibre.compose.material3.DisappearingCompassButton].
 * The touch target stays mounted at all times so the hidden handle remains grabbable.
 */
@Composable
fun MapZoomSlider(
    zoom: Double,
    onZoomChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    minZoom: Double = 1.0,
    maxZoom: Double = 20.0,
    trackWidth: Dp = 6.dp,
    trackHeight: Dp = 260.dp,
    thumbSize: Dp = 32.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    visibilityDuration: Duration = 1500.milliseconds,
) {
    val density = LocalDensity.current

    var trackHeightPx by remember { mutableStateOf(0f) }
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val maxOffsetPx = (trackHeightPx - thumbSizePx).coerceAtLeast(0f)

    //0f = thumb at the top (maxZoom), maxOffsetPx = thumb at the bottom (minZoom). Plain state
    //(not Animatable) so the thumb tracks the finger 1:1 with zero coroutine-dispatch lag.
    var thumbOffsetPx by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    //Hidden by default - fades in whenever the zoom actually changes (drag or pinch), then
    //fades back out after visibilityDuration of no further change, unless still being dragged.
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(zoom) {
        isVisible = true
        delay(visibilityDuration)
        if (!isDragging) isVisible = false
    }
    val alpha by animateFloatAsState(if (isVisible || isDragging) 1f else 0f)

    LaunchedEffect(zoom, maxOffsetPx) {
        if (isDragging || maxOffsetPx <= 0f) return@LaunchedEffect
        val fraction = ((zoom - minZoom) / (maxZoom - minZoom)).coerceIn(0.0, 1.0)
        thumbOffsetPx = ((1.0 - fraction) * maxOffsetPx).toFloat()
    }

    // Touch target spans the thumb's width and stays mounted regardless of visibility, so the
    // hidden handle can still be found and grabbed - only the drawn track/thumb fade in and out.
    Box(
        modifier = modifier
            .width(thumbSize)
            .height(trackHeight)
            .onSizeChanged { size -> trackHeightPx = size.height.toFloat() }
            .pointerInput(minZoom, maxZoom) {
                detectDragGestures(
                    onDragStart = { isDragging = true; isVisible = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = (thumbOffsetPx + dragAmount.y).coerceIn(0f, maxOffsetPx)
                        thumbOffsetPx = newOffset
                        if (maxOffsetPx > 0f) {
                            val fraction = 1.0 - (newOffset / maxOffsetPx)
                            onZoomChange((minZoom + fraction * (maxZoom - minZoom)).coerceIn(minZoom, maxZoom))
                        }
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Thin decorative track line, slimmer than the thumb (Snap Map style)
        Box(
            modifier = Modifier
                .alpha(alpha)
                .width(trackWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(trackWidth / 2))
                .background(trackColor)
        )

        Box(
            modifier = Modifier
                .alpha(alpha)
                .offset { IntOffset(0, thumbOffsetPx.roundToInt()) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = zoomTierIcon(zoom),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(thumbSize * 0.6f)
            )
        }
    }
}

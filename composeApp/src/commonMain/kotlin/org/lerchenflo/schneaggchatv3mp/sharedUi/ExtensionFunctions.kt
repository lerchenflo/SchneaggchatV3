package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.clearFocusOnTap() : Modifier {
    val focusManager = LocalFocusManager.current
    // IOSKEYBOARDFIX: on iOS clearFocus() alone does not always resign the first responder,
    // so the keyboard stays up. Every dialog in this app already pairs it with hide().
    val keyboardController = LocalSoftwareKeyboardController.current
    return this.pointerInput(Unit) {
        detectTapGestures {
            keyboardController?.hide() // IOSKEYBOARDFIX
            focusManager.clearFocus()
        }
    }
}

// Desktop mouse wheels only report scroll input on the y axis, but horizontalScroll only
// reacts to its own (x) axis - so without this, a horizontally scrolling row can't be
// scrolled with a mouse wheel on desktop, only by dragging.
private val MOUSE_WHEEL_SCROLL_STEP = 48.dp

/**
 * Drop-in replacement for [Modifier.horizontalScroll] that also reacts to a desktop mouse
 * wheel by forwarding its (vertical) scroll delta into [state].
 */
@Composable
fun Modifier.horizontalScrollWithMouseWheel(state: ScrollState = rememberScrollState()): Modifier {
    val density = LocalDensity.current
    val mouseWheelScrollStepPx = with(density) { MOUSE_WHEEL_SCROLL_STEP.toPx() }

    return this
        .horizontalScroll(state)
        .pointerInput(state) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (scrollDelta != 0f) {
                            state.dispatchRawDelta(scrollDelta * mouseWheelScrollStepPx)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
        }
}

private const val MOUSE_WHEEL_ZOOM_IN = 1.1f
private const val MOUSE_WHEEL_ZOOM_OUT = 0.9f

/**
 * Hoisted zoom/pan state for [Modifier.zoomable]. Hoisted so callers can react to [scale]
 * (e.g. morphing a clip shape while zooming).
 *
 * @param clampToBounds keep the zoomed content covering its own layout bounds while panning.
 * When false the content can be panned freely, partly off screen.
 */
@Stable
class ZoomState(
    private val maxScale: Float = 5f,
    private val clampToBounds: Boolean = true
) {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    internal var size: IntSize = IntSize.Zero

    // Applies a zoom change centered on `centroid` (in the content's own, pre-transform local
    // coordinates), plus an optional pan delta. This keeps the point under the fingers/cursor
    // stationary while zooming, instead of always zooming around the content center.
    internal fun applyZoom(centroid: Offset, zoomChange: Float, panChange: Offset = Offset.Zero) {
        val newScale = (scale * zoomChange).coerceIn(1f, maxScale)

        // Vector from content center to the zoom centroid.
        val centroidX = centroid.x - size.width / 2f
        val centroidY = centroid.y - size.height / 2f

        var newOffsetX = centroidX - newScale * (centroidX - offset.x) / scale + panChange.x
        var newOffsetY = centroidY - newScale * (centroidY - offset.y) / scale + panChange.y

        if (newScale <= 1f) {
            newOffsetX = 0f
            newOffsetY = 0f
        } else if (clampToBounds) {
            val maxOffsetX = size.width * (newScale - 1f) / 2f
            val maxOffsetY = size.height * (newScale - 1f) / 2f
            newOffsetX = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
            newOffsetY = newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
        }

        scale = newScale
        offset = Offset(newOffsetX, newOffsetY)
    }
}

@Composable
fun rememberZoomState(maxScale: Float = 5f, clampToBounds: Boolean = true): ZoomState =
    remember(maxScale, clampToBounds) { ZoomState(maxScale, clampToBounds) }

/**
 * Pinch-to-zoom + pan on touch, and mouse wheel zoom on desktop. Zooms around the
 * finger/cursor position.
 *
 * Modifiers chained after this one live inside the zoomed layer, so e.g. a clipping
 * `graphicsLayer { shape = ...; clip = true }` scales along with the content.
 */
fun Modifier.zoomable(state: ZoomState): Modifier = this
    .onSizeChanged { state.size = it }
    .pointerInput(state) {
        detectTransformGestures { centroid, pan, zoom, _ ->
            state.applyZoom(centroid, zoom, pan)
        }
    }
    .pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val change = event.changes.firstOrNull() ?: continue
                    val scrollDelta = change.scrollDelta.y
                    if (scrollDelta != 0f) {
                        val zoomFactor = if (scrollDelta < 0) MOUSE_WHEEL_ZOOM_IN else MOUSE_WHEEL_ZOOM_OUT
                        state.applyZoom(change.position, zoomFactor)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
    }
    .graphicsLayer {
        scaleX = state.scale
        scaleY = state.scale
        translationX = state.offset.x
        translationY = state.offset.y
    }

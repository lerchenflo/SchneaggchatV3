package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position

/**
 * Custom layout modifier to position a composable based on a specific geographic projection [offset]
 * and alignment [anchor].
 */
fun Modifier.absoluteOffsetWithAnchor(
    offset: DpOffset,
    anchor: Alignment = Alignment.Center
): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)

    // Convert DpOffset to pixels
    val xPx = offset.x.roundToPx()
    val yPx = offset.y.roundToPx()

    val width = placeable.width
    val height = placeable.height

    // Calculate the top-left placement coordinate adjusted for the anchor point
    val alignedOffset = when (anchor) {
        Alignment.TopStart -> IntOffset(xPx, yPx)
        Alignment.TopCenter -> IntOffset(xPx - width / 2, yPx)
        Alignment.TopEnd -> IntOffset(xPx - width, yPx)
        Alignment.CenterStart -> IntOffset(xPx, yPx - height / 2)
        Alignment.Center -> IntOffset(xPx - width / 2, yPx - height / 2)
        Alignment.CenterEnd -> IntOffset(xPx - width, yPx - height / 2)
        Alignment.BottomStart -> IntOffset(xPx, yPx - height)
        Alignment.BottomCenter -> IntOffset(xPx - width / 2, yPx - height)
        Alignment.BottomEnd -> IntOffset(xPx - width, yPx - height)
        else -> IntOffset(xPx - width / 2, yPx - height / 2)
    }

    layout(placeable.width, placeable.height) {
        placeable.place(alignedOffset)
    }
}

@Composable
fun MapIcon(
    cameraState: CameraState,
    targetPosition: Position,
    modifier: Modifier = Modifier,
    anchor: Alignment = Alignment.Center,
    content: @Composable (BoxScope.() -> Unit),
) {
    // 1. Project the geographic position to screen pixels
    val dpOffset = remember(targetPosition, cameraState.position, cameraState.position.zoom, cameraState.position.bearing, cameraState.position.tilt) {
        cameraState.projection?.screenLocationFromPosition(targetPosition)
    }

    // If projection isn't ready, don't render anything
    val target = dpOffset ?: return

    // 2. Position the content using our custom anchor layout modifier
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.absoluteOffsetWithAnchor(target, anchor)
        ) {
            content()
        }
    }
}
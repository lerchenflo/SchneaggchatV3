package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.content.image

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage

@Composable
fun FullscreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    var containerWidth by remember { mutableStateOf(0f) }
    var containerHeight by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Applies a zoom change centered on `centroid` (in the AsyncImage's own,
    // pre-transform local coordinates), plus an optional pan delta.
    // This is what keeps the point under your fingers/cursor stationary while zooming,
    // instead of the image always zooming around its own center.
    fun applyZoom(centroid: Offset, zoomChange: Float, panChange: Offset = Offset.Zero) {
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)

        // Vector from container center to the zoom centroid.
        val centroidX = centroid.x - containerWidth / 2f
        val centroidY = centroid.y - containerHeight / 2f

        if (scale != 0f) {
            offsetX = centroidX - newScale * (centroidX - offsetX) / scale + panChange.x
            offsetY = centroidY - newScale * (centroidY - offsetY) / scale + panChange.y
        }
        scale = newScale

        if (newScale > 1f) {
            val maxOffsetX = containerWidth * (newScale - 1f) / 2f
            val maxOffsetY = containerHeight * (newScale - 1f) / 2f
            offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
            offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full screen image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged {
                        containerWidth = it.width.toFloat()
                        containerHeight = it.height.toFloat()
                    }
                    .pointerInput(Unit) {                    // touch pinch-to-zoom + pan
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            applyZoom(centroid, zoom, pan)
                        }
                    }
                    .pointerInput(Unit) {                    // desktop scroll wheel zoom
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Scroll) {
                                    val change = event.changes.firstOrNull()
                                    val scrollDelta = change?.scrollDelta?.y ?: 0f
                                    val zoomFactor = if (scrollDelta < 0) 1.1f else 0.9f
                                    val centroid = change?.position
                                        ?: Offset(containerWidth / 2f, containerHeight / 2f)

                                    applyZoom(centroid, zoomFactor)

                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
            )

            // Top-right icons container
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                // Download button
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Close button
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
package org.lerchenflo.schneaggchatv3mp.sharedUi.picture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import org.lerchenflo.schneaggchatv3mp.sharedUi.rememberZoomState
import org.lerchenflo.schneaggchatv3mp.sharedUi.zoomable

// Scale at which the circular clip has fully morphed into the square image
private const val CIRCLE_TO_SQUARE_SCALE = 1.5f

// Zoagt es Profilbild groß a
@Composable
fun ProfilePictureBigDialog(
    onDismiss: () -> Unit,
    filepath: String,
    onEdit: () -> Unit = {},
    showEditButton: Boolean = false
) {
    // Full-width dialog window + no pan clamping, so the zoomed image can grow past the
    // circle and the dialog bounds instead of being cut off inside them.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val zoomState = rememberZoomState(clampToBounds = false)
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // The window now covers the whole screen, so outside taps have to be caught here
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onDismiss
                    )
            )

            Column(
                // Roughly the size the platform default dialog width gave before
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = filepath,
                    contentDescription = "Profile picture big",
                    modifier = Modifier
                        .fillMaxWidth()
                        .zoomable(zoomState)
                        // Inside the zoomed layer, so the clip scales with the image. Corner
                        // radius shrinks from circle (50%) to square while zooming in.
                        .graphicsLayer {
                            val morphProgress = ((zoomState.scale - 1f) / (CIRCLE_TO_SQUARE_SCALE - 1f))
                                .coerceIn(0f, 1f)
                            shape = RoundedCornerShape(size.minDimension / 2f * (1f - morphProgress))
                            clip = true
                        }
                )

                // Placed after the image so the buttons stay on top of a zoomed image
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    if(showEditButton){
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

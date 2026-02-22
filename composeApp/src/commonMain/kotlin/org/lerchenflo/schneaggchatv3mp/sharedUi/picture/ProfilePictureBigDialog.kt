package org.lerchenflo.schneaggchatv3mp.sharedUi.picture

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import io.ktor.client.request.invoke

// Zoagt es Profilbild groß a
@Composable
fun ProfilePictureBigDialog(
    onDismiss: () -> Unit,
    filepath: String,
    onEdit: () -> Unit = {},
    showEditButton: Boolean = false
) {
    var containerWidth by remember { mutableStateOf(0f) }
    var containerHeight by remember { mutableStateOf(0f) }


    Dialog(onDismissRequest = onDismiss) {

        var scale by remember { mutableStateOf(1f) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }

        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
            scale = newScale

            if (newScale > 1f) {
                val extraWidth = containerWidth * (newScale - 1f)
                val extraHeight = containerHeight * (newScale - 1f)

                val maxOffsetX = extraWidth / 2f
                val maxOffsetY = extraHeight / 2f

                offsetX = (offsetX + panChange.x).coerceIn(-maxOffsetX, maxOffsetX)
                offsetY = (offsetY + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
            } else {
                offsetX = 0f
                offsetY = 0f
            }
        }


        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp)
                    .clip(CircleShape)
                    .transformable(transformableState)
                    .onSizeChanged {
                        containerWidth = it.width.toFloat()
                        containerHeight = it.height.toFloat()
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = filepath,
                    contentDescription = "Profile picture big",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
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
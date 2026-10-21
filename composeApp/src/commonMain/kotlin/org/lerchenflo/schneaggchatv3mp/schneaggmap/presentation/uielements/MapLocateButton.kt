package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.maplibre.compose.map.MapState
import org.maplibre.compose.overlay.CompassDefaults
import kotlin.math.abs

//Same tolerance the library's DisappearingCompassButton uses before it counts the map as turned.
private const val ROTATION_SLOP_DEGREES = 0.5

/**
 * Combined compass and "my location" button. While the map is rotated or tilted it shows the
 * compass needle (turning with the map) and a tap turns the map back north; otherwise it shows the
 * locate icon and a tap starts following the own location. The locate icon is filled in the
 * primary color while follow mode is active.
 *
 * Hidden while neither applies: map not rotated and no own location available.
 */
@Composable
fun MapLocateButton(
    mapState: MapState,
    isLocationAvailable: Boolean,
    isFollowingLocation: Boolean,
    onLocateClick: () -> Unit,
    onResetRotationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    //derivedStateOf so the button only recomposes when the rotated state flips, not on every
    //camera frame - the needle itself reads the bearing in its graphicsLayer.
    val isRotated by remember(mapState) {
        derivedStateOf {
            val camera = mapState.cameraPosition
            val bearing = ((camera.bearing % 360) + 360) % 360
            val bearingFromNorth = minOf(bearing, 360 - bearing)
            bearingFromNorth > ROTATION_SLOP_DEGREES || abs(camera.pitch) > ROTATION_SLOP_DEGREES
        }
    }

    if (!isRotated && !isLocationAvailable) return

    SmallFloatingActionButton(
        onClick = { if (isRotated) onResetRotationClick() else onLocateClick() },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    ) {
        when {
            isRotated -> Image(
                painter = CompassDefaults.needlePainter(),
                contentDescription = CompassDefaults.contentDescription(),
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        rotationZ = -mapState.cameraPosition.bearing.toFloat()
                        rotationX = mapState.cameraPosition.pitch.toFloat()
                    }
            )

            isFollowingLocation -> Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            else -> Icon(
                imageVector = Icons.Default.LocationSearching,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

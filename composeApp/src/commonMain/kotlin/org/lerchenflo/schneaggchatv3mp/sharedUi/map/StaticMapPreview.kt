package org.lerchenflo.schneaggchatv3mp.sharedUi.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.MaplibreComposable

/**
 * Small map inside a popup or a details screen that shows [focus] with the markers drawn by
 * [markers] (the shared [UserMarkerLayer] / [EventMarkerLayer], same as on the Schneaggmap). One
 * position is centered at [zoom], several are fitted into the view. Gestures are off, so a
 * scrolling parent keeps scrolling; a tap anywhere on it calls [onClick]. Shows nothing for an
 * empty [focus].
 */
@Composable
fun StaticMapPreview(
    focus: List<LatLong>,
    styleUrl: String,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    zoom: Double = 13.0,
    markers: @Composable @MaplibreComposable () -> Unit,
) {
    val center = centerOf(focus) ?: return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // The camera is only read on the first composition, so a new focus needs a new map state
        key(focus, styleUrl) {
            val mapState = rememberMapState(
                baseStyle = BaseStyle.Uri(styleUrl),
                initialCameraPosition = CameraPosition(center = center.toPosition(), zoom = zoom),
                content = markers,
            )
            LaunchedEffect(mapState) { mapState.fitCameraTo(focus, animate = false) }

            // Empty overlay: no compass, scale bar or attribution button, the strip below replaces them
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                state = mapState,
                interactions = MapInteractions.None,
                overlay = {},
            )

            // On top of the native map view, so the tap reaches Compose on every platform
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(role = Role.Button, onClick = onClick)
                    .semantics { this.contentDescription = contentDescription }
            )

            // Legally required copyright notice, same strip as on the Schneaggmap; above the
            // tap layer so its links stay tappable
            MapAttributionBar(
                mapState = mapState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

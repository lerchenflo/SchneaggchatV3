package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.maplibre.compose.map.MapState
import org.maplibre.compose.material3.AttributionLinks
import org.maplibre.compose.overlay.attributions

/**
 * Always visible, half transparent strip with the map style's attributions (legally required
 * OSM/OpenFreeMap copyright notice), replacing the library's collapsible attribution button.
 */
@Composable
fun MapAttributionBar(
    mapState: MapState,
    modifier: Modifier = Modifier,
) {
    val mapStyle = mapState.style
    val attributions by remember(mapStyle) { derivedStateOf { mapStyle.attributions() } }
    if (attributions.isEmpty()) return

    AttributionLinks(
        attributions = attributions,
        textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            //Without this, a drag across the strip pans the map underneath it
            .pointerInput(Unit) {}
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

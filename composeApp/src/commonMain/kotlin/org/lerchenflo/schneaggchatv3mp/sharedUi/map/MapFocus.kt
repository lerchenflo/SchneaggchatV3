package org.lerchenflo.schneaggchatv3mp.sharedUi.map

import androidx.compose.ui.unit.dp
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.maplibre.compose.camera.CameraFit
import org.maplibre.compose.map.MapState
import org.maplibre.compose.util.DpPadding
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position

// Camera helpers shared by the Schneaggmap and the small map previews.

/** Space kept between the outermost markers and the map edge, so a marker is never cut off. */
private val FIT_PADDING = 48.dp

fun LatLong.toPosition(): Position = Position(longitude = long, latitude = lat)

/** Middle of [positions], for a camera that starts there before it fits them. Null when empty. */
fun centerOf(positions: List<LatLong>): LatLong? {
    if (positions.isEmpty()) return null
    return LatLong(lat = positions.map { it.lat }.average(), long = positions.map { it.long }.average())
}

/** True when the camera has to fit an area instead of centering one point. */
fun needsFit(positions: List<LatLong>): Boolean = positions.distinct().size > 1

/**
 * Moves the camera so every one of [positions] is visible with some room around it. Does nothing
 * for fewer than two distinct positions (a single point has no area to fit) or when the map can't
 * calculate the camera.
 */
suspend fun MapState.fitCameraTo(positions: List<LatLong>, animate: Boolean) {
    if (!needsFit(positions)) return
    val bounds = BoundingBox(
        Position(longitude = positions.minOf { it.long }, latitude = positions.minOf { it.lat }),
        Position(longitude = positions.maxOf { it.long }, latitude = positions.maxOf { it.lat }),
    )
    val fit = CameraFit(fitPadding = DpPadding(FIT_PADDING, FIT_PADDING, FIT_PADDING, FIT_PADDING))
    try {
        if (animate) animateCameraToBounds(bounds, fit) else fitCameraToBounds(bounds, fit)
    } catch (_: IllegalStateException) {
        // The backend can't calculate a camera for these bounds: keep the current one
    }
}

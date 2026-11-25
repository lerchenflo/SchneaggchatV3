package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.buildJsonObject
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.SnailTrailPoint
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.safeAdd
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.location.LocationMeasurement
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Position

//Distinct, stable color per user for snail trails - picked by hashing the user id into a
//small curated palette so colors stay visually distinguishable from each other.
private val SNAIL_TRAIL_COLORS = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00),
    Color(0xFF8E24AA), Color(0xFF00ACC1), Color(0xFFD81B60), Color(0xFF6D4C41),
)

private fun snailTrailColor(userId: String): Color =
    SNAIL_TRAIL_COLORS[userId.hashCode().mod(SNAIL_TRAIL_COLORS.size)]

//Extends the recorded trail with the user's live position so the line reaches all the way to
//where they actually are right now, not just to the last synced trail point. Skipped if the
//user is already sitting at that last point (no redundant zero-length segment).
private fun appendLiveEndIfMoved(trailPositions: List<Position>, livePosition: Position?): List<Position> {
    if (livePosition == null) return trailPositions
    return if (trailPositions.isEmpty() || trailPositions.last() != livePosition) {
        trailPositions + listOf(livePosition)
    } else {
        trailPositions
    }
}


/**
 * The Schneaggmap's snail trails: each friend's recorded way in their own color and the own one in
 * the primary color, each extended to the live position. Drawn under the friends (see
 * [org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.MapContentLayers]'s `underPeople`).
 */
@Composable
@MaplibreComposable
fun SnailTrailLayers(
    usersWithLocation: List<User>,
    snailTrails: Map<String, List<SnailTrailPoint>>,
    ownLocation: LocationMeasurement?,
) {
    val ownId = SessionCache.requireLoggedIn()?.userId

    usersWithLocation.forEach { user ->
        key(user.id) {
            val trail = snailTrails[user.id]
            val trailPositions = trail?.map { point ->
                Position(longitude = point.long, latitude = point.lat)
            } ?: emptyList()
            val livePosition = user.location?.let { Position(longitude = it.long, latitude = it.lat) }
            val fullTrailPositions = appendLiveEndIfMoved(trailPositions, livePosition)
            if (fullTrailPositions.size >= 2) {
                safeAdd(layerId = "snailtrail-${user.id}") {
                    val trailSource = rememberGeoJsonSource(
                        data = GeoJsonData.Features(
                            FeatureCollection(
                                Feature(
                                    geometry = LineString(fullTrailPositions),
                                    properties = buildJsonObject {},

                                )
                            )
                        )
                    )

                    LineLayer(
                        id = "snailtrail-${user.id}",
                        source = trailSource,
                        color = const(snailTrailColor(user.id)),
                        width = const(3.dp),
                        cap = const(LineCap.Round),
                        join = const(LineJoin.Round),
                    )
                }
            }
        }
    }

    // Our own snail trail - drawn in the theme's primary color so it stands out from
    // friends' hashed palette colors.
    if (ownId != null) {
        key(ownId) {
            val ownTrail = snailTrails[ownId]
            val ownTrailPositions = ownTrail?.map { point ->
                Position(longitude = point.long, latitude = point.lat)
            } ?: emptyList()
            val ownLivePosition = ownLocation?.position
            val fullOwnTrailPositions = appendLiveEndIfMoved(ownTrailPositions, ownLivePosition)
            if (fullOwnTrailPositions.size >= 2) {
                safeAdd(layerId = "snailtrail-$ownId") {
                    val ownTrailColor = MaterialTheme.colorScheme.primary
                    val ownTrailSource = rememberGeoJsonSource(
                        data = GeoJsonData.Features(
                            FeatureCollection(
                                Feature(
                                    geometry = LineString(fullOwnTrailPositions),
                                    properties = buildJsonObject {},

                                )
                            )
                        )
                    )

                    LineLayer(
                        id = "snailtrail-$ownId",
                        source = ownTrailSource,
                        color = const(ownTrailColor),
                        width = const(3.dp),
                        cap = const(LineCap.Round),
                        join = const(LineJoin.Round),
                    )
                }
            }
        }
    }
}

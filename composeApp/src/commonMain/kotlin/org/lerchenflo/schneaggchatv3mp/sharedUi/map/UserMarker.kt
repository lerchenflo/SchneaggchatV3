package org.lerchenflo.schneaggchatv3mp.sharedUi.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.layers.FeaturesClickHandler
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_online

// The user marker of the Schneaggmap: the profile picture with the name and the online / last seen
// pill under it. Shared, so every map (the Schneaggmap, the small previews) draws users the same.

/** A drawn marker and the size it is shown at. */
data class MarkerIcon(val bitmap: ImageBitmap, val size: DpSize)

/** What the marker pill of one user says. */
data class UserMarkerData(
    val username: String,
    val statusText: String,
    val isOnline: Boolean,
    val speed: Double?,
    val heading: Double?,
)

/** Theme colors and texts of the user marker, read once per composition. */
data class UserMarkerStyle(
    val pillColor: Color,
    val onlineColor: Color,
    val offlineColor: Color,
    val headingColor: Color,
    val onlineLabel: String,
)

@Composable
fun rememberUserMarkerStyle(): UserMarkerStyle = UserMarkerStyle(
    pillColor = MaterialTheme.colorScheme.surface,
    onlineColor = MaterialTheme.colorScheme.primary,
    offlineColor = MaterialTheme.colorScheme.onSurface,
    headingColor = MaterialTheme.colorScheme.surface,
    onlineLabel = stringResource(Res.string.schneaggmap_user_online),
)

/** The pill of [this] user: "online" when connected right now, else the last seen time. */
@Composable
private fun User.toUserMarkerData(isOnline: Boolean, style: UserMarkerStyle): UserMarkerData = UserMarkerData(
    username = displayName,
    statusText = if (isOnline) style.onlineLabel else lastSeen?.let { millisToTimeDateOrYesterday(it) } ?: "-",
    isOnline = isOnline,
    speed = location?.speed,
    heading = location?.heading,
)

/** The decoded profile picture and the marker drawn from it. */
data class LoadedUserMarker(val picture: ImageBitmap, val marker: MarkerIcon)

/**
 * Reads the profile picture at [pictureFilePath] and draws the user marker with [data]. Reads a
 * file, so call it off the composition. Null when there is no picture or it can't be read: the
 * marker layer shows the placeholder then.
 */
private fun loadUserMarker(
    pictureFilePath: String,
    data: UserMarkerData,
    style: UserMarkerStyle,
    textMeasurer: TextMeasurer,
    density: Density,
): LoadedUserMarker? {
    if (pictureFilePath.isBlank()) return null
    return runCatching {
        val bytes = SystemFileSystem.source(Path(pictureFilePath)).buffered().readByteArray()
        val picture = bytes.decodeToImageBitmap()
        val bitmap = mergeProfilePictureWithStatusText(
            profilePicture = picture,
            username = data.username,
            statusText = data.statusText,
            backgroundColor = style.pillColor,
            nameColor = style.offlineColor,
            statusColor = if (data.isOnline) style.onlineColor else style.offlineColor,
            textMeasurer = textMeasurer,
            density = density,
            heading = data.heading,
            speed = data.speed,
            headingIndicatorColor = style.headingColor,
        )
        val size = with(density) { DpSize(bitmap.width.toDp(), bitmap.height.toDp()) }
        LoadedUserMarker(picture = picture, marker = MarkerIcon(bitmap = bitmap, size = size))
    }.getOrNull()
}

/**
 * The markers of [users] by user id, drawn off the composition (file read and bitmap decode) and
 * drawn again only when a picture, name or status changes. A user whose picture can't be loaded
 * is missing from the map, so [UserMarkerLayer] shows the placeholder for them.
 */
@Composable
fun rememberUserMarkers(users: List<User>, isOnline: (User) -> Boolean): Map<String, LoadedUserMarker> {
    val style = rememberUserMarkerStyle()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val markerData = users.associate { user -> user.id to user.toUserMarkerData(isOnline(user), style) }
    val picturePaths = users.map { it.profilePictureUrl }

    var markers by remember { mutableStateOf<Map<String, LoadedUserMarker>>(emptyMap()) }
    LaunchedEffect(picturePaths, markerData, style) {
        markers = users.mapNotNull { user ->
            val data = markerData[user.id] ?: return@mapNotNull null
            loadUserMarker(user.profilePictureUrl, data, style, textMeasurer, density)?.let { user.id to it }
        }.toMap()
    }
    return markers
}

private val PLACEHOLDER_MARKER_SIZE = DpSize(33.dp, 33.dp)

/**
 * One user's marker at [position], with layer and feature id `user-<userId>`. Without a [marker]
 * (no picture loaded) it shows the default user icon. [onClick] gets the clicked feature, whose id
 * is [userId].
 */
@Composable
@MaplibreComposable
fun UserMarkerLayer(
    userId: String,
    position: Position,
    marker: MarkerIcon?,
    onClick: FeaturesClickHandler? = null,
) {
    safeAdd(layerId = "user-$userId") {
        val source = rememberGeoJsonSource(
            data = GeoJsonData.Features(
                FeatureCollection(
                    features = listOf(
                        Feature(
                            geometry = Point(coordinates = position),
                            properties = buildJsonObject {},
                            id = JsonPrimitive(userId),
                        )
                    )
                )
            )
        )
        val painter = marker?.let { BitmapPainter(it.bitmap) } ?: painterResource(Res.drawable.icon_nutzer)

        //Note: rotate this marker using the heading once we have a directional marker design -
        // heading is already stored/synced per friend but unused for rendering.
        SymbolLayer(
            id = "user-$userId",
            source = source,
            onClick = onClick,
            iconImage = image(painter, size = marker?.size ?: PLACEHOLDER_MARKER_SIZE),
            iconAllowOverlap = const(true),
        )
    }
}

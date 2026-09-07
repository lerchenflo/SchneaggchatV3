package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserLocation
import org.lerchenflo.schneaggchatv3mp.events.domain.icon
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.entries
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.drawableRes
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapAction
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapState
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.approximateDistanceMeters
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.metersPerDpAtTarget
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.safeAdd
import org.lerchenflo.schneaggchatv3mp.utilities.battery.BatteryService
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.location.Location
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.StyleState
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.extensions.inDegrees
import org.maplibre.spatialk.units.extensions.inMeters
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.icon_beer
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_online
import kotlin.math.roundToInt
import kotlin.time.Clock

//Radius, in dp, within which nearby friends get merged into one marker - converted to meters via
//the camera's current scale so it stays a constant on-screen size regardless of zoom level.
private const val USER_CLUSTER_RADIUS_DP = 40.0

private data class MarkerIcon(val bitmap: ImageBitmap, val size: DpSize)

private data class UserMarkerData(val username: String, val statusText: String, val isOnline: Boolean, val speed: Double?, val heading: Double?)

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

private data class UserCluster(val users: List<User>, val centroid: Position)

//Stable identity for a cluster (independent of member order) so layers/icons key correctly
//across recompositions and only churn when membership actually changes.
private fun clusterKey(cluster: UserCluster): String =
    cluster.users.map { it.id }.sorted().joinToString("-")

//Greedily grows each cluster from a seed user, re-checking the running centroid against the
//remaining users so chains of nearby users merge together rather than only the seed's neighbors.
private fun clusterUsersByProximity(users: List<User>, radiusMeters: Double): List<UserCluster> {
    val remaining = users.filter { it.location != null }.toMutableList()
    val clusters = mutableListOf<UserCluster>()

    while (remaining.isNotEmpty()) {
        val members = mutableListOf(remaining.removeAt(0))
        var grew = true
        while (grew) {
            grew = false
            val centroidLat = members.map { it.location!!.lat }.average()
            val centroidLong = members.map { it.location!!.long }.average()
            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                val location = candidate.location!!
                if (approximateDistanceMeters(centroidLat, centroidLong, location.lat, location.long) <= radiusMeters) {
                    members.add(candidate)
                    iterator.remove()
                    grew = true
                }
            }
        }
        val centroidLat = members.map { it.location!!.lat }.average()
        val centroidLong = members.map { it.location!!.long }.average()
        clusters.add(UserCluster(users = members, centroid = Position(longitude = centroidLong, latitude = centroidLat)))
    }

    return clusters
}

/**
 * Draws the Schneaggmap tiles, POI markers, friend/cluster markers, event pins, snail trails and
 * own-location puck onto a [MaplibreMap]. Contains no phone-only chrome (FABs, search bar,
 * dropdowns, info cards, dialogs) so it can be shared verbatim between the phone screen
 * ([org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapScreen]) and the Android
 * Auto car surface.
 */
@Composable
fun SchneaggmapLayers(
    state: SchneaggmapState,
    cameraState: CameraState,
    styleState: StyleState,
    ownLocation: Location?,
    onAction: (SchneaggmapAction) -> Unit,
    modifier: Modifier = Modifier,
) {

    val directionHeadingColor = MaterialTheme.colorScheme.surface

    val ownId = SessionCache.requireLoggedIn()?.userId

    //Resolve all icons (Cycle trough the entrys, code is more garbage otherwise (Auto resolves new types)
    val typeIcons: Map<LocationType, DrawableResource> = remember {
        entries.associateWith { type ->
            type.drawableRes()
        }
    }

    //Decoded bitmaps of the same icons, needed to composite a merged marker for entries that
    //match 2+ location types (see multiTypeEntries below). imageResource() caches by resource
    //id internally, so re-calling it here on every recomposition is cheap.
    val typeIconBitmaps: Map<LocationType, ImageBitmap> = entries.associateWith { type ->
        imageResource(type.drawableRes())
    }

    //Resolve user profile pictures off the composition (file read + bitmap decode), keyed by
    //path so a changed profile picture re-resolves but unrelated state changes don't. Each
    //bitmap has the user's last-online status baked in underneath the picture, so the size
    //varies per user (the status pill width depends on the text) and is tracked alongside it.
    var userIcons by remember { mutableStateOf<Map<String, MarkerIcon>>(emptyMap()) }

    //Raw, undecorated profile pictures (no status pill) kept alongside userIcons so merged
    //cluster icons can composite them without re-reading the file from disk.
    var rawAvatarBitmaps by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }

    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val pillColor = MaterialTheme.colorScheme.surface
    val onlineColor = MaterialTheme.colorScheme.primary
    val offlineColor = MaterialTheme.colorScheme.onSurface
    val onlineLabel = stringResource(Res.string.schneaggmap_user_online)

    //Hock (group hangout) cluster marker ingredients: a translucent version of the marker pill
    //background as the "table", a beer for the center/decorations, and a generic person icon for
    //cluster members whose profile picture hasn't loaded (so the ring still shows every member).
    val clusterBackgroundColor = pillColor.copy(alpha = 0.85f)

    //Merged multi-type location marker background - much more transparent than the user cluster
    //one above, since it's just a backdrop for a couple of small icons, not a "table".
    val mergedLocationBackgroundColor = pillColor.copy(alpha = 0.35f)
    val clusterCountBackgroundColor = MaterialTheme.colorScheme.primary
    val clusterCountTextColor = MaterialTheme.colorScheme.onPrimary
    val beerIcon = imageResource(Res.drawable.icon_beer)
    val defaultAvatarBitmap = imageResource(Res.drawable.icon_nutzer)

    val batteryService = koinInject<BatteryService>()
    val ownUserWithLocation = remember(state.ownUser, ownLocation, batteryService) {
        state.ownUser?.copy(
            location = ownLocation?.let { location ->
                UserLocation(
                    lat = location.position.value.latitude,
                    long = location.position.value.longitude,
                    date = Clock.System.now().toEpochMilliseconds(),
                    speed = location.speed?.distancePerSecond?.inMeters,
                    heading = location.course?.value?.let { bearing -> (bearing - Bearing.North).inDegrees },
                    altitude = location.position.value.altitude,
                    batteryLevel = batteryService.getBatteryLevel(),
                )
            }
        )
    }

    val allUsersWithLocation = remember(state.usersWithLocation, ownUserWithLocation) {
        if (ownUserWithLocation != null && ownUserWithLocation.location != null) {
            listOf(ownUserWithLocation) + state.usersWithLocation.filter { it.id != ownUserWithLocation.id }
        } else {
            state.usersWithLocation
        }
    }

    //Live presence: a friend counts as "online" if the server currently has them connected,
    //otherwise we show their last-seen time instead.
    val userMarkerData = allUsersWithLocation.associate { user ->
        val isOwnUser = user.id == ownId
        val isOnline = isOwnUser || user.id in state.onlineFriendIds
        val statusText = if (isOnline) onlineLabel else user.lastSeen?.let { millisToTimeDateOrYesterday(it) } ?: "-"
        val username = user.displayName
        user.id to UserMarkerData(username = username, statusText = statusText, isOnline = isOnline, speed = user.location?.speed, heading = user.location?.heading)
    }

    val userPicturePaths = allUsersWithLocation.map { it.profilePictureUrl }
    LaunchedEffect(userPicturePaths, userMarkerData, pillColor, onlineColor, offlineColor) {
        val rawBitmaps = mutableMapOf<String, ImageBitmap>()
        userIcons = allUsersWithLocation.mapNotNull { user ->
            if (user.profilePictureUrl.isBlank()) return@mapNotNull null
            val markerData = userMarkerData[user.id] ?: return@mapNotNull null
            runCatching {
                val bytes = SystemFileSystem.source(Path(user.profilePictureUrl)).buffered().readByteArray()
                val profilePicture = bytes.decodeToImageBitmap()
                rawBitmaps[user.id] = profilePicture
                val mergedBitmap = mergeProfilePictureWithStatusText(
                    profilePicture = profilePicture,
                    username = markerData.username,
                    statusText = markerData.statusText,
                    backgroundColor = pillColor,
                    nameColor = offlineColor,
                    statusColor = if (markerData.isOnline) onlineColor else offlineColor,
                    textMeasurer = textMeasurer,
                    density = density,
                    heading = markerData.heading,
                    speed = markerData.speed,
                    headingIndicatorColor = directionHeadingColor
                )
                val markerSize = with(density) {
                    DpSize(mergedBitmap.width.toDp(), mergedBitmap.height.toDp())
                }
                user.id to MarkerIcon(bitmap = mergedBitmap, size = markerSize)
            }.getOrNull()
        }.toMap()
        rawAvatarBitmaps = rawBitmaps
    }

    //Merge nearby friends into a single marker once they're closer together on screen than
    //USER_CLUSTER_RADIUS_DP - converted to meters via the camera's current scale, then quantized
    //so a continuous pinch/pan gesture doesn't re-cluster (and churn the GL layers) every frame.
    val rawClusterRadiusMeters = USER_CLUSTER_RADIUS_DP * cameraState.metersPerDpAtTarget
    val clusterRadiusMeters = (rawClusterRadiusMeters / 5.0).roundToInt() * 5.0
    val userClusters = remember(allUsersWithLocation, clusterRadiusMeters, state.mergeUsers) {
        if (state.mergeUsers) {
            clusterUsersByProximity(allUsersWithLocation, clusterRadiusMeters)
        } else {
            allUsersWithLocation.filter { it.location != null }.map { user ->
                UserCluster(
                    users = listOf(user),
                    centroid = Position(longitude = user.location!!.long, latitude = user.location.lat)
                )
            }
        }
    }

    val isOwnUserInHock = remember(userClusters, ownId) {
        ownId != null && userClusters.any { cluster ->
            cluster.users.size >= 2 && cluster.users.any { user -> user.id == ownId }
        }
    }

    val ownLocationDotColor = Color(0xFF4285F4)

//Own-avatar stand-in for cluster icons: a blue dot (matching the standalone "own location"
//marker) instead of the profile picture, so it's still recognizably "you" inside a hock.
    val ownDotAvatarBitmap = remember(defaultAvatarBitmap, ownLocationDotColor) {
        val size = defaultAvatarBitmap.width.coerceAtLeast(defaultAvatarBitmap.height)
        val bitmap = ImageBitmap(size, size)
        val canvas = Canvas(bitmap)
        val center = Offset(size / 2f, size / 2f)
        val radius = size / 2f * 0.85f

        canvas.drawCircle(
            center = center,
            radius = radius,
            paint = Paint().apply { color = ownLocationDotColor }
        )
        canvas.drawCircle(
            center = center,
            radius = radius,
            paint = Paint().apply {
                color = Color.White
                style = PaintingStyle.Stroke
                strokeWidth = size * 0.08f
            }
        )
        bitmap
    }

    val clusterIcons: Map<String, MarkerIcon> = remember(
        userClusters, rawAvatarBitmaps, clusterBackgroundColor, beerIcon, defaultAvatarBitmap, textMeasurer, clusterCountBackgroundColor, clusterCountTextColor
    ) {

        userClusters.filter { it.users.size >= 2 }.associate { cluster ->
            //Always one avatar per member (falling back to the generic icon) so the ring
            //accurately reflects how many people are in the cluster.
            val avatarBitmaps = cluster.users.map { user ->
                if (user.id == ownId) {
                    ownDotAvatarBitmap
                } else {
                    rawAvatarBitmaps[user.id] ?: defaultAvatarBitmap
                }
            }
            val bitmap = mergeClusterAvatarsIcon(
                profilePictures = avatarBitmaps,
                beerIcon = beerIcon,
                backgroundColor = clusterBackgroundColor,
                density = density,
                textMeasurer = textMeasurer,
                countBackgroundColor = clusterCountBackgroundColor,
                countTextColor = clusterCountTextColor,
            )
            val size = with(density) { DpSize(bitmap.width.toDp(), bitmap.height.toDp()) }
            clusterKey(cluster) to MarkerIcon(bitmap = bitmap, size = size)
        }
    }


    MaplibreMap(
        modifier = modifier.fillMaxSize(),
        baseStyle = BaseStyle.Uri(state.mapStyleUrl),
        cameraState = cameraState,
        styleState = styleState,
        //Scale bar, compass and attribution are drawn by SchneaggmapScreen itself, positioned
        //around the rest of the map chrome.
        overlay = MapOverlay.None,
        onMapClick = { position, _ ->
            onAction(SchneaggmapAction.OnMapClick(LatLong(position.latitude, position.longitude), longClick = false))

            ClickResult.Pass
        },
        onMapLongClick = { position, _ ->
            onAction(SchneaggmapAction.OnMapClick(LatLong(position.latitude, position.longitude), longClick = true))

            ClickResult.Consume
        }
    ) {

        //println("MapLocations: ${state.entries}")


        //If no entry is loaded, dont render anything (Crash)
        if (state.entries.isNotEmpty()) {

            val enabledTypeKeysMap = state.enabledTypes

            //Entries that match 2+ currently enabled types get a single merged-icon marker
            //(rendered further below) instead of one full icon per type stacked on the same
            //coordinate, so they're excluded from the per-type layers here.
            val multiTypeEntries = remember(state.entries, enabledTypeKeysMap) {
                state.entries.filter { entry ->
                    entry.locationData
                        .map { it.locationtype }
                        .distinct()
                        .count { it in enabledTypeKeysMap } >= 2
                }
            }
            val multiTypeEntryIds = remember(multiTypeEntries) { multiTypeEntries.map { it.id }.toSet() }

            //Precomputed once per entries/multi-type change instead of re-filtering all entries
            //for every one of the ~30 location types on every recomposition.
            val entriesByType = remember(state.entries, multiTypeEntryIds) {
                entries.associateWith { type ->
                    state.entries.filter { entry ->
                        entry.id !in multiTypeEntryIds &&
                            entry.locationData.any { it.locationtype == type }
                    }
                }
            }

            entries.forEach { type ->

                //Skip if not enabled on map
                if (!enabledTypeKeysMap.contains(type)) return@forEach

                val entriesForType = entriesByType[type].orEmpty()
                if (entriesForType.isEmpty()) return@forEach


                val iconRes = typeIcons[type] ?: return@forEach

                safeAdd(layerId = "type-${type.name}") {
                    val mapLocationSource = rememberGeoJsonSource(
                        data = GeoJsonData.Features(
                            FeatureCollection(
                                features = entriesForType
                                    .map { entry ->
                                    Feature(
                                        geometry = Point(
                                            coordinates = Position(
                                                longitude = entry.coordinates.long,
                                                latitude = entry.coordinates.lat,
                                            )
                                        ),
                                        properties = buildJsonObject {
                                            put("type", JsonPrimitive(type.name))
                                        },
                                        id = JsonPrimitive(entry.id)
                                    )
                                }
                            )
                        ),

                        options = GeoJsonOptions(
                            cluster = state.useClustering,
                            clusterRadius = 12,
                            clusterMinPoints = 6,
                            //synchronousUpdate = true
                        )


                    )

                    SymbolLayer(
                        id = "type-${type.name}",
                        source = mapLocationSource,
                        onClick = { clickedItems ->
                            if (clickedItems.isNotEmpty()) {
                                onAction(SchneaggmapAction.OnEntryClick(clickedItems.first().id!!.content))
                                ClickResult.Consume
                            } else ClickResult.Pass
                        },
                        iconImage = image(painterResource(iconRes), size = DpSize(33.dp, 33.dp)),
                        iconAllowOverlap = const(!state.useClustering)
                    )
                }
            }

            //Multi-type entries: one marker per entry with a composited icon combining every
            //enabled type it belongs to (see mergeLocationTypeIcons), instead of stacking a full
            //icon per type on the same coordinate. Not clustered - these are rare compared to
            //single-type entries, so native per-type clustering above is left untouched.
            val entryMergedIcons: Map<String, MarkerIcon> = remember(
                multiTypeEntries, typeIconBitmaps, mergedLocationBackgroundColor, density
            ) {
                multiTypeEntries.associate { entry ->
                    val icons = entry.locationData
                        .map { it.locationtype }
                        .distinct()
                        .filter { it in enabledTypeKeysMap }
                        .sortedBy { it.ordinal }
                        .mapNotNull { typeIconBitmaps[it] }
                    val bitmap = mergeLocationTypeIcons(
                        icons = icons,
                        backgroundColor = mergedLocationBackgroundColor,
                        density = density,
                    )
                    val size = with(density) { DpSize(bitmap.width.toDp(), bitmap.height.toDp()) }
                    entry.id to MarkerIcon(bitmap = bitmap, size = size)
                }
            }

            multiTypeEntries.forEach { entry ->
                key(entry.id) {
                    safeAdd(layerId = "entry-${entry.id}") {
                        val mapLocationSource = rememberGeoJsonSource(
                            data = GeoJsonData.Features(
                                FeatureCollection(
                                    features = listOf(
                                        Feature(
                                            geometry = Point(
                                                coordinates = Position(
                                                    longitude = entry.coordinates.long,
                                                    latitude = entry.coordinates.lat,
                                                )
                                            ),
                                            properties = buildJsonObject {},
                                            id = JsonPrimitive(entry.id)
                                        )
                                    )
                                )
                            )
                        )

                        val markerIcon = entryMergedIcons[entry.id]
                        if (markerIcon != null) {
                            SymbolLayer(
                                id = "entry-${entry.id}",
                                source = mapLocationSource,
                                onClick = { clickedItems ->
                                    if (clickedItems.isNotEmpty()) {
                                        onAction(SchneaggmapAction.OnEntryClick(clickedItems.first().id!!.content))
                                        ClickResult.Consume
                                    } else ClickResult.Pass
                                },
                                iconImage = image(BitmapPainter(markerIcon.bitmap), size = markerIcon.size),
                                iconAllowOverlap = const(true)
                            )
                        }
                    }
                }
            }
        }


        //Show snail trails (drawn before the user markers so the avatars sit on top of the lines)
        if (state.showSnailTrails) {
            state.usersWithLocation.forEach { user ->
                key(user.id) {
                    val trail = state.snailTrails[user.id]
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
                    val ownTrail = state.snailTrails[ownId]
                    val ownTrailPositions = ownTrail?.map { point ->
                        Position(longitude = point.long, latitude = point.lat)
                    } ?: emptyList()
                    val ownLivePosition = ownLocation?.position?.value
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

        //Show user locations - friends close enough together at the current zoom are merged into
        //a single "stacked avatars" marker (see userClusters) instead of overlapping pins.
        if (allUsersWithLocation.isNotEmpty() && state.showUsers) {
            userClusters.forEach { cluster ->
                if (cluster.users.size == 1) {
                    val user = cluster.users.first()
                    if (user.id != ownId) {
                        key(user.id) {
                            safeAdd(layerId = "user-${user.id}") {
                                val mapLocationSource = rememberGeoJsonSource(
                                    data = GeoJsonData.Features(
                                        FeatureCollection(
                                            features = listOf(Feature(
                                                geometry = Point(
                                                    coordinates = Position(
                                                        longitude = user.location!!.long,
                                                        latitude = user.location.lat,
                                                    )
                                                ),
                                                properties = buildJsonObject {
                                                    put("type", JsonPrimitive(user.name))
                                                },
                                                id = JsonPrimitive(user.id)
                                            ))
                                        )
                                    )
                                )

                                val markerIcon = userIcons[user.id]
                                val profilePicturePainter = markerIcon?.let { BitmapPainter(it.bitmap) }
                                    ?: painterResource(Res.drawable.icon_nutzer)
                                val markerSize = markerIcon?.size ?: DpSize(33.dp, 33.dp)

                                //Note: rotate this marker using user.location?.heading once we have a directional
                                // marker design - heading is already stored/synced per friend but unused for rendering.
                                SymbolLayer(
                                    id = "user-${user.id}",
                                    source = mapLocationSource,
                                    onClick = { clickedItems ->
                                        if (clickedItems.isNotEmpty()) {
                                            onAction(SchneaggmapAction.OnUserClick(clickedItems.first().id!!.content))
                                            ClickResult.Consume
                                        } else ClickResult.Pass
                                    },
                                    iconImage = image(profilePicturePainter, size = markerSize),
                                    iconAllowOverlap = const(true)
                                )
                            }
                        }
                    }
                } else {
                    val clusterId = clusterKey(cluster)
                    key(clusterId) {
                        safeAdd(layerId = "cluster-$clusterId") {
                            val clusterSource = rememberGeoJsonSource(
                                data = GeoJsonData.Features(
                                    FeatureCollection(
                                        features = listOf(Feature(
                                            geometry = Point(coordinates = cluster.centroid),
                                            properties = buildJsonObject {},
                                            id = JsonPrimitive(clusterId)
                                        ))
                                    )
                                )
                            )

                            val clusterIcon = clusterIcons[clusterId]
                            if (clusterIcon != null) {
                                //Tapping a cluster zooms in on it rather than opening a specific
                                //user - once it splits apart, individual pins are clickable as usual.
                                SymbolLayer(
                                    id = "cluster-$clusterId",
                                    source = clusterSource,
                                    onClick = {
                                        scope.launch {
                                            cameraState.animateTo(
                                                cameraState.position.copy(
                                                    target = cluster.centroid,
                                                    zoom = cameraState.position.zoom + 2,
                                                )
                                            )
                                        }
                                        ClickResult.Consume
                                    },
                                    iconImage = image(BitmapPainter(clusterIcon.bitmap), size = clusterIcon.size),
                                    iconAllowOverlap = const(true)
                                )
                            }
                        }
                    }
                }
            }
        }

        //Show events with a location set, gated by the "Events" toggle in the filter dropdown.
        //Each pin uses the same icon as the event's type (see EventType.icon()).
        if (state.eventsWithLocation.isNotEmpty() && state.showEvents) {
            val eventPinTint = MaterialTheme.colorScheme.onError

            state.eventsWithLocation.forEach { event ->
                val location = event.location ?: return@forEach

                safeAdd(layerId = "event-${event.id}") {
                    val eventSource = rememberGeoJsonSource(
                        data = GeoJsonData.Features(
                            FeatureCollection(features = listOf(Feature(
                                geometry = Point(
                                    coordinates = Position(
                                        longitude = location.long,
                                        latitude = location.lat,
                                    )
                                ),
                                properties = buildJsonObject {
                                    put("type", JsonPrimitive("event"))
                                },
                                id = JsonPrimitive(event.id)
                            )))
                        )
                    )

                    val eventPinPainter = rememberVectorPainter(event.type.icon())

                    SymbolLayer(
                        id = "event-${event.id}",
                        source = eventSource,
                        onClick = { clickedItems ->
                            if (clickedItems.isNotEmpty()) {
                                onAction(SchneaggmapAction.OnEventPinClick(clickedItems.first().id!!.content))
                                ClickResult.Consume
                            } else ClickResult.Pass
                        },
                        iconImage = image(eventPinPainter, size = DpSize(33.dp, 33.dp), colorFilter = ColorFilter.tint(eventPinTint)),
                        iconAllowOverlap = const(true)
                    )
                }
            }
        }

        //Own position: a dot, or a heading-rotated arrow while moving (no profile picture needed for yourself).
        //Hidden if the logged in user is currently merged into a group hock.
        ownLocation?.takeIf { !isOwnUserInHock }?.let { location ->
            val ownLocationSource = rememberGeoJsonSource(
                data = GeoJsonData.Features(
                    FeatureCollection(features = listOf(Feature(
                        geometry = Point(coordinates = location.position.value),
                        properties = buildJsonObject {
                            put("type", JsonPrimitive("self"))
                        },
                        id = JsonPrimitive("self")
                    )))
                )
            )

            // Once we're moving fast enough to have a reliable heading, show a rotated arrow
            // instead of a plain dot. Both layers always exist to avoid source remove+re-add
            // when heading appears/disappears (CannotAddSourceException).
            val heading = location.speed
                ?.takeIf { it.distancePerSecond.inMeters > 3 }
                ?.let { location.course?.value }

            val arrowPainter = rememberVectorPainter(Icons.Default.Navigation)
            SymbolLayer(
                id = "own-location-arrow",
                source = ownLocationSource,
                visible = heading != null,
                iconImage = image(
                    arrowPainter,
                    size = DpSize(28.dp, 28.dp),
                    colorFilter = ColorFilter.tint(Color(0xFF4285F4))
                ),
                iconRotate = const(((heading ?: Bearing.North) - Bearing.North).inDegrees.toFloat()),
                iconAllowOverlap = const(true)
            )
            CircleLayer(
                id = "own-location-dot",
                source = ownLocationSource,
                visible = heading == null,
                color = const(Color(0xFF4285F4)),
                radius = const(8.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp)
            )
        }
    }

}

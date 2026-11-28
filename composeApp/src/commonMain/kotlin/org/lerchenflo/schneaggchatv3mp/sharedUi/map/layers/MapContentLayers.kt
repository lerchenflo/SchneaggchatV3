package org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.compose.resources.imageResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserLocation
import org.lerchenflo.schneaggchatv3mp.events.domain.icon
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.entries
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapLayersData
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.drawableRes
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.EventMarkerLayer
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.MarkerIcon
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.UserMarkerLayer
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.addMarkerBackground
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.mergeClusterAvatarsIcon
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.mergeLocationTypeIcons
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.metersPerDpAtTarget
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.rememberUserMarkerStyle
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.rememberUserMarkers
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.safeAdd
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.scaleMarkerIcon
import org.lerchenflo.schneaggchatv3mp.utilities.battery.BatteryService
import org.lerchenflo.schneaggchatv3mp.utilities.geo.approximateDistanceMeters
import org.maplibre.compose.expressions.dsl.condition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FeaturesClickHandler
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.location.LocationMeasurement
import org.maplibre.compose.map.MapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.extensions.inDegrees
import org.maplibre.spatialk.units.extensions.inMeters
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.icon_beer
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import kotlin.math.roundToInt
import kotlin.time.Clock

//Radius, in dp, within which nearby friends get merged into one marker - converted to meters via
//the camera's current scale so it stays a constant on-screen size regardless of zoom level.
private const val USER_CLUSTER_RADIUS_DP = 40.0

//Place (POI) markers are half the size of the 40 dp profile picture in user markers, so friends
//stand out over the places.
private val PLACE_MARKER_SIZE = 28.dp
//Wide enough to hold the corners of a square marker icon, about the size of a merged marker
private val RECENTLY_SEEN_DISC_SIZE = 40.dp
private const val RECENTLY_SEEN_PROPERTY = "recentlySeen"

//Per-type icon size inside a merged multi-type place marker, half of mergeLocationTypeIcons' default.
private val MERGED_PLACE_ICON_SIZE = 15.dp

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

/** Friends with a known location, e.g. the zoom snap targets of a [org.lerchenflo.schneaggchatv3mp.sharedUi.map.BaseMap]. */
fun MapLayersData.friendPositions(): List<Position> = usersWithLocation.mapNotNull { user ->
    user.location?.let { location -> Position(longitude = location.long, latitude = location.lat) }
}

/** Taps on the shared layers, each with the id of the tapped entry, user or event. */
data class MapLayerClicks(
    val onEntryClick: (entryId: String) -> Unit = {},
    val onUserClick: (userId: String) -> Unit = {},
    val onEventClick: (eventId: String) -> Unit = {},
    val onOwnUserClick: () -> Unit = {},
)

/**
 * The layers every map screen shares (Schneaggmap, route planner): POI markers, friend/cluster
 * markers, event pins and the own-location puck, as [mapState]'s declared style content, shown as
 * [display] says. [underPeople] is drawn over the places and under friends, events and the own
 * position (the Schneaggmap's snail trails). Contains no chrome (chips, cards, dialogs).
 */
@Composable
@MaplibreComposable
fun MapContentLayers(
    data: MapLayersData,
    display: MapLayersDisplay,
    mapState: MapState,
    ownLocation: LocationMeasurement?,
    clicks: MapLayerClicks,
    underPeople: @Composable @MaplibreComposable () -> Unit = {},
) {

    val ownId = data.ownUserId

    //Decoded type icons, scaled into the per-type markers and composited into a merged marker for
    //entries that match 2+ location types (see multiTypeEntries below). imageResource() caches by
    //resource id internally, so re-calling it here on every recomposition is cheap.
    val typeIconBitmaps: Map<LocationType, ImageBitmap> = entries.associateWith { type ->
        imageResource(type.drawableRes())
    }

    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val markerStyle = rememberUserMarkerStyle()
    val pillColor = markerStyle.pillColor

    //Hock (group hangout) cluster marker ingredients: a translucent version of the marker pill
    //background as the "table", a beer for the center/decorations, and a generic person icon for
    //cluster members whose profile picture hasn't loaded (so the ring still shows every member).
    val clusterBackgroundColor = pillColor.copy(alpha = 0.85f)

    //Merged multi-type location marker background - much more transparent than the user cluster
    //one above, since it's just a backdrop for a couple of small icons, not a "table".
    val mergedLocationBackgroundColor = pillColor.copy(alpha = 0.35f)
    //Same backdrop for recently seen police and radars, in the green of the default themes
    val recentlySeenBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val clusterCountBackgroundColor = MaterialTheme.colorScheme.primary
    val clusterCountTextColor = MaterialTheme.colorScheme.onPrimary
    val beerIcon = imageResource(Res.drawable.icon_beer)
    val defaultAvatarBitmap = imageResource(Res.drawable.icon_nutzer)

    val batteryService = koinInject<BatteryService>()
    val ownUserWithLocation = remember(data.ownUser, ownLocation, batteryService) {
        data.ownUser?.copy(
            location = ownLocation?.let { location ->
                UserLocation(
                    lat = location.position.latitude,
                    long = location.position.longitude,
                    date = Clock.System.now().toEpochMilliseconds(),
                    speed = location.distancePerSecond?.inMeters,
                    heading = location.course?.let { bearing -> (bearing - Bearing.North).inDegrees },
                    altitude = location.position.altitude,
                    batteryLevel = batteryService.getBatteryLevel(),
                )
            }
        )
    }

    val allUsersWithLocation = remember(data.usersWithLocation, ownUserWithLocation) {
        if (ownUserWithLocation != null && ownUserWithLocation.location != null) {
            listOf(ownUserWithLocation) + data.usersWithLocation.filter { it.id != ownUserWithLocation.id }
        } else {
            data.usersWithLocation
        }
    }

    //Live presence: a friend counts as "online" if the server currently has them connected,
    //otherwise we show their last-seen time instead.
    val userMarkers = rememberUserMarkers(allUsersWithLocation) { user ->
        user.id == ownId || user.id in data.onlineFriendIds
    }
    val userIcons = userMarkers.mapValues { it.value.marker }
    //Raw, undecorated profile pictures (no status pill) so merged cluster icons can composite
    //them without re-reading the file from disk.
    val rawAvatarBitmaps = userMarkers.mapValues { it.value.picture }

    //Merge nearby friends into a single marker once they're closer together on screen than
    //USER_CLUSTER_RADIUS_DP - converted to meters via the camera's current scale, then quantized
    //so a continuous pinch/pan gesture doesn't re-cluster (and churn the GL layers) every frame.
    val rawClusterRadiusMeters = USER_CLUSTER_RADIUS_DP * mapState.metersPerDpAtTarget
    val clusterRadiusMeters = (rawClusterRadiusMeters / 5.0).roundToInt() * 5.0
    val userClusters = remember(allUsersWithLocation, clusterRadiusMeters, data.mergeUsers) {
        if (data.mergeUsers) {
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

    //println("MapLocations: ${data.entries}")


    //If no entry is loaded, dont render anything (Crash)
    if (data.entries.isNotEmpty()) {

        val enabledTypeKeysMap = display.enabledTypes

        //Entries that match 2+ currently enabled types get a single merged-icon marker
        //(rendered further below) instead of one full icon per type stacked on the same
        //coordinate, so they're excluded from the per-type layers here.
        val multiTypeEntries = remember(data.entries, enabledTypeKeysMap) {
            data.entries.filter { entry ->
                entry.locationData
                    .map { it.locationtype }
                    .distinct()
                    .count { it in enabledTypeKeysMap } >= 2
            }
        }
        val multiTypeEntryIds = remember(multiTypeEntries) { multiTypeEntries.map { it.id }.toSet() }

        //Precomputed once per entries/multi-type change instead of re-filtering all entries
        //for every one of the ~30 location types on every recomposition.
        val entriesByType = remember(data.entries, multiTypeEntryIds) {
            entries.associateWith { type ->
                data.entries.filter { entry ->
                    entry.id !in multiTypeEntryIds &&
                        entry.locationData.any { it.locationtype == type }
                }
            }
        }

        entries.forEach { type ->

            val entriesOfType = entriesByType[type].orEmpty()

            //A type that is not enabled on the map still shows its active sightings
            val entriesForType = when {
                enabledTypeKeysMap.contains(type) -> entriesOfType
                display.showActiveSightings -> entriesOfType.filter { type in data.activeSightings[it.id].orEmpty() }
                else -> emptyList()
            }
            if (entriesForType.isEmpty()) return@forEach


            val iconBitmap = typeIconBitmaps[type] ?: return@forEach

            safeAdd(layerId = "type-${type.name}") {
                //A ready bitmap instead of painterResource: maplibre renders a Painter on the
                //RenderThread while the main thread waits, which froze the map for ~1.5 s when all
                //place types were enabled at once
                val markerIcon = remember(iconBitmap, density) {
                    scaleMarkerIcon(iconBitmap, density, PLACE_MARKER_SIZE)
                }

                //Only built when a marker needs it, a second bitmap for every enabled type is wasted work
                val iconImage = if (entriesForType.any { type in data.activeSightings[it.id].orEmpty() }) {
                    val recentlySeenIcon = remember(markerIcon, recentlySeenBackgroundColor, density) {
                        addMarkerBackground(markerIcon, recentlySeenBackgroundColor, density, RECENTLY_SEEN_DISC_SIZE)
                    }
                    //A cluster feature has no such property and keeps the plain icon
                    switch(
                        condition(feature.has(RECENTLY_SEEN_PROPERTY), image(recentlySeenIcon)),
                        fallback = image(markerIcon),
                    )
                } else {
                    image(markerIcon)
                }

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
                                        if (type in data.activeSightings[entry.id].orEmpty()) {
                                            put(RECENTLY_SEEN_PROPERTY, JsonPrimitive(true))
                                        }
                                    },
                                    id = JsonPrimitive(entry.id)
                                )
                            }
                        )
                    ),

                    from = GeoJsonOptions(
                        from = GeoJsonOptions.Standard
                    ) {
                        cluster = data.useClustering
                        clusterRadius = 12
                        clusterMinPoints = 6
                        synchronousTiling = false
                    }


                )

                SymbolLayer(
                    id = "type-${type.name}",
                    source = mapLocationSource,
                    onClick = { clickedItems ->
                        if (clickedItems.isNotEmpty()) {
                            clicks.onEntryClick(clickedItems.first().id!!.content)
                            ClickResult.Consume
                        } else ClickResult.Pass
                    },
                    iconImage = iconImage,
                    iconAllowOverlap = const(!data.useClustering)
                )
            }
        }

        //Multi-type entries: one marker per entry with a composited icon combining every
        //enabled type it belongs to (see mergeLocationTypeIcons), instead of stacking a full
        //icon per type on the same coordinate. Not clustered - these are rare compared to
        //single-type entries, so native per-type clustering above is left untouched.
        val entryMergedIcons: Map<String, MarkerIcon> = remember(
            multiTypeEntries, typeIconBitmaps, mergedLocationBackgroundColor,
            recentlySeenBackgroundColor, data.activeSightings, density
        ) {
            multiTypeEntries.associate { entry ->
                val shownTypes = entry.locationData
                    .map { it.locationtype }
                    .distinct()
                    .filter { it in enabledTypeKeysMap }
                    .sortedBy { it.ordinal }
                val icons = shownTypes.mapNotNull { typeIconBitmaps[it] }
                val recentlySeen = shownTypes.any { it in data.activeSightings[entry.id].orEmpty() }
                val bitmap = mergeLocationTypeIcons(
                    icons = icons,
                    backgroundColor = if (recentlySeen) recentlySeenBackgroundColor else mergedLocationBackgroundColor,
                    density = density,
                    iconSize = MERGED_PLACE_ICON_SIZE,
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
                                    clicks.onEntryClick(clickedItems.first().id!!.content)
                                    ClickResult.Consume
                                } else ClickResult.Pass
                            },
                            //The bitmap itself, not a BitmapPainter, so maplibre skips the RenderThread capture
                            iconImage = image(markerIcon.bitmap),
                            iconAllowOverlap = const(true)
                        )
                    }
                }
            }
        }
    }


    //Drawn before the user markers so the avatars sit on top (snail trails)
    underPeople()

    //Show user locations - friends close enough together at the current zoom are merged into
    //a single "stacked avatars" marker (see userClusters) instead of overlapping pins.
    if (allUsersWithLocation.isNotEmpty() && display.showUsers) {
        userClusters.forEach { cluster ->
            if (cluster.users.size == 1) {
                val user = cluster.users.first()
                if (user.id != ownId) {
                    UserMarkerLayer(
                        userId = user.id,
                        position = Position(longitude = user.location!!.long, latitude = user.location.lat),
                        marker = userIcons[user.id],
                        onClick = { clickedItems ->
                            if (clickedItems.isNotEmpty()) {
                                clicks.onUserClick(clickedItems.first().id!!.content)
                                ClickResult.Consume
                            } else ClickResult.Pass
                        },
                    )
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
                                        mapState.animateCamera(
                                            mapState.cameraPosition.copy(
                                                center = cluster.centroid,
                                                zoom = mapState.cameraPosition.zoom + 2,
                                            ).toCameraUpdate()
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
    //Joined events are drawn in the primary color, all others in the error color.
    if (data.eventsWithLocation.isNotEmpty() && display.showEvents) {
        data.eventsWithLocation.forEach { event ->
            val location = event.location ?: return@forEach
            EventMarkerLayer(
                eventId = event.id,
                position = Position(longitude = location.long, latitude = location.lat),
                icon = event.type.icon(),
                isJoined = event.id in data.joinedEventIds,
                onClick = { clickedItems ->
                    if (clickedItems.isNotEmpty()) {
                        clicks.onEventClick(clickedItems.first().id!!.content)
                        ClickResult.Consume
                    } else ClickResult.Pass
                },
            )
        }
    }

    //Own position: a dot, or a heading-rotated arrow while moving (no profile picture needed for yourself).
    //Hidden if the logged in user is currently merged into a group hock.
    ownLocation?.takeIf { !isOwnUserInHock }?.let { location ->
        val ownLocationSource = rememberGeoJsonSource(
            data = GeoJsonData.Features(
                FeatureCollection(features = listOf(Feature(
                    geometry = Point(coordinates = location.position),
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
        val heading = location.distancePerSecond
            ?.takeIf { it.inMeters > 3 }
            ?.let { location.course }

        //Tapping the own position opens the own user info card
        val onOwnLocationClick: FeaturesClickHandler = { clickedItems ->
            if (clickedItems.isNotEmpty()) {
                clicks.onOwnUserClick()
                ClickResult.Consume
            } else ClickResult.Pass
        }

        val arrowPainter = rememberVectorPainter(Icons.Default.Navigation)
        SymbolLayer(
            id = "own-location-arrow",
            source = ownLocationSource,
            visible = heading != null,
            onClick = onOwnLocationClick,
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
            onClick = onOwnLocationClick,
            color = const(Color(0xFF4285F4)),
            radius = const(8.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(2.dp)
        )
    }
}

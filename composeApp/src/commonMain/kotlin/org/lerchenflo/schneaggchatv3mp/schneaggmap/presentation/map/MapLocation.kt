package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map
/*
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.json.JsonPrimitive
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Point

sealed class MapLocation {
    abstract val id: String
    abstract val coordinate: Coordinate

    data class UserLocation(
        override val id: String,
        override val coordinate: Coordinate,
        val username: String,
        val profilePicture: ImageBitmap?,
        val lastSeen: String
    ) : MapLocation()

    data class PlaceLocation(
        override val id: String,
        override val coordinate: Coordinate,
        val name: String,
        val locationType: LocationType,
        val description: String,
        val rating: Long
    ) : MapLocation()
}

data class Coordinate(
    val latitude: Double,
    val longitude: Double
)

fun MapLocation.toFeature(): Feature<Point, Map<String, JsonPrimitive>> {
    return when (this) {
        is MapLocation.UserLocation -> Feature(
            geometry = Point(coordinate.longitude, coordinate.latitude),
            properties = mapOf(
                "id" to JsonPrimitive(id),
                "type" to kotlinx.serialization.json.JsonPrimitive("user"),
                "username" to kotlinx.serialization.json.JsonPrimitive(username),
                "lastSeen" to kotlinx.serialization.json.JsonPrimitive(lastSeen),
                "hasProfilePic" to kotlinx.serialization.json.JsonPrimitive(profilePicture != null)
            ),
            id = kotlinx.serialization.json.JsonPrimitive(id)
        )

        is MapLocation.PlaceLocation -> Feature(
            geometry = Point(coordinate.longitude, coordinate.latitude),
            properties = mapOf(
                "id" to kotlinx.serialization.json.JsonPrimitive(id),
                "type" to kotlinx.serialization.json.JsonPrimitive("place"),
                "name" to kotlinx.serialization.json.JsonPrimitive(name),
                "placeType" to JsonPrimitive(locationType.name),
                "iconName" to kotlinx.serialization.json.JsonPrimitive(locationType.name)
            ),
            id = kotlinx.serialization.json.JsonPrimitive(id)
        )
    }
}

 */
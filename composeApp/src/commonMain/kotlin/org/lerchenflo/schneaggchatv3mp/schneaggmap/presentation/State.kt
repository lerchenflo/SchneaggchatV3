package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation
/*
import kotlinx.serialization.json.JsonPrimitive
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.MapLocation
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.toFeature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point

data class SchneaggmapState(
    val userLocations: List<MapLocation.UserLocation> = emptyList(),
    val placeLocations: List<MapLocation.PlaceLocation> = emptyList()
){
    fun getUserLocationFeatureCollection(): FeatureCollection<Point, Map<String, JsonPrimitive>> {
        return FeatureCollection(
            features = userLocations.map { it.toFeature() }
        )
    }

    fun getPlaceLocationFeatureCollection(): FeatureCollection<Point, Map<String, JsonPrimitive>> {
        return FeatureCollection(
            features = placeLocations.map { it.toFeature() }
        )
    }
}

sealed interface SchneaggmapAction {
    data object OnBackClicked : SchneaggmapAction
}

 */
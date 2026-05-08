package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.MapLocation

data class SchneaggmapState(
    val placeLocations: List<MapLocation.SimplePlaceLocation> = emptyList(),
    val userLocations: List<MapLocation.UserLocation> = emptyList(),
    val enabledTypes: Set<LocationType> = LocationType.entries.toSet(),
    val isFilterDropdownVisible: Boolean = false,
    val selectedLocation: MapLocation? = null,
)

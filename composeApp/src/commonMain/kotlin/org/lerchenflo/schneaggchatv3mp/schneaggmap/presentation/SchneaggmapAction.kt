package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.MapLocation

sealed interface SchneaggmapAction {
    data object OnBackClicked : SchneaggmapAction
    data object ToggleFilterDropdown : SchneaggmapAction
    data class ToggleLocationType(val type: LocationType) : SchneaggmapAction
    data class SelectLocation(val location: MapLocation?) : SchneaggmapAction
}

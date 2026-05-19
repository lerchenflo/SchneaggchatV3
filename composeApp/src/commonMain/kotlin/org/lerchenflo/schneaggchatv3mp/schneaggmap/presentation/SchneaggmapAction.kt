package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry

sealed interface SchneaggmapAction {
    data object OnBackClicked : SchneaggmapAction
    data object ToggleFilterDropdown : SchneaggmapAction
    data object Refresh : SchneaggmapAction
    data class ToggleMainType(val key: String) : SchneaggmapAction
    data class SelectEntry(val entry: MapEntry?) : SchneaggmapAction
}

package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry

sealed interface SchneaggmapAction {
    data object OnBackClicked : SchneaggmapAction
    data object ToggleFilterDropdown : SchneaggmapAction
    data class ToggleMainType(val key: String) : SchneaggmapAction
    data class SelectEntry(val entry: MapEntry?) : SchneaggmapAction

    data class OnMapClick(val coordinates: LatLong, val longClick: Boolean) : SchneaggmapAction
    data class OnEntryClick(val entryId: String) : SchneaggmapAction

    data object OnEntryPopupDismiss: SchneaggmapAction
    data class OnEntryPopupSave(val newEntry: MapEntry): SchneaggmapAction

}

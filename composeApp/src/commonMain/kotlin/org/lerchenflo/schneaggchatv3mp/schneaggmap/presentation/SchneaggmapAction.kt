package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry

sealed interface SchneaggmapAction {
    data object OnBackClicked : SchneaggmapAction
    data object ToggleFilterDropdown : SchneaggmapAction
    data class ToggleMainType(val key: LocationType) : SchneaggmapAction
    data class SelectEntry(val entry: MapEntry?) : SchneaggmapAction

    data class OnMapClick(val coordinates: LatLong, val longClick: Boolean) : SchneaggmapAction
    data class OnEntryClick(val entryId: String) : SchneaggmapAction
    data class OnUserClick(val userId: String) : SchneaggmapAction
    data class OnOpenChatClick(val user: User) : SchneaggmapAction

    data object OnPopupDismiss: SchneaggmapAction
    data class OnEntryPopupSave(val entry: MapEntry): SchneaggmapAction

    data class OnEntryPopupDelete(val entryId: String): SchneaggmapAction


    data object OnSettingsClick: SchneaggmapAction

}

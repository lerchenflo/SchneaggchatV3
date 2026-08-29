package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationGroup
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry

sealed interface SchneaggmapAction {
    data object OnBackClicked : SchneaggmapAction
    data object ToggleFilterDropdown : SchneaggmapAction

    data object ToggleShowUsers: SchneaggmapAction
    data object ToggleShowEvents: SchneaggmapAction
    data class OnEventPinClick(val eventId: String) : SchneaggmapAction
    data class ToggleMainType(val key: LocationType) : SchneaggmapAction
    data class ToggleGroup(val group: LocationGroup) : SchneaggmapAction
    data class ToggleGroupExpanded(val group: LocationGroup) : SchneaggmapAction
    data class SelectEntry(val entry: MapEntry?) : SchneaggmapAction

    data class OnMapClick(val coordinates: LatLong, val longClick: Boolean) : SchneaggmapAction
    data class OnEntryClick(val entryId: String) : SchneaggmapAction
    data class OnUserClick(val userId: String) : SchneaggmapAction
    data object OnOwnUserClick : SchneaggmapAction
    data class OnOpenChatClick(val user: User) : SchneaggmapAction

    data object OnPopupDismiss: SchneaggmapAction
    data object OnFocusEntryHandled: SchneaggmapAction
    data class OnEntryPopupSave(val entry: MapEntry): SchneaggmapAction

    data class OnEntryPopupDelete(val entryId: String): SchneaggmapAction


    data object OnSettingsClick: SchneaggmapAction

    data object ToggleSnailTrails: SchneaggmapAction

    data object ToggleMapStyleDropdown: SchneaggmapAction
    data class SelectMapStyle(val style: MapStyleSetting): SchneaggmapAction


    data class OnSearchTermChange(val newTerm: String) : SchneaggmapAction

    data class OnSearchResultClick(val entry: MapEntry) : SchneaggmapAction

    /** Confirms the pin in pickLocationMode and hands it back via Route.Events(selectedEvent = ...). */
    data class OnConfirmLocationPick(val coordinates: LatLong) : SchneaggmapAction

    /**
     * Cancels pickLocationMode without delivering a result. Distinct from [OnBackClicked]: a plain
     * back-pop would fall through to the app's fixed home tab once this tab's backstack is down to
     * one entry, not back to the Events tab that opened the picker.
     */
    data object OnCancelLocationPick : SchneaggmapAction

    /** Dismisses the "map entry or event?" choice dialog shown after a long-press, without creating anything. */
    data object OnCreateChoiceDismiss : SchneaggmapAction

    /** User chose "map entry" in the long-press choice dialog - opens the blank MapEntry sheet at that coordinate. */
    data class OnCreateMapEntryChoice(val location: LatLong): SchneaggmapAction

    /** User chose "event" in the long-press choice dialog - navigates to Events with a blank Event pre-filled at that coordinate. */
    data class OnCreateEventChoice(val location: LatLong) : SchneaggmapAction
}

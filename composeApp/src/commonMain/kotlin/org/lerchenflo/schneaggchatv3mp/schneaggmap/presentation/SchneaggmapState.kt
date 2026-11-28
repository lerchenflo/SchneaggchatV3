package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import org.lerchenflo.schneaggchatv3mp.chat.domain.SnailTrailPoint
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityName
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationGroup
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapLayersData
import org.lerchenflo.schneaggchatv3mp.settings.domain.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.MapLayersDisplay


data class SchneaggmapState(
    // Places, friends and events, shared with every map screen
    val layers: MapLayersData = MapLayersData(),
    val layerDisplay: MapLayersDisplay = MapLayersDisplay(),

    // Friends to show first in the friends bar, empty while the setting is off
    val pinnedFriendIds: Set<String> = emptySet(),
    val showSnailTrails: Boolean = false,
    val snailTrails: Map<String, List<SnailTrailPoint>> = emptyMap(),

    val selectedEntry: MapEntry? = null,
    // Name of the user who changed the selected entry last
    val selectedEntryEditorName: UserIdentityName? = null,
    val selectedUser: User? = null,

    // One-shot camera target for opening the map with a specific entry (deep link);
    // cleared again via OnFocusEntryHandled once the camera has moved there.
    val focusEntryTarget: LatLong? = null,
    // One-shot set of positions to fit into view when the map opens (a group's map preview);
    // cleared via OnFocusEntryHandled too.
    val focusAreaTarget: List<LatLong>? = null,

    val locationPermissionGranted: Boolean = false,

    val isLoading: Boolean = false,
    val error: String? = null,

    val mapStyle: MapStyleSetting = MapStyleSetting.LIBERTY,
    val mapStyleUrl: String = MapStyleSetting.LIBERTY.tileUrl,

    //Searching
    val searchTerm: String = "",
    val searchResults: List<MapEntry> = emptyList(),
    val searchSelectedGroups: Set<LocationGroup> = emptySet(),
    val searchSelectedTypes: Set<LocationType> = emptySet(),

    // True when this screen was opened to pick a coordinate for something else (e.g. an event's
    // location) rather than for normal map browsing - swaps the usual chrome for a focused
    // confirm-pin overlay. The result travels back via Route.Events(selectedEvent = ...); see
    // SchneaggmapAction.OnConfirmLocationPick.
    val pickLocationMode: Boolean = false,

    val currentlySelectedLocation: LatLong? = null
) {
    val ownLocationShared: Boolean get() = layers.ownUser?.locationShared ?: false
}

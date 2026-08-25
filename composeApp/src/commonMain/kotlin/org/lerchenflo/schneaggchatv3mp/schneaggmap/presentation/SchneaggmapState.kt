package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import org.lerchenflo.schneaggchatv3mp.chat.domain.SnailTrailPoint
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationGroup
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry


data class SchneaggmapState(
    val entries: List<MapEntry> = emptyList(),

    val usersWithLocation: List<User> = emptyList(),
    val onlineFriendIds: Set<String> = emptySet(),
    val showSnailTrails: Boolean = false,
    val snailTrails: Map<String, List<SnailTrailPoint>> = emptyMap(),

    val enabledTypes: Set<LocationType> = emptySet(),
    val isFilterDropdownVisible: Boolean = false,
    val expandedFilterGroups: Set<LocationGroup> = emptySet(),

    val selectedEntry: MapEntry? = null,
    val selectedUser: User? = null,
    val ownUser: User? = null,

    // One-shot camera target for opening the map with a specific entry (deep link);
    // cleared again via OnFocusEntryHandled once the camera has moved there.
    val focusEntryTarget: LatLong? = null,

    val locationPermissionGranted: Boolean = false,
    val ownLocationShared: Boolean = false,

    val isLoading: Boolean = false,
    val error: String? = null,

    val useClustering: Boolean = false,
    val mergeUsers: Boolean = true,
    val showUsers: Boolean = true,

    val eventsWithLocation: List<Event> = emptyList(),
    val showEvents: Boolean = true,

    val mapStyle: MapStyleSetting = MapStyleSetting.LIBERTY,
    val mapStyleUrl: String = MapStyleSetting.LIBERTY.tileUrl,
    val isMapStyleDropdownVisible: Boolean = false,

    //Searching
    val searchTerm: String = "",
    val searchResults: List<MapEntry> = emptyList(),

    // True when this screen was opened to pick a coordinate for something else (e.g. an event's
    // location) rather than for normal map browsing - swaps the usual chrome for a focused
    // confirm-pin overlay. The result travels back via Route.Events(pickedLocation = ...); see
    // SchneaggmapAction.OnConfirmLocationPick.
    val pickLocationMode: Boolean = false,
)

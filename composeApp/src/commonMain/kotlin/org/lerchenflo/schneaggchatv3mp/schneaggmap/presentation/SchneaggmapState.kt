package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import org.lerchenflo.schneaggchatv3mp.chat.domain.SnailTrailPoint
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
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

    val selectedEntry: MapEntry? = null,
    val selectedUser: User? = null,

    val locationPermissionGranted: Boolean = false,
    val ownLocationShared: Boolean = false,

    val isLoading: Boolean = false,
    val error: String? = null,

    val useClustering: Boolean = false,

    val mapStyleUrl: String = MapStyleSetting.LIBERTY.tileUrl
)

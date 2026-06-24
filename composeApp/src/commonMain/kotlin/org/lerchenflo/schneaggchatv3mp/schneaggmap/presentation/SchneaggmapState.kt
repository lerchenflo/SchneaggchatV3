package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry


data class SchneaggmapState(
    val entries: List<MapEntry> = emptyList(),

    val usersWithLocation: List<User> = emptyList(),

    val enabledTypes: Set<LocationType> = emptySet(),
    val isFilterDropdownVisible: Boolean = false,

    val selectedEntry: MapEntry? = null,
    val selectedUser: User? = null,

    val locationPermissionGranted: Boolean = false,
    val ownLocationShared: Boolean = false,

    val isLoading: Boolean = false,
    val error: String? = null,

    val useClustering: Boolean = false
)

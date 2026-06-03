package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry


data class SchneaggmapState(
    val entries: List<MapEntry> = emptyList(),



    val enabledTypes: Set<String> = emptySet(),
    val isFilterDropdownVisible: Boolean = false,
    val selectedEntry: MapEntry? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

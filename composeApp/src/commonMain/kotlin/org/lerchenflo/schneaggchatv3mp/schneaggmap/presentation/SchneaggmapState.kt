package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MainType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.Subtype

data class SchneaggmapState(
    val entries: List<MapEntry> = emptyList(),
    val subtypesByMainType: Map<String, List<Subtype>> = emptyMap(),
    val mainTypes: List<MainType> = emptyList(),
    val enabledMainTypes: Set<String> = emptySet(),
    val isFilterDropdownVisible: Boolean = false,
    val selectedEntry: MapEntry? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

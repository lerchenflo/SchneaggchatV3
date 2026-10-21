package org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote.dto

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.MapEntryResponse

// ─── MapEntry ─────────────────────────────────────────────────────────────────

fun MapEntryResponse.toMapEntry(): MapEntry = MapEntry(
    id = id,
    coordinates = coordinates,
    name = name,
    description = description,
    locationData = locationData,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedBy = updatedBy,
    updatedAt = updatedAt,
    updatedByName = updatedByName,
)

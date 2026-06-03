package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.dtos.MapEntryDto
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry

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
)

fun MapEntry.toDto(): MapEntryDto = MapEntryDto(
    id = id,
    coordinates = coordinates,
    name = name,
    description = description,
    locationData = locationData,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedBy = updatedBy,
    updatedAt = updatedAt,
)

fun MapEntryDto.toMapEntry(): MapEntry = MapEntry(
    id = id,
    coordinates = coordinates,
    name = name,
    description = description,
    locationData = locationData,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedBy = updatedBy,
    updatedAt = updatedAt,
)

fun MapEntryResponse.toDto(): MapEntryDto = toMapEntry().toDto()
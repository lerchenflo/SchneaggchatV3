package org.lerchenflo.schneaggchatv3mp.schneaggmap.data.local.entity

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry

fun MapEntry.toEntity(): MapEntryEntity = MapEntryEntity(
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

fun MapEntryEntity.toMapEntry(): MapEntry = MapEntry(
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

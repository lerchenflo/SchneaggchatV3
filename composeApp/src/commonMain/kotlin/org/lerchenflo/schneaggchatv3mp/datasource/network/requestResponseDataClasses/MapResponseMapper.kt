package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import org.lerchenflo.schneaggchatv3mp.datasource.network.AppJson
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.dtos.MainTypeDto
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.dtos.MapEntryDto
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.dtos.SubtypeDto
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MainType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.Subtype

// ─── MapEntry ─────────────────────────────────────────────────────────────────

fun MapEntryResponse.toMapEntry(): MapEntry = MapEntry(
    id = id,
    mainTypeKey = mainTypeKey,
    subtypeIds = subtypeIds,
    lat = coordinates.lat,
    lon = coordinates.long,
    description = description,
    attributes = attributes,
    createdBy = createdBy,
    createdAt = createdAt,
    lastChangedBy = lastChangedBy,
    updatedAt = lastChangedAt.toString(),
    deleted = deleted,
)

fun MapEntry.toDto(): MapEntryDto = MapEntryDto(
    id = id,
    mainTypeKey = mainTypeKey,
    subtypeIds = AppJson.instance.encodeToString(subtypeIds),
    lat = lat,
    lon = lon,
    description = description,
    attributes = AppJson.instance.encodeToString(attributes),
    createdBy = createdBy,
    createdAt = createdAt,
    lastChangedBy = lastChangedBy,
    updatedAt = updatedAt,
    deleted = deleted,
)

fun MapEntryDto.toMapEntry(): MapEntry = MapEntry(
    id = id,
    mainTypeKey = mainTypeKey,
    subtypeIds = AppJson.instance.decodeFromString(subtypeIds),
    lat = lat,
    lon = lon,
    description = description,
    attributes = AppJson.instance.decodeFromString(attributes),
    createdBy = createdBy,
    createdAt = createdAt,
    lastChangedBy = lastChangedBy,
    updatedAt = updatedAt,
    deleted = deleted,
)

fun MapEntryResponse.toDto(): MapEntryDto = toMapEntry().toDto()

// ─── Subtype ──────────────────────────────────────────────────────────────────

fun SubtypeResponse.toSubtype(): Subtype = Subtype(
    id = id,
    mainTypeKey = mainTypeKey,
    name = name,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = lastChangedAt.toString(),
    deleted = deleted,
)

fun Subtype.toDto(): SubtypeDto = SubtypeDto(
    id = id,
    mainTypeKey = mainTypeKey,
    name = name,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deleted = deleted,
)

fun SubtypeDto.toSubtype(): Subtype = Subtype(
    id = id,
    mainTypeKey = mainTypeKey,
    name = name,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deleted = deleted,
)

// ─── MainType ─────────────────────────────────────────────────────────────────

fun MainTypeResponse.toMainType(): MainType = MainType(
    key = key,
    displayName = displayName,
    attributeDefinitions = attributeDefinitions,
    conditionalRules = conditionalRules,
)

fun MainType.toDto(): MainTypeDto = MainTypeDto(
    key = key,
    displayName = displayName,
    attributeDefinitions = AppJson.instance.encodeToString(attributeDefinitions),
    conditionalRules = AppJson.instance.encodeToString(conditionalRules),
)

fun MainTypeDto.toMainType(): MainType = MainType(
    key = key,
    displayName = displayName,
    attributeDefinitions = AppJson.instance.decodeFromString(attributeDefinitions),
    conditionalRules = AppJson.instance.decodeFromString(conditionalRules),
)

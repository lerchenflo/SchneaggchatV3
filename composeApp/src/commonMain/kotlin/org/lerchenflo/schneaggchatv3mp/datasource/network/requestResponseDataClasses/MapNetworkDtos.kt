package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeDefinition
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.ConditionalRule

@Serializable
data class MapEntryResponse(
    val id: String,
    val mainTypeKey: String,
    val subtypeIds: List<String>,
    val coordinates: LatLongResponse,
    val description: String,
    val attributes: Map<String, AttributeValue>,
    val createdBy: String,
    val createdAt: Long,
    val lastChangedBy: String,
    val lastChangedAt: Long,
    val deleted: Boolean,
)

@Serializable
data class LatLongResponse(val lat: Double, val long: Double)

@Serializable
data class SubtypeResponse(
    val id: String,
    val mainTypeKey: String,
    val name: String,
    val createdBy: String,
    val createdAt: Long,
    val lastChangedAt: Long,
    val deleted: Boolean,
)

@Serializable
data class MainTypeResponse(
    val key: String,
    val displayName: String,
    val attributeDefinitions: List<AttributeDefinition>,
    val conditionalRules: List<ConditionalRule>,
)

@Serializable
data class MapSyncResponse(
    val updatedEntries: List<MapEntryResponse>,
    val deletedEntries: List<String>,
    val moreEntries: Boolean,
)

@Serializable
data class SubtypeSyncResponse(
    val updatedSubtypes: List<SubtypeResponse>,
    val deletedSubtypeIds: List<String>,
    val moreSubtypes: Boolean,
)

@Serializable
data class MapEntryCreateRequest(
    val mainTypeKey: String,
    val subtypeIds: List<String>,
    val coordinates: LatLongResponse,
    val description: String,
    val attributes: Map<String, AttributeValue>,
)

@Serializable
data class MapEntryEditRequest(
    val entryId: String,
    val subtypeIds: List<String>,
    val coordinates: LatLongResponse,
    val description: String,
    val attributes: Map<String, AttributeValue>,
)

@Serializable
data class SubtypeCreateNetworkRequest(
    val mainTypeKey: String,
    val name: String,
)

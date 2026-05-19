package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

data class MapEntry(
    val id: String,
    val mainTypeKey: String,
    val subtypeIds: List<String>,
    val lat: Double,
    val lon: Double,
    val description: String,
    val attributes: Map<String, AttributeValue>,
    val createdBy: String,
    val createdAt: Long,
    val lastChangedBy: String,
    val updatedAt: String,
    val deleted: Boolean,
)

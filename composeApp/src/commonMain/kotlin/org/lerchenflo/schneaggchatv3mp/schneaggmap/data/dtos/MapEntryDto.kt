package org.lerchenflo.schneaggchatv3mp.schneaggmap.data.dtos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "map_entries")
data class MapEntryDto(
    @PrimaryKey val id: String,
    val mainTypeKey: String,
    val subtypeIds: String,   // JSON: List<String>
    val lat: Double,
    val lon: Double,
    val description: String,
    val attributes: String,   // JSON: Map<String, AttributeValue>
    val createdBy: String,
    val createdAt: Long,
    val lastChangedBy: String,
    val updatedAt: String,    // lastChangedAt epoch ms as String
    val deleted: Boolean,
)

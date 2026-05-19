package org.lerchenflo.schneaggchatv3mp.schneaggmap.data.dtos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "map_subtypes")
data class SubtypeDto(
    @PrimaryKey val id: String,
    val mainTypeKey: String,
    val name: String,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: String,  // lastChangedAt epoch ms as String
    val deleted: Boolean,
)

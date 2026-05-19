package org.lerchenflo.schneaggchatv3mp.schneaggmap.data.dtos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "map_main_types")
data class MainTypeDto(
    @PrimaryKey val key: String,
    val displayName: String,
    val attributeDefinitions: String,  // JSON: List<AttributeDefinition>
    val conditionalRules: String,      // JSON: List<ConditionalRule>
)

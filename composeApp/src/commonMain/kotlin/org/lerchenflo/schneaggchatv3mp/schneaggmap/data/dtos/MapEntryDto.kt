package org.lerchenflo.schneaggchatv3mp.schneaggmap.data.dtos

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData

@Entity(tableName = "map_entries")
data class MapEntryDto(
    @PrimaryKey val id: String,

    val coordinates: LatLong,
    val name: String,
    val description: String,

    val locationData: List<LocationData>,

    val createdBy: String,
    val createdAt: Long,

    val updatedBy: String,
    val updatedAt: Long
)

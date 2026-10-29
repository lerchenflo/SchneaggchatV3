package org.lerchenflo.schneaggchatv3mp.datasource.network.dto

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData

@Serializable
data class MapEntryResponse(
    val id: String,

    val coordinates: LatLong,
    val name: String,
    val description: String,

    val locationData: List<LocationData>,

    val createdBy: String,
    val createdAt: Long,

    val updatedBy: String,
    val updatedAt: Long,

    //Resolved server side. Defaulted so entries from an older server still deserialize
    val updatedByName: String = "",
)

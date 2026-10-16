package org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote.dto

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.MapEntryResponse

@Serializable
data class MapSyncResponse(
    val updatedEntries: List<MapEntryResponse>,
    val deletedEntries: List<String>,
    val moreEntries: Boolean,
)

@Serializable
data class MapEntryRequest(
    val entryId: String?,
    val name: String,
    val description: String,
    val coordinates: LatLong,
    val locationData: List<LocationData>,
)

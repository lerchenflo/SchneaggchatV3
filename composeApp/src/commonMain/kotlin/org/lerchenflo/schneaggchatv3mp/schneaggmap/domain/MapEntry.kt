package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

data class MapEntry(
    val id: String,

    val coordinates: LatLong,
    val name: String,
    val description: String,

    val locationData: List<LocationData>,

    val createdBy: String,
    val createdAt: Long,

    val updatedBy: String,
    val updatedAt: Long,

    /** Username of [updatedBy], resolved server side. Empty when the server did not send one. */
    val updatedByName: String = "",
)
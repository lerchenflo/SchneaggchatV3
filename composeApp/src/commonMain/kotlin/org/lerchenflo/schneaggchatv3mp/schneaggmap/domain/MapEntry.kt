package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

data class MapEntry(
    val id: String,

    val coordinates: LatLong,
    val name: String,
    val description: String,

    val locationData: LocationData,

    val createdBy: String,
    val createdAt: Long,

    val updatedBy: String,
    val updatedAt: Long
)

enum class MapCategory { STREET, ACTIVITY, FOOD, CAMPING }

fun MapEntry.category(): MapCategory = when (locationData) {
    is LocationData.Radar,
    is LocationData.Street        -> MapCategory.STREET
    is LocationData.SightSeeing,
    is LocationData.SwimmingLocation,
    is LocationData.PartyLocation -> MapCategory.ACTIVITY
    is LocationData.Food          -> MapCategory.FOOD
    is LocationData.Camping       -> MapCategory.CAMPING
}
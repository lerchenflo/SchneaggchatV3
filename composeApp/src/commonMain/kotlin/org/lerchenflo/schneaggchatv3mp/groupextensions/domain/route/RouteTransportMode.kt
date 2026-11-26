package org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route

/**
 * How the group travels a route; sets the average speed of the rough travel time. Stored by its
 * [name] in `ExtensionSettings.Route.transportMode`, so never rename an entry.
 */
enum class RouteTransportMode(val averageSpeedKmh: Double) {
    FOOT(averageSpeedKmh = 4.5),
    BICYCLE(averageSpeedKmh = 15.0),
    MOTORBIKE(averageSpeedKmh = 50.0),
    CAR(averageSpeedKmh = 60.0);

    companion object {
        val DEFAULT = CAR

        /** The stored value; null or a mode this app version doesn't know falls back to [DEFAULT]. */
        fun fromStored(value: String?): RouteTransportMode =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}

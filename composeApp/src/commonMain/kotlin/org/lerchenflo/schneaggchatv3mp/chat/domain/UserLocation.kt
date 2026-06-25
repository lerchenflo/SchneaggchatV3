package org.lerchenflo.schneaggchatv3mp.chat.domain

/**
 * A user's last known location (domain-only; the Room entity keeps separate columns).
 *
 * [altitude]/[batteryLevel]/[distanceTraveled24h] are populated whenever the location-owner
 * shares their location with us at all. [speed]/[heading] are only populated when they also
 * have "Advanced location sharing" enabled towards us.
 */
data class UserLocation(
    val lat: Double,
    val long: Double,
    val date: Long,
    val speed: Double? = null, // meters/second
    val heading: Double? = null, // degrees, 0-360
    val altitude: Double? = null, // meters above sea level
    val batteryLevel: Int? = null, // percent, 0-100
    val distanceTraveled24h: Double? = null, // meters
)

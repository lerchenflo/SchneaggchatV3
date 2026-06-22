package org.lerchenflo.schneaggchatv3mp.chat.domain

/** A user's last known location (domain-only; the Room entity keeps separate columns). */
data class UserLocation(
    val lat: Double,
    val long: Double,
    val date: Long,
)

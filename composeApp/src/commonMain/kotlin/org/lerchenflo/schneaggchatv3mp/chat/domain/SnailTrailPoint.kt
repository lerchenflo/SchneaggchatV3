package org.lerchenflo.schneaggchatv3mp.chat.domain

/** One sampled point of a friend's snail trail, as shared with us via `FriendLocationPayload`. */
data class SnailTrailPoint(
    val lat: Double,
    val long: Double,
    val locationTime: Long,
    val speed: Double? = null,
    val heading: Double? = null,
)

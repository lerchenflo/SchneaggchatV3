package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.friendcompass

import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong

data class FriendCompassState(
    /** Friends with a currently valid location, own user excluded. */
    val friends: List<User> = emptyList(),

    /** Live GPS position of this device, null until the first fix arrives. */
    val ownLocation: LatLong? = null,

    /**
     * Device azimuth in degrees clockwise from north (0-360), from
     * [org.lerchenflo.schneaggchatv3mp.utilities.location.LocationService]'s orientation sensor.
     * Null when the platform has no orientation sensor (Desktop) - the compass is then rendered
     * north-up.
     */
    val azimuthDegrees: Float? = null,

    /** The friend this compass was opened for; highlighted and shown as the main target. */
    val targetUserId: String? = null,
)

package org.lerchenflo.schneaggchatv3mp.utilities.location

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong

/**
 * A single GPS fix from [LocationService.getLocationFlow], with metadata beyond plain coordinates.
 *
 * @property altitude meters above sea level, or null if the platform didn't provide one for this fix.
 * @property heading direction of travel in degrees clockwise from true north (0-360), or null if
 *   unavailable (e.g. the device isn't moving fast enough to determine a heading).
 * @property timestamp epoch millis when this fix was acquired by the platform.
 */
data class DeviceLocation(
    val coordinates: LatLong,
    val altitude: Int?,
    val heading: Int?,
    val timestamp: Long,
)

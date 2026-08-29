package org.lerchenflo.schneaggchatv3mp.utilities.compass

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic compass (device orientation) provider.
 * Emits the direction the top of the device is pointing at, independent of movement —
 * unlike [org.lerchenflo.schneaggchatv3mp.utilities.location.DeviceLocation.heading],
 * which is the GPS direction of travel and useless while standing still.
 */
expect class CompassService {
    /**
     * A cold Flow that emits the device azimuth in degrees clockwise from north (0-360)
     * whenever the device rotates. Emits null on platforms without an orientation sensor
     * (Desktop), so collectors can fall back to a north-up display.
     */
    fun getAzimuthFlow(): Flow<Float?>
}

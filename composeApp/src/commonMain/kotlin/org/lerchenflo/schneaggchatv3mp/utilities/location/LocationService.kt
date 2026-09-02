package org.lerchenflo.schneaggchatv3mp.utilities.location

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState

/**
 * Platform-agnostic location provider.
 * Emits the current [DeviceLocation] whenever the device position changes.
 * Emits null on platforms that have no GPS (Desktop).
 */
expect class LocationService {
    /**
     * A cold Flow that emits location updates.
     * Collect this to receive position changes. The flow completes when
     * the collecting coroutine is cancelled.
     *
     * @param fastUpdates when true, requests the highest accuracy and emits on every location
     *   change instead of applying a distance filter. Needed by callers like the friend compass
     *   that recompute a bearing on every tiny movement - the default filtered updates are too
     *   coarse for that and make the compass feel broken.
     */
    fun getLocationFlow(fastUpdates: Boolean = false): Flow<DeviceLocation?>

    /**
     * A cold Flow that emits the device azimuth in degrees clockwise from north (0-360),
     * from the device's orientation sensor - independent of movement, unlike
     * [DeviceLocation.heading] which is the GPS direction of travel and useless while standing
     * still. Emits null on platforms without an orientation sensor (Desktop).
     */
    fun getHeadingFlow(): Flow<Float?>

    /**
     * Checks whether the app already has location permission.
     */
    suspend fun checkLocationPermission(): PermissionState

    /**
     * Requests location permission if it is not already granted.
     * Returns the resulting [PermissionState].
     */
    suspend fun requestLocationPermission(): PermissionState
}

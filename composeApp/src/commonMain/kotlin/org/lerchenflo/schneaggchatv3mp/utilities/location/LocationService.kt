package org.lerchenflo.schneaggchatv3mp.utilities.location

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState

/**
 * Platform-agnostic location provider.
 * Emits the current LatLong whenever the device position changes.
 * Emits null on platforms that have no GPS (Desktop).
 */
expect class LocationService {
    /**
     * A cold Flow that emits location updates.
     * Collect this to receive position changes. The flow completes when
     * the collecting coroutine is cancelled.
     */
    fun getLocationFlow(): Flow<LatLong?>

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

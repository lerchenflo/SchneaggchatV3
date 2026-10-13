package org.lerchenflo.schneaggchatv3mp.utilities.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState

// Desktop has no GPS — all methods are no-ops
class DesktopLocationService : LocationService {

    override fun getLocationFlow(fastUpdates: Boolean): Flow<DeviceLocation?> = flowOf(null)

    /** Desktop has no orientation sensor - emit null so the UI falls back to north-up. */
    override fun getHeadingFlow(): Flow<Float?> = flowOf(null)

    override suspend fun checkLocationPermission(): PermissionState = PermissionState.NOT_DETERMINED

    override suspend fun requestLocationPermission(): PermissionState = PermissionState.NOT_DETERMINED
}

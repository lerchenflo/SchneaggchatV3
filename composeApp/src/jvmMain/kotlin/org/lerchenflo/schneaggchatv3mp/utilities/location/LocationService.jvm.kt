package org.lerchenflo.schneaggchatv3mp.utilities.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState

// Desktop has no GPS — all methods are no-ops
actual class LocationService {

    actual fun getLocationFlow(): Flow<LatLong?> = flowOf(null)

    actual suspend fun checkLocationPermission(): PermissionState = PermissionState.NOT_DETERMINED

    actual suspend fun requestLocationPermission(): PermissionState = PermissionState.NOT_DETERMINED
}

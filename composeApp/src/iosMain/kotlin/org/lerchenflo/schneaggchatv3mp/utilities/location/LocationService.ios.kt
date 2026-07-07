package org.lerchenflo.schneaggchatv3mp.utilities.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyNearestTenMeters
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.math.roundToInt

@OptIn(ExperimentalForeignApi::class)
actual class LocationService {

    actual fun getLocationFlow(): Flow<DeviceLocation?> = callbackFlow {
        val manager = CLLocationManager()

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(
                manager: CLLocationManager,
                didUpdateLocations: List<*>
            ) {
                val loc = didUpdateLocations.lastOrNull() as? CLLocation ?: return
                trySend(loc.toDeviceLocation())
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                // Non-fatal – keep the flow alive, just skip this update
                println("LocationService iOS: ${didFailWithError.localizedDescription}")
            }
        }

        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
        manager.distanceFilter = 5.0
        manager.startUpdatingLocation()

        // Emit cached location immediately so the flow isn't empty at start
        manager.location?.let { loc ->
            trySend(loc.toDeviceLocation())
        }

        awaitClose {
            manager.stopUpdatingLocation()
            manager.delegate = null
        }
    }

    actual suspend fun checkLocationPermission(): PermissionState =
        clStatusToState(CLLocationManager.authorizationStatus())

    actual suspend fun requestLocationPermission(): PermissionState {
        val current = checkLocationPermission()
        if (current == PermissionState.GRANTED) return current

        return suspendCancellableCoroutine { continuation ->
            val manager = CLLocationManager()

            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                    val state = clStatusToState(manager.authorizationStatus())
                    if (state != PermissionState.NOT_DETERMINED) {
                        continuation.resume(state)
                    }
                }
            }

            manager.delegate = delegate
            manager.requestWhenInUseAuthorization()
        }
    }

    private fun clStatusToState(status: CLAuthorizationStatus): PermissionState = when (status) {
        kCLAuthorizationStatusAuthorizedWhenInUse,
        kCLAuthorizationStatusAuthorizedAlways -> PermissionState.GRANTED
        kCLAuthorizationStatusDenied,
        kCLAuthorizationStatusRestricted -> PermissionState.DENIED
        kCLAuthorizationStatusNotDetermined -> PermissionState.NOT_DETERMINED
        else -> PermissionState.NOT_DETERMINED
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CLLocation.toDeviceLocation(): DeviceLocation = DeviceLocation(
    coordinates = LatLong(
        lat = coordinate.useContents { latitude },
        long = coordinate.useContents { longitude }
    ),
    // verticalAccuracy is negative when altitude couldn't be determined
    altitude = altitude.takeIf { verticalAccuracy >= 0.0 }?.roundToInt(),
    // course is negative when the heading couldn't be determined
    heading = course.takeIf { it >= 0.0 }?.roundToInt(),
    // speed is negative when it couldn't be determined
    speed = speed.takeIf { it >= 0.0 },
    timestamp = (timestamp.timeIntervalSince1970 * 1000).toLong(),
)

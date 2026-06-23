package org.lerchenflo.schneaggchatv3mp.utilities.location

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
import platform.CoreLocation.kCLDistanceFilterNone
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

actual class LocationService {

    actual fun getLocationFlow(): Flow<LatLong?> = callbackFlow {
        val manager = CLLocationManager()

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(
                manager: CLLocationManager,
                didUpdateLocations: List<*>
            ) {
                val loc = didUpdateLocations.lastOrNull() as? CLLocation ?: return
                trySend(
                    LatLong(
                        lat = loc.coordinate.useContents { latitude },
                        long = loc.coordinate.useContents { longitude }
                    )
                )
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                // Non-fatal – keep the flow alive, just skip this update
                println("LocationService iOS: ${didFailWithError.localizedDescription}")
            }
        }

        manager.delegate = delegate
        manager.desiredAccuracy = 100.0         // ~balanced accuracy (kCLLocationAccuracyHundredMeters)
        manager.distanceFilter = 20.0           // only emit when moved > 20 m
        manager.startUpdatingLocation()

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

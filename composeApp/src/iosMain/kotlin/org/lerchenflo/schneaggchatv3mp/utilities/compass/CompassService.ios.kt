package org.lerchenflo.schneaggchatv3mp.utilities.compass

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.darwin.NSObject

actual class CompassService {

    actual fun getAzimuthFlow(): Flow<Float?> = callbackFlow {
        if (!CLLocationManager.headingAvailable()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val manager = CLLocationManager()

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
                // trueHeading is negative when it can't be determined (no GPS fix for
                // declination) - fall back to the magnetic heading in that case.
                val heading = didUpdateHeading.trueHeading.takeIf { it >= 0.0 }
                    ?: didUpdateHeading.magneticHeading.takeIf { it >= 0.0 }
                heading?.let { trySend(((it.toFloat()) % 360f + 360f) % 360f) }
            }
        }

        manager.delegate = delegate
        manager.headingFilter = 2.0
        manager.startUpdatingHeading()

        awaitClose {
            manager.stopUpdatingHeading()
            manager.delegate = null
        }
    }
}

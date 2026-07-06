package org.lerchenflo.schneaggchatv3mp.utilities.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import kotlin.math.roundToInt

actual class LocationService(private val context: Context) {

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    actual fun getLocationFlow(): Flow<DeviceLocation?> {
        val permManager = PermissionManager(context)

        // Build a callbackFlow so we can use the FusedLocation callback API
        return callbackFlow {
            // Quick permission guard – caller should have requested permission first
            if (permManager.checkLocationPermission() != PermissionState.GRANTED) {
                trySend(null)
                close()
                return@callbackFlow
            }

            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,   // or keep BALANCED if battery matters more
                5_000L
            )
                .build()

            // emit cached location immediately so the flow isn't empty at start
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { trySend(it.toDeviceLocation()) }
            }


            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    trySend(loc.toDeviceLocation())
                }
            }

            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

            awaitClose {
                fusedClient.removeLocationUpdates(callback)
            }
        }
    }

    actual suspend fun checkLocationPermission(): PermissionState {
        return PermissionManager(context).checkLocationPermission()
    }

    actual suspend fun requestLocationPermission(): PermissionState {
        return PermissionManager(context).requestLocationPermission()
    }
}

private fun Location.toDeviceLocation(): DeviceLocation = DeviceLocation(
    coordinates = LatLong(lat = latitude, long = longitude),
    altitude = if (hasAltitude()) altitude.roundToInt() else null,
    heading = if (hasBearing()) bearing.roundToInt() else null,
    speed = if (hasSpeed()) speed.toDouble() else null,
    timestamp = time,
)

package org.lerchenflo.schneaggchatv3mp.utilities.location

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import kotlin.math.PI
import kotlin.math.roundToInt

actual class LocationService(private val context: Context) {

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    actual fun getLocationFlow(fastUpdates: Boolean): Flow<DeviceLocation?> {
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
                if (fastUpdates) 1_000L else 5_000L
            )
                .setMinUpdateDistanceMeters(if (fastUpdates) 0f else 20f)
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

    actual fun getHeadingFlow(): Flow<Float?> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (rotationSensor == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val azimuthDegrees = (orientation[0] * 180f / PI.toFloat() + 360f) % 360f
                trySend(azimuthDegrees)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)

        awaitClose {
            sensorManager.unregisterListener(listener)
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

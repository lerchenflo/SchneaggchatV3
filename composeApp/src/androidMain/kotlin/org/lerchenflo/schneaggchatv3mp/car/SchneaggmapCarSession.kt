package org.lerchenflo.schneaggchatv3mp.car

import android.content.Intent
import android.content.res.Configuration
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager

/**
 * One car session per Android Auto connection. Its lifecycle (not the phone Activity's) is what
 * marks the socket/location-sharing pipeline as "active" while only the car is in use - see
 * [AppLifecycleManager.setCarSessionActive].
 */
class SchneaggmapCarSession : Session() {

    private var screen: SchneaggmapCarScreen? = null

    init {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> AppLifecycleManager.setCarSessionActive(true)
                Lifecycle.Event.ON_STOP -> AppLifecycleManager.setCarSessionActive(false)
                else -> Unit
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen {
        val newScreen = SchneaggmapCarScreen(carContext)
        screen = newScreen
        return newScreen
    }

    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        super.onCarConfigurationChanged(newConfiguration)
        screen?.onCarConfigurationChanged()
    }
}

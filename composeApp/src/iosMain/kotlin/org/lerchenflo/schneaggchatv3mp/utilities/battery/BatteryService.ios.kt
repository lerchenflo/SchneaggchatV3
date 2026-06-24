package org.lerchenflo.schneaggchatv3mp.utilities.battery

import platform.UIKit.UIDevice
import kotlin.math.roundToInt

actual class BatteryService {

    actual fun getBatteryLevel(): Int? {
        val device = UIDevice.currentDevice
        device.batteryMonitoringEnabled = true

        val level = device.batteryLevel
        // -1.0 means unknown (e.g. battery monitoring just got enabled, or running on a simulator).
        if (level < 0f) return null

        return (level * 100).roundToInt().coerceIn(0, 100)
    }
}

package org.lerchenflo.schneaggchatv3mp.utilities.battery

import android.content.Context
import android.os.BatteryManager

actual class BatteryService(private val context: Context) {

    actual fun getBatteryLevel(): Int? {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return null

        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        // Negative values mean the platform couldn't determine the level.
        return level.takeIf { it in 0..100 }
    }
}

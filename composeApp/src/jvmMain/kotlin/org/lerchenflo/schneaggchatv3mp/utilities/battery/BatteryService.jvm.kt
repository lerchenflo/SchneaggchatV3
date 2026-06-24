package org.lerchenflo.schneaggchatv3mp.utilities.battery

actual class BatteryService {

    // No portable battery API on Desktop without an extra dependency (e.g. oshi) — report unknown.
    actual fun getBatteryLevel(): Int? = null
}

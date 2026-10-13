package org.lerchenflo.schneaggchatv3mp.utilities.battery

class DesktopBatteryService : BatteryService {

    // No portable battery API on Desktop without an extra dependency (e.g. oshi) — report unknown.
    override fun getBatteryLevel(): Int? = null
}

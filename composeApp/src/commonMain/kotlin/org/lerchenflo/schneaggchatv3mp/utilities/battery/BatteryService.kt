package org.lerchenflo.schneaggchatv3mp.utilities.battery

/**
 * Platform-agnostic battery level reader.
 */
expect class BatteryService {
    /**
     * Returns the current battery level as a percentage (0..100), or null if the
     * platform can't report it (e.g. Desktop, which has no portable battery API).
     */
    fun getBatteryLevel(): Int?
}

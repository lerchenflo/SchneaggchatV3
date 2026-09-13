package org.lerchenflo.schneaggchatv3mp.car

import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Entry point for the Android Auto integration - declared in the app manifest under the POI
 * category (see plans/ANDROID_AUTO_MAP_PLAN.md for why POI and not NAVIGATION). Only shows the
 * map ([SchneaggmapCarScreen]); chat messages reach the car through the normal notification
 * shade (MessagingStyle + voice reply, see Notifier.android.kt) and don't need a car screen.
 */
class SchneaggmapCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return if (debuggable) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(this)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession(): Session = SchneaggmapCarSession()
}

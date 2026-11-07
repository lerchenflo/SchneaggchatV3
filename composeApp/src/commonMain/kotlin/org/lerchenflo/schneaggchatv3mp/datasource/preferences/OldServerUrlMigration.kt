package org.lerchenflo.schneaggchatv3mp.datasource.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL_TEST

private val MOVED_SERVER_HOSTS = mapOf(
    "schneaggchatv3.lerchenflo.eu" to BASE_SERVER_URL.substringAfter("://"),
    "schneaggchatv3test.lerchenflo.eu" to BASE_SERVER_URL_TEST.substringAfter("://"),
)

private fun String.withCurrentServerHost(): String =
    MOVED_SERVER_HOSTS.entries.fold(this) { url, (oldHost, newHost) ->
        url.replace(oldHost, newHost, ignoreCase = true)
    }

/**
 * Moves a stored server URL from an old hostname to the current one. DataStore runs this at every
 * app start before the first read, so no request can go out with the old URL. A custom server URL
 * stays as it is.
 */
object OldServerUrlMigration : DataMigration<Preferences> {
    private val serverUrlKey = Preferencemanager.PrefsKeys.SERVER_URL

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val serverUrl = currentData[serverUrlKey] ?: return false
        return serverUrl != serverUrl.withCurrentServerHost()
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val serverUrl = currentData[serverUrlKey] ?: return currentData
        return currentData.toMutablePreferences().apply {
            this[serverUrlKey] = serverUrl.withCurrentServerHost()
        }.toPreferences()
    }

    override suspend fun cleanUp() = Unit
}

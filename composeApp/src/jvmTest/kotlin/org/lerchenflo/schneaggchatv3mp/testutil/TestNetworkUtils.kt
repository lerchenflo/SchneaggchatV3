package org.lerchenflo.schneaggchatv3mp.testutil

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.datasource.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.fakes.FakePreferencesDataStore

/**
 * A real [NetworkUtils] on [httpClient] (build it with the app's `createHttpClient(mockEngine)`),
 * used for both its plain and its auth client:
 * - a real `Preferencemanager` on an in-memory DataStore (`NetworkUtils` only reads the server URL
 *   from it, so requests go to the default server URL);
 * - an [unusedDependency] `AppVersion`, which only the login/registration calls read.
 */
fun createTestNetworkUtils(httpClient: HttpClient, logger: Logger): NetworkUtils =
    NetworkUtils(
        httpClient = httpClient,
        authHttpClient = httpClient,
        preferenceManager = Preferencemanager(
            prefs = FakePreferencesDataStore(),
            securePrefs = unusableKSafe(),
            loggingRepository = logger,
        ),
        loggingRepository = logger,
        appVersion = unusedDependency(),
    )

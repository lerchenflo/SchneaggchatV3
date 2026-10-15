package org.lerchenflo.schneaggchatv3mp.testutil

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.KtorMessageRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.MessageRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.fakes.FakePreferencesDataStore
import org.lerchenflo.schneaggchatv3mp.settings.data.remote.KtorSettingsRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.settings.data.remote.SettingsRemoteDataSource

/**
 * Real Ktor remote data sources on [httpClient] (build it with the app's
 * `createHttpClient(mockEngine)`), each with a real `Preferencemanager` on an in-memory DataStore:
 * the data sources only read the server URL from it, so requests go to the default server URL.
 */
fun createTestMessageRemoteDataSource(httpClient: HttpClient, logger: Logger): MessageRemoteDataSource =
    KtorMessageRemoteDataSource(
        httpClient = httpClient,
        preferenceManager = createTestPreferencemanager(logger),
        loggingRepository = logger,
    )

/** See [createTestMessageRemoteDataSource]. */
fun createTestSettingsRemoteDataSource(httpClient: HttpClient, logger: Logger): SettingsRemoteDataSource =
    KtorSettingsRemoteDataSource(
        httpClient = httpClient,
        preferenceManager = createTestPreferencemanager(logger),
        loggingRepository = logger,
    )

private fun createTestPreferencemanager(logger: Logger): Preferencemanager =
    Preferencemanager(
        prefs = FakePreferencesDataStore(),
        securePrefs = unusableKSafe(),
        loggingRepository = logger,
    )

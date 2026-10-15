package org.lerchenflo.schneaggchatv3mp.settings.data.remote

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.UserSettingsRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager

/** [SettingsRemoteDataSource] over Ktor, against the server URL from [Preferencemanager]. */
class KtorSettingsRemoteDataSource(
    httpClient: HttpClient,
    preferenceManager: Preferencemanager,
    loggingRepository: Logger,
) : SettingsRemoteDataSource {

    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    override suspend fun updateSettings(request: UserSettingsRequest): NetworkResult<Unit, NetworkingError> {
        return server.safePost(
            endpoint = "/users/settings",
            body = request
        )
    }
}

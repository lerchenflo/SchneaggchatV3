package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.TokenPair
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.safeNetworkCall
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.settings.data.DEVICETYPE

/**
 * `POST /auth/refresh` through the client **without** the Auth plugin. Deliberately not routed
 * through `NetworkUtils`: that class depends on the authenticated client, which depends on
 * `AuthSessionManager`, which depends on this - a cycle Koin cannot construct.
 */
class KtorAuthRefreshApi(
    private val plainHttpClient: HttpClient,
    private val preferencemanager: Preferencemanager,
    private val appVersion: AppVersion,
    private val loggingRepository: LoggingRepository,
) : AuthRefreshApi {

    @Serializable
    data class RefreshRequest(
        val refreshToken: String,
        val deviceName: String,
        val deviceType: DEVICETYPE,
    )

    override suspend fun refresh(refreshToken: String): NetworkResult<TokenPair, NetworkingError> {
        return safeNetworkCall<TokenPair>(loggingRepository) {
            plainHttpClient.post(preferencemanager.buildServerUrl("/auth/refresh")) {
                contentType(ContentType.Application.Json)
                setBody(
                    RefreshRequest(
                        refreshToken = refreshToken,
                        deviceName = appVersion.getDeviceName(),
                        deviceType = appVersion.getDeviceType(),
                    )
                )
            }
        }.trackConnectivity()
    }
}

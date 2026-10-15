package org.lerchenflo.schneaggchatv3mp.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.safeNetworkCall

/** [AppInfoRemoteDataSource] over Ktor, on the client without the bearer token. */
class KtorAppInfoRemoteDataSource(
    private val authHttpClient: HttpClient, //Without the bearer
    private val loggingRepository: Logger,
) : AppInfoRemoteDataSource {

    /** GETs [url] as given (not a server endpoint), without the bearer token. */
    private suspend inline fun <reified R> safeAuthGet(url: String): NetworkResult<R, NetworkingError> {
        return safeNetworkCall(loggingRepository) {
            authHttpClient.get(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    override suspend fun testServer(serverUrl: String) : NetworkResult<String, NetworkingError> {
        return safeNetworkCall(loggingRepository) {
            authHttpClient.get("$serverUrl/public/test")
        }
    }

    override suspend fun getChangeLog(githubUrl: String) : NetworkResult<String, NetworkingError> {
        return safeAuthGet(url = githubUrl)
    }

    override suspend fun getGitHubVersionJson(url: String) : NetworkResult<String, NetworkingError> {
        return safeAuthGet(url = url)
    }
}

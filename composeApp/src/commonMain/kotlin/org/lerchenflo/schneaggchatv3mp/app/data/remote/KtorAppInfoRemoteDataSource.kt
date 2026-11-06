package org.lerchenflo.schneaggchatv3mp.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.safeNetworkCall

/** [AppInfoRemoteDataSource] over Ktor, on the client without the bearer token. */
class KtorAppInfoRemoteDataSource(
    private val authHttpClient: HttpClient, //Without the bearer
    private val loggingRepository: Logger,
) : AppInfoRemoteDataSource {

    /** GETs [url] as given (not a server endpoint), without the bearer token. */
    private suspend inline fun <reified R> safeAuthGet(url: String): DataResult<R, DataError> {
        return safeNetworkCall(loggingRepository, "GET $url") {
            authHttpClient.get(url)
        }
    }

    override suspend fun testServer(serverUrl: String) : DataResult<String, DataError> {
        return safeNetworkCall(loggingRepository, "GET $serverUrl/public/test") {
            authHttpClient.get("$serverUrl/public/test")
        }
    }

    override suspend fun getChangeLog(githubUrl: String) : DataResult<String, DataError> {
        return safeAuthGet(url = githubUrl)
    }

    override suspend fun getGitHubVersionJson(url: String) : DataResult<String, DataError> {
        return safeAuthGet(url = url)
    }
}

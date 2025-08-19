package org.lerchenflo.schneaggchatv3mp.network

import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.http.HttpMethod
import io.ktor.util.network.UnresolvedAddressException
import org.lerchenflo.schneaggchatv3mp.network.util.NetworkError
import org.lerchenflo.schneaggchatv3mp.network.util.Result
import org.lerchenflo.schneaggchatv3mp.utilities.Base64Util
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
class NetworkUtils(
    private val httpClient: HttpClient
) {
    private val SERVERURL = "https://schneaggchat.lerchenflo.eu"

    /**
     * Perform a network request (GET when `get == true`, POST otherwise).
     *
     * - Encodes outgoing header values with Base64.
     * - Decodes incoming header values from Base64 (if decode fails, returns the raw header string).
     *
     * Returns Result.Success(map) where map contains all decoded response headers (lowercased keys)
     * and the response body under the key "body". On error returns Result.Error(NetworkError.*).
     */
    suspend fun <T> executeNetworkOperation(
        headers: Map<String, String>? = null,
        body: T? = null,
        get: Boolean = true,
        requestTimeoutMillis: Long = 5_000L
    ): Result<Map<String, String>, NetworkError> {
        try {
            val response: HttpResponse = httpClient.request {
                url(SERVERURL)
                method = if (get) HttpMethod.Get else HttpMethod.Post

                // Per-request timeout (requires HttpTimeout plugin to be installed in client).
                timeout {
                    this.requestTimeoutMillis = requestTimeoutMillis
                }

                // Built-in header: handy time (milliseconds)
                headers {
                    append("handytime", Base64Util.encode(Clock.System.now().toEpochMilliseconds().toString()))
                }

                // Additional headers (encode values)
                headers?.forEach { (k, v) ->
                    // normalize keys: trim newlines, toLowerCase
                    val key = k.replace("\n", "").lowercase()
                    val value = v.replace("\n", "")
                    if (key.isNotBlank() && value.isNotBlank()) {
                        header(key, Base64Util.encode(value))
                    }
                }

                // Body for POST
                if (!get && body.toString() != "") {
                    setBody(body.toString())
                }
            }

            val responseBody = response.bodyAsText()

            // Decode response headers
            val decodedHeaders = mutableMapOf<String, String>()
            response.headers.names().forEach { rawName ->
                // normalize key: strip newlines + lowercase
                val key = rawName.replace("\n", "").lowercase()
                val joined = response.headers.getAll(rawName)?.joinToString(",") ?: ""
                val decodedValue = try {
                    Base64Util.decode(joined)
                } catch (e: IllegalArgumentException) {
                    // not valid Base64, keep raw
                    joined
                }
                decodedHeaders[key] = decodedValue
            }

            // include body in the map (same behavior as your Java code)
            decodedHeaders["body"] = responseBody

            return Result.Success(decodedHeaders)
        } catch (e: UnresolvedAddressException) {
            return Result.Error(NetworkError.NO_INTERNET)
        } catch (e: HttpRequestTimeoutException) {
            return Result.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: io.ktor.client.network.sockets.SocketTimeoutException) {
            return Result.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: SocketTimeoutException) {
            return Result.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: Exception) {
            // You can log e.message here if you have a logger
            return Result.Error(NetworkError.UNKNOWN)
        }
    }



    suspend fun login(username: String, password: String): Result<String, NetworkError> {
        val headers = mapOf(
            "msgtype" to LOGINMESSAGE,
            "username" to username,
            "password" to password
        )

        return when (val res = executeNetworkOperation(headers = headers, body = "", get = true)) {
            is Result.Success -> {
                val map = res.data
                val body = map["body"] ?: ""
                Result.Success(body)
            }
            is Result.Error -> Result.Error(res.error)
        }
    }


}


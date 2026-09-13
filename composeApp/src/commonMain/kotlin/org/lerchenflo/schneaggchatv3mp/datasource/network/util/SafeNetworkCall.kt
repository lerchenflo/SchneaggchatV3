package org.lerchenflo.schneaggchatv3mp.datasource.network.util

import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository

/**
 * Runs one HTTP call and maps its result - success body, HTTP error status, or thrown exception -
 * to a [NetworkResult]. Shared by [org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils]
 * and the auth refresh API, which must not depend on `NetworkUtils` (that would close a dependency
 * cycle through the authenticated HTTP client).
 *
 * `response.body()` deserializes inside the try - a socket drop or serialization failure while
 * reading a successful reply lands in a catch below and comes back as an Error, even though the
 * server already committed the write. Callers that need idempotency rely on their own keys
 * (see `AppRepository.sendMessage`) rather than on this helper avoiding the case.
 */
suspend inline fun <reified R> safeNetworkCall(
    loggingRepository: LoggingRepository,
    crossinline block: suspend () -> HttpResponse
): NetworkResult<R, NetworkingError> {
    return try {
        val response = block()

        if (response.status.isSuccess()) {
            NetworkResult.Success(response.body())
        } else {
            NetworkResult.Error(mapHttpStatusToError(response.status.value, response.body<String>()))
        }
    } catch (e: UnresolvedAddressException) {
        println("DNS resolution failed - ${e.message}")
        NetworkResult.Error(NetworkingError.NoInternetConnection)
    } catch (e: ConnectTimeoutException) {
        println("Connection timeout - ${e.message}")
        NetworkResult.Error(NetworkingError.NetworkTimeout())
    } catch (e: HttpRequestTimeoutException) {
        println("HTTP request timeout - ${e.message}")
        NetworkResult.Error(NetworkingError.NetworkTimeout())
    } catch (e: SocketTimeoutException) {
        println("Going offline: Socket timeout - ${e.message}")
        NetworkResult.Error(NetworkingError.NetworkTimeout())
    } catch (e: IOException) {
        // Covers SocketTimeoutException, UnknownHostException, etc. on JVM/Android
        NetworkResult.Error(NetworkingError.NoInternetConnection)
    } catch (e: SerializationException) {
        println("Serialization error (staying online): ${e.message}")
        loggingRepository.logWarning("SerializationException: ${e.message}")
        NetworkResult.Error(NetworkingError.SerializationError(message = e.message))
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive() //Check for cancellation exceptions

        // Detect platform-specific network errors by message/type name: on iOS, NSURLErrorDomain
        // errors land here as they don't extend IOException
        val isNetworkingError = isNetworkException(e)
        println("Is network connection error: $isNetworkingError: ${e.message}")
        if (isNetworkingError) {
            println("Platform network exception - ${e.message}")
            NetworkResult.Error(NetworkingError.NoInternetConnection)
        } else {
            println("Unknown exception (staying online): ${e.message}")
            loggingRepository.logWarning("safeCall failed: ${e.message}")
            NetworkResult.Error(NetworkingError.Unknown(message = e.message))
        }
    }
}

/** Inspects the exception type name since iOS network errors don't have a common base class. */
fun isNetworkException(e: Exception): Boolean {
    val name = e::class.simpleName ?: ""
    val message = e.message ?: ""
    // Ktor body-deserialization errors mention class paths containing "network" - exclude them
    if (message.startsWith("Expected response body")) return false
    return name.contains("NSURLError", ignoreCase = true)
            || name.contains("Network", ignoreCase = true)
            || name.contains("Socket", ignoreCase = true)
            || name.contains("Connection", ignoreCase = true)
            || message.contains("network", ignoreCase = true)
            || message.contains("internet", ignoreCase = true)
            || message.contains("offline", ignoreCase = true)
            || message.contains("unreachable", ignoreCase = true)
}

/** Maps an HTTP status code (plus the response body as message) to a [NetworkingError]. */
fun mapHttpStatusToError(statusCode: Int, message: String?): NetworkingError {
    return when (statusCode) {
        400 -> NetworkingError.BadRequest(message = message)
        401 -> NetworkingError.Unauthorized(message = message)
        403 -> NetworkingError.Forbidden(message = message)
        404 -> NetworkingError.NotFound(message = message)
        408 -> NetworkingError.NetworkTimeout(message = message)
        409 -> NetworkingError.Conflict(message = message)
        413 -> NetworkingError.PayloadTooLarge(message = message)
        429 -> NetworkingError.TooManyRequests(message = message)
        in 500..599 -> NetworkingError.ServerError(message = message)
        else -> NetworkingError.Unknown(message = message)
    }
}

package org.lerchenflo.schneaggchatv3mp.datasource.network.util

import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.util.network.UnresolvedAddressException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.app.domain.isTransient

/**
 * Runs one HTTP call and maps its result - success body, HTTP error status, or thrown exception -
 * to a [DataResult]. Shared by the remote data sources (via
 * [org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient]) and the auth refresh API,
 * which must not depend on `KtorAuthRemoteDataSource` (that would close a dependency cycle through
 * the authenticated HTTP client).
 *
 * `response.body()` deserializes inside the try - a socket drop or serialization failure while
 * reading a successful reply lands in a catch below and comes back as an Error, even though the
 * server already committed the write. Callers that need idempotency rely on their own keys
 * (see `OfflineFirstMessageRepository.sendMessage`) rather than on this helper avoiding the case.
 */
suspend inline fun <reified R> safeNetworkCall(
    loggingRepository: Logger,
    request: String,
    crossinline block: suspend () -> HttpResponse
): DataResult<R, DataError> {
    return try {
        val response = block()

        if (response.status.isSuccess()) {
            DataResult.Success(response.body())
        } else {
            val error = mapHttpStatusToError(response.status.value, response.body<String>())
            loggingRepository.logRequestFailure(request, error)
            DataResult.Error(error)
        }
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive() //Check for cancellation exceptions

        val error = e.toDataError()
        loggingRepository.logRequestFailure(request, error, cause = e)
        DataResult.Error(error)
    }
}

/** Maps an exception thrown by a request to a [DataError]. Its text stays out of the popup. */
@PublishedApi
internal fun Exception.toDataError(): DataError = when {
    this is UnresolvedAddressException -> DataError.NoInternetConnection
    this is ConnectTimeoutException ||
        this is HttpRequestTimeoutException ||
        this is SocketTimeoutException -> DataError.NetworkTimeout(debugInfo = message)
    // Covers SocketTimeoutException, UnknownHostException, etc. on JVM/Android
    this is IOException -> DataError.NoInternetConnection
    this is SerializationException -> DataError.SerializationError(debugInfo = message)
    // On iOS, NSURLErrorDomain errors land here as they don't extend IOException
    isNetworkException(this) -> DataError.NoInternetConnection
    else -> DataError.Unknown(debugInfo = message)
}

/**
 * Logs a failed request without its query string, which can hold what the user typed (a search
 * term). Transient errors only go to the console: the log table keeps 50 rows, and an offline
 * device fails a request every 5 seconds, which would push out every other entry.
 * A failed log write never fails the request.
 */
@PublishedApi
internal suspend fun Logger.logRequestFailure(request: String, error: DataError, cause: Exception? = null) {
    val details = listOfNotNull(cause?.let { it::class.simpleName }, error.debugInfo ?: cause?.message)
        .joinToString(": ")
        .replace(urlQuery, "")
        .take(MAX_LOGGED_DETAILS_LENGTH)
    val line = "${request.substringBefore('?')} failed (${error.errorCode}): $details"

    if (error.isTransient()) {
        println(line)
        return
    }

    try {
        logWarning(line)
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        println("$line (log write failed: ${e.message})")
    }
}

private const val MAX_LOGGED_DETAILS_LENGTH = 500

/** The query string of a URL inside an exception text. */
private val urlQuery = Regex("""\?[^\s`'"]+""")

/** Inspects the exception type name since iOS network errors don't have a common base class. */
private fun isNetworkException(e: Exception): Boolean {
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

/**
 * Maps an HTTP status code plus the response body to a [DataError]. The body stays in
 * [DataError.debugInfo]; [DataError.message] only gets text a user can read.
 */
@PublishedApi
internal fun mapHttpStatusToError(statusCode: Int, body: String?): DataError {
    val json = runCatching { Json.parseToJsonElement(body.orEmpty()).jsonObject }.getOrNull()
    val message = readableMessage(body, json)

    return when (statusCode) {
        400 -> DataError.BadRequest(message = message, debugInfo = body)
        401 -> DataError.Unauthorized(message = message, debugInfo = body)
        403 -> DataError.Forbidden(message = message, debugInfo = body)
        404 -> DataError.NotFound(message = message, debugInfo = body)
        408 -> DataError.NetworkTimeout(message = message, debugInfo = body)
        409 -> DataError.Conflict(message = message, debugInfo = body)
        413 -> DataError.PayloadTooLarge(message = message, debugInfo = body)
        429 -> DataError.TooManyRequests(message = message, debugInfo = body, retryAfter = retryAfter(json))
        in 500..599 -> DataError.ServerError(message = message, debugInfo = body, errorCode = statusCode)
        else -> DataError.Unknown(message = message, debugInfo = body, errorCode = statusCode)
    }
}

/**
 * Server reasons are plain text, validation replies list them under `errors`. Other JSON (a
 * rate limit) and HTML pages (a proxy in front of the server) are nothing to show a user.
 */
private fun readableMessage(body: String?, json: JsonObject?): String? = when {
    json != null -> (json["errors"] as? JsonArray)?.joinToString("\n") { (it as? JsonPrimitive)?.content.orEmpty() }
    body.isNullOrBlank() || body.trimStart().startsWith("<") -> null
    else -> body
}

/** The rate limit reply is `{"error":"rate_limited","retryAfterSeconds":N}` (RateLimitFilter.deny). */
private fun retryAfter(json: JsonObject?): Duration? =
    (json?.get("retryAfterSeconds") as? JsonPrimitive)?.longOrNull?.takeIf { it > 0 }?.seconds

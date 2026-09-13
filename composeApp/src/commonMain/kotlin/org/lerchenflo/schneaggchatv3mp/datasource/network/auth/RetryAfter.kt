package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Reads the server's rate-limit hint out of a 429. The server answers
 * `{"error":"rate_limited","retryAfterSeconds":N}` (RateLimitFilter.deny) and `safeNetworkCall`
 * keeps that body as the error message, so no header access is needed.
 */
object RetryAfter {
    private val bodyPattern = Regex("\"retryAfterSeconds\"\\s*:\\s*(\\d+)")

    fun parse(error: NetworkingError): Duration? {
        if (error !is NetworkingError.TooManyRequests) return null
        val seconds = bodyPattern.find(error.message.orEmpty())?.groupValues?.get(1)?.toLongOrNull() ?: return null
        if (seconds <= 0) return null
        return seconds.seconds
    }
}

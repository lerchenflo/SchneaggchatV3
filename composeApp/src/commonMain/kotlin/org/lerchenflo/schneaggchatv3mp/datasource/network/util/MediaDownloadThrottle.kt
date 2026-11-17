package org.lerchenflo.schneaggchatv3mp.datasource.network.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * One shared queue for media downloads (profile pictures, group pictures, image and voice
 * messages). A sync with many missing files would otherwise use up the server's rate limit
 * (200 requests per minute per IP) and get messages and everything else refused with a 429.
 *
 * Downloads run one at a time and start at least [gap] apart, so media takes at most
 * 120 requests per minute. A single download after a quiet moment starts at once.
 * A 429 pauses the whole queue for the server's retry-after ([defaultRetryAfter] when it gave
 * none) and retries the same request, at most [maxRetries] times.
 */
class MediaDownloadThrottle(
    private val gap: Duration = 500.milliseconds,
    private val defaultRetryAfter: Duration = 10.seconds,
    private val maxRetries: Int = 3,
) {
    private val mutex = Mutex()
    private var lastStart: TimeMark? = null

    suspend fun <D> download(request: suspend () -> DataResult<D, DataError>): DataResult<D, DataError> =
        mutex.withLock { downloadWithRetries(request) }

    private suspend fun <D> downloadWithRetries(request: suspend () -> DataResult<D, DataError>): DataResult<D, DataError> {
        repeat(maxRetries) {
            val result = downloadAfterGap(request)
            val retryAfter = result.retryAfter() ?: return result
            delay(retryAfter)
        }
        return downloadAfterGap(request)
    }

    private suspend fun <D> downloadAfterGap(request: suspend () -> DataResult<D, DataError>): DataResult<D, DataError> {
        // A negative remaining wait (the gap already passed) returns at once
        lastStart?.let { delay(gap - it.elapsedNow()) }
        lastStart = TimeSource.Monotonic.markNow()
        return request()
    }

    /** How long to wait when this result is a 429, null for every other result. */
    private fun DataResult<*, DataError>.retryAfter(): Duration? {
        val error = (this as? DataResult.Error)?.error as? DataError.TooManyRequests ?: return null
        return error.retryAfter ?: defaultRetryAfter
    }
}

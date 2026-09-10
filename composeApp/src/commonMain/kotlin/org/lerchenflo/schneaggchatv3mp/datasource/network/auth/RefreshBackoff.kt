package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Pure exponential backoff with jitter for refresh retries. No I/O, no clock: it only computes
 * how long to wait; the caller decides when the wait starts.
 *
 * Defaults (AUTH_SESSION_REBUILD_PLAN decision 3): 2 s base, doubling, 2 min cap, +/-25 % jitter.
 * Many devices dropped by one server restart therefore retry spread out instead of in lockstep.
 */
class RefreshBackoff(
    private val base: Duration = 2.seconds,
    private val factor: Double = 2.0,
    private val cap: Duration = 2.minutes,
    private val jitterFraction: Double = 0.25,
    private val random: Random = Random.Default,
) {
    /** Consecutive failures since the last [reset]. */
    var attempt: Int = 0
        private set

    /**
     * Delay before the next attempt. Doubles per consecutive failure, capped, jittered. A server
     * supplied [retryAfter] (429) wins when it is longer than the computed delay.
     */
    fun nextDelay(retryAfter: Duration? = null): Duration {
        val raw = (base * factor.pow(attempt)).coerceAtMost(cap)
        attempt++
        val jittered = jitter(raw)
        return if (retryAfter != null && retryAfter > jittered) retryAfter else jittered
    }

    /** Delay for a permanently failing attempt: straight to the cap, still jittered. */
    fun capDelay(): Duration {
        attempt++
        return jitter(cap)
    }

    fun reset() {
        attempt = 0
    }

    private fun jitter(duration: Duration): Duration {
        if (jitterFraction <= 0.0) return duration
        val multiplier = 1.0 + (random.nextDouble() * 2.0 - 1.0) * jitterFraction
        return duration * multiplier
    }
}

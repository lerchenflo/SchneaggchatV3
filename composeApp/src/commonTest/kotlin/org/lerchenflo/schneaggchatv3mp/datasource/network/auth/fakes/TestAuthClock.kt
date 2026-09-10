@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes

import kotlinx.coroutines.test.TestCoroutineScheduler
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthClock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Reads the test scheduler's virtual time, so `delay` inside the manager and `now()` agree. */
class TestAuthClock(
    private val scheduler: TestCoroutineScheduler,
    private val baseEpochMillis: Long = BASE_EPOCH_MILLIS,
) : AuthClock {

    override fun now(): Instant = Instant.fromEpochMilliseconds(baseEpochMillis + scheduler.currentTime)

    companion object {
        /** 2027-01-15, comfortably after any `exp` a real token would carry when these tests were written. */
        const val BASE_EPOCH_MILLIS = 1_800_000_000_000L
    }
}

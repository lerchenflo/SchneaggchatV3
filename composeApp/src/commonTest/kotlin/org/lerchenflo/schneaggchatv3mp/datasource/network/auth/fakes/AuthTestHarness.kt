@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.TokenPair
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthSessionManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.RefreshBackoff
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * One manager wired to fakes on the test scope's background scope (so its timers and collectors
 * run on virtual time and die with the test). Jitter is off by default so timing assertions are
 * exact; `RefreshBackoff` gets its own jitter tests.
 */
class AuthTestHarness(
    testScope: TestScope,
    storedTokens: TokenPair? = null,
    config: AuthSessionManager.Config = AuthSessionManager.Config(),
    jitterFraction: Double = 0.0,
) {
    val clock = TestAuthClock(testScope.testScheduler)
    val store = FakeAuthSessionStore(storedTokens)
    val api = FakeAuthRefreshApi { clock.now().toEpochMilliseconds() }
    val sink = RecordingAuthEventSink()
    val log = RecordingAuthLog()
    val online = MutableStateFlow(true)
    val resumed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val backoff = RefreshBackoff(jitterFraction = jitterFraction, random = Random(42))

    val manager = AuthSessionManager(
        store = store,
        api = api,
        sink = sink,
        clock = clock,
        log = log,
        scope = testScope.backgroundScope,
        onlineFlow = online,
        appResumedEvents = resumed,
        backoff = backoff,
        config = config,
    )

    fun nowMillis(): Long = clock.now().toEpochMilliseconds()

    fun accessToken(validFor: Duration = 15.minutes, sub: String = TestJwt.DEFAULT_SUBJECT): String =
        TestJwt.token(sub = sub, expEpochMillis = nowMillis() + validFor.inWholeMilliseconds, type = "access_token")

    fun refreshToken(validFor: Duration = 30.days, sub: String = TestJwt.DEFAULT_SUBJECT): String =
        TestJwt.token(sub = sub, expEpochMillis = nowMillis() + validFor.inWholeMilliseconds, type = "refresh_token")

    fun pair(
        accessValidFor: Duration = 15.minutes,
        refreshValidFor: Duration = 30.days,
        sub: String = TestJwt.DEFAULT_SUBJECT,
    ): TokenPair = TokenPair(accessToken(accessValidFor, sub), refreshToken(refreshValidFor, sub))

    /** Puts a pair into storage as if a previous run had left it there. */
    fun storeSession(pair: TokenPair): TokenPair {
        store.stored = pair
        return pair
    }
}

/** Advances virtual time and runs everything that became due, including tasks scheduled exactly at the new time. */
fun TestScope.advance(duration: Duration) {
    advanceTimeBy(duration)
    runCurrent()
}

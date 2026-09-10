package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.AuthTestHarness
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.TestAuthClock
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.advance
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** R7, D2, D9, D10, D11, cases 13, 17, 44, 58. */
class BackoffTest {

    // ---------------------------------------------------------------- pure RefreshBackoff

    @Test
    fun doublesAndCaps() {
        val backoff = RefreshBackoff(jitterFraction = 0.0)
        val delays = List(8) { backoff.nextDelay() }
        assertEquals(listOf(2, 4, 8, 16, 32, 64, 120, 120).map { it.seconds }, delays)
        assertEquals(8, backoff.attempt)
    }

    @Test
    fun jitterStaysWithinPlusMinusTwentyFivePercent() {
        val backoff = RefreshBackoff(jitterFraction = 0.25, random = Random(7))
        val samples = List(300) { backoff.reset(); backoff.nextDelay() }
        val outOfBounds = samples.filter { it < 1.5.seconds || it > 2.5.seconds }
        assertTrue(outOfBounds.isEmpty(), "out of bounds: $outOfBounds")
        assertTrue(samples.distinct().size > 50, "jitter must actually spread the delays")
    }

    @Test
    fun retryAfterWinsOnlyWhenLarger() {
        val backoff = RefreshBackoff(jitterFraction = 0.0)
        assertEquals(47.seconds, backoff.nextDelay(retryAfter = 47.seconds))
        assertEquals(4.seconds, backoff.nextDelay(retryAfter = 1.seconds))
    }

    @Test
    fun resetStartsOver() {
        val backoff = RefreshBackoff(jitterFraction = 0.0)
        repeat(4) { backoff.nextDelay() }
        backoff.reset()
        assertEquals(0, backoff.attempt)
        assertEquals(2.seconds, backoff.nextDelay())
    }

    @Test
    fun capDelayGoesStraightToTheCap() {
        val backoff = RefreshBackoff(jitterFraction = 0.0)
        assertEquals(120.seconds, backoff.capDelay())
        assertEquals(1, backoff.attempt)
    }

    // ---------------------------------------------------------------- manager integration

    private suspend fun TestScope.activeHarness(): AuthTestHarness {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        assertIs<SessionCheck.Active>(h.manager.ensureSession())
        return h
    }

    private fun AuthTestHarness.callTimesMillis(): List<Long> = api.calls.map { it.atEpochMillis - TestAuthClock.BASE_EPOCH_MILLIS }

    @Test
    fun retryAfterSecondsFromA429IsHonoured() = runTest {
        val h = activeHarness()
        h.api.enqueueError(NetworkingError.TooManyRequests(message = """{"error":"rate_limited","retryAfterSeconds":47}"""))

        val outcome = assertIs<RefreshOutcome.Retryable>(h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(47.seconds, outcome.retryAfter)

        val suppressed = assertIs<RefreshOutcome.Retryable>(h.manager.refresh(RefreshReason.Reactive401))
        assertIs<NetworkingError.TooManyRequests>(suppressed.error)
        assertEquals(1, h.api.calls.size, "no network while the server told us to wait")

        advance(46.seconds)
        h.manager.refresh(RefreshReason.Reactive401)
        assertEquals(1, h.api.calls.size)

        advance(1.seconds)
        assertEquals(2, h.api.calls.size, "the scheduler retried exactly when the server allowed")
        assertTrue(h.sink.invalidations.isEmpty(), "a 429 is never a logout")
        assertNotNull(h.store.stored)
    }

    @Test
    fun cooldownGrowsPerConsecutiveFailure() = runTest {
        val h = activeHarness()   // api fallback: NoInternetConnection

        h.manager.refresh(RefreshReason.Reactive401)
        advance(2.seconds)
        advance(4.seconds)
        advance(8.seconds)

        assertEquals(listOf(0L, 2_000L, 6_000L, 14_000L), h.callTimesMillis())
        assertEquals(4, h.backoff.attempt)
    }

    @Test
    fun successResetsTheBackoff() = runTest {
        val h = activeHarness()
        h.manager.refresh(RefreshReason.Reactive401)
        advance(2.seconds)
        advance(4.seconds)
        h.api.enqueueSuccess(h.pair())
        advance(8.seconds)
        assertEquals(0, h.backoff.attempt)
        assertIs<AuthSessionState.Active>(h.manager.state.value)

        advance(1.seconds)
        val outcome = assertIs<RefreshOutcome.Retryable>(h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(2.seconds, outcome.retryAfter, "first failure after a success starts at the base")
    }

    @Test
    fun freshLoginResetsTheFailureCounter() = runTest {
        val h = activeHarness()
        h.manager.refresh(RefreshReason.Reactive401)
        advance(2.seconds)
        advance(4.seconds)
        assertEquals(3, h.backoff.attempt)

        h.manager.onLoggedIn(h.pair())
        assertEquals(0, h.backoff.attempt)

        advance(1.seconds)
        val outcome = assertIs<RefreshOutcome.Retryable>(h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(2.seconds, outcome.retryAfter, "D11: first failure after a login must not arm a long window")
    }

    @Test
    fun connectivityRestoreResetsAndRefreshesImmediately() = runTest {
        val h = activeHarness()
        h.manager.refresh(RefreshReason.Reactive401)
        advance(2.seconds)
        advance(4.seconds)
        assertEquals(3, h.api.calls.size)   // cooldown now 8 s, until t = 14 s

        advance(1.seconds)
        h.online.value = false
        runCurrent()
        h.online.value = true
        runCurrent()

        assertEquals(4, h.api.calls.size, "D2: the backoff window must not outlive the outage")
        assertEquals(7_000L, h.callTimesMillis().last())
    }

    @Test
    fun flappingConnectivityIsDebounced() = runTest {
        val h = activeHarness()
        h.manager.refresh(RefreshReason.Reactive401)
        assertEquals(1, h.api.calls.size)

        repeat(10) {
            h.online.value = false
            runCurrent()
            h.online.value = true
            runCurrent()
        }

        assertEquals(2, h.api.calls.size, "ten flaps within the debounce window spend one refresh")
    }

    @Test
    fun appResumeRefreshesWhenDegraded() = runTest {
        val h = activeHarness()
        h.manager.refresh(RefreshReason.Reactive401)
        assertIs<AuthSessionState.Degraded>(h.manager.state.value)

        h.resumed.tryEmit(Unit)
        runCurrent()

        assertEquals(2, h.api.calls.size)
    }

    @Test
    fun successfulAuthenticatedRequestResetsTheBackoff() = runTest {
        val h = activeHarness()
        h.manager.refresh(RefreshReason.Reactive401)
        advance(2.seconds)
        advance(4.seconds)
        assertEquals(3, h.backoff.attempt)

        h.manager.onAuthenticatedRequestSucceeded()
        runCurrent()
        assertEquals(0, h.backoff.attempt)

        h.manager.refresh(RefreshReason.Reactive401)
        assertEquals(4, h.api.calls.size, "the stale cooldown no longer holds the next 401 back")
    }

    @Test
    fun manualRefreshIgnoresTheCooldown() = runTest {
        val h = activeHarness()
        h.manager.refresh(RefreshReason.Reactive401)
        assertEquals(1, h.api.calls.size)

        h.manager.refresh(RefreshReason.Manual)
        assertEquals(2, h.api.calls.size)
    }

    @Test
    fun restoreWithAFreshTokenOnlyResetsTheBackoff() = runTest {
        val h = activeHarness()
        h.online.value = false
        runCurrent()
        h.online.value = true
        runCurrent()

        assertTrue(h.api.calls.isEmpty(), "nothing to refresh, nothing refreshed (case 58)")
        assertEquals(0, h.backoff.attempt)
        assertIs<AuthSessionState.Active>(h.manager.state.value)
    }
}

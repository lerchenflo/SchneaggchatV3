package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.AuthTestHarness
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.advance
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** R12, cases 13, 15, 36, 57. */
class ConnectivityTest {

    @Test
    fun optionalRefreshesAreNotAttemptedWhileOffline() = runTest {
        val h = AuthTestHarness(this)
        h.online.value = false
        h.storeSession(h.pair(accessValidFor = (-1).minutes))

        for (reason in listOf(RefreshReason.Proactive, RefreshReason.Scheduled, RefreshReason.SocketHandshake)) {
            val outcome = assertIs<RefreshOutcome.Retryable>(h.manager.refresh(reason), "$reason")
            assertEquals(NetworkingError.NoInternetConnection, outcome.error)
            assertEquals(Duration.ZERO, outcome.retryAfter)
        }
        assertTrue(h.api.calls.isEmpty())
        assertEquals(0, h.backoff.attempt, "an offline skip is not a failure")
    }

    @Test
    fun reactive401IsAttemptedEvenWhileTheOnlineFlagIsStale() = runTest {
        val h = AuthTestHarness(this)
        h.online.value = false
        h.storeSession(h.pair())
        h.api.enqueueSuccess(h.pair())

        assertEquals(RefreshOutcome.Success, h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(1, h.api.calls.size)
    }

    @Test
    fun exactlyOneRefreshFiresWhenConnectivityReturns() = runTest {
        val h = AuthTestHarness(this)
        h.online.value = false
        h.storeSession(h.pair(accessValidFor = (-1).minutes))
        h.manager.ensureSession()
        h.api.enqueueSuccess(h.pair())

        repeat(5) { launch { h.manager.refresh(RefreshReason.Proactive) } }
        repeat(5) { h.manager.currentTokens() }
        runCurrent()
        assertTrue(h.api.calls.isEmpty())

        h.online.value = true
        runCurrent()

        assertEquals(1, h.api.calls.size, "not one per queued caller")
        assertIs<AuthSessionState.Active>(h.manager.state.value)
    }

    @Test
    fun scheduledRetryGatedOfflineResumesOnRestore() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        h.manager.refresh(RefreshReason.Reactive401)          // fails: offline fallback, retry in 2 s
        h.online.value = false
        runCurrent()

        advance(10.seconds)
        assertEquals(1, h.api.calls.size, "the timer fired into the offline gate and did not spin")

        h.api.enqueueSuccess(h.pair())
        h.online.value = true
        runCurrent()
        assertEquals(2, h.api.calls.size)
        assertIs<AuthSessionState.Active>(h.manager.state.value)
    }

    @Test
    fun goingOfflineKeepsTheCredentialsAndTheSession() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()

        repeat(3) {
            h.manager.refresh(RefreshReason.Manual)
        }

        assertNotNull(h.store.stored)
        assertNotNull(h.manager.currentTokens())
        assertTrue(h.sink.invalidations.isEmpty(), "DNS failure / no route is a state, not a logout (case 15)")
        assertIs<AuthSessionState.Degraded>(h.manager.state.value)
    }

    @Test
    fun flappingDuringBackoffResetsOnceAndDoesNotStorm() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        h.manager.refresh(RefreshReason.Reactive401)
        advance(2.seconds)
        assertEquals(2, h.api.calls.size)

        repeat(20) {
            h.online.value = false
            runCurrent()
            h.online.value = true
            runCurrent()
        }
        assertEquals(3, h.api.calls.size)
    }
}

package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.AuthTestHarness
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.RefreshResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.advance
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** R8, D14, cases 19, 23. */
class ProactiveRefreshTest {

    @Test
    fun currentTokensNeverAwaitsTheNetwork() = runTest {
        val h = AuthTestHarness(this)
        val stored = h.storeSession(h.pair(accessValidFor = 1.minutes))
        h.api.enqueueHang()

        val tokens = h.manager.currentTokens()

        assertEquals(stored, tokens, "the request path gets what is stored, immediately")
        runCurrent()
        assertEquals(1, h.api.calls.size, "and a proactive refresh was kicked off in the background")
    }

    @Test
    fun nearExpiryLaunchesAtMostOneBackgroundRefresh() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = 1.minutes))
        val gate = CompletableDeferred<RefreshResult>()
        h.api.enqueueGate(gate)

        repeat(5) { h.manager.currentTokens() }
        runCurrent()
        repeat(5) { h.manager.currentTokens() }
        runCurrent()

        assertEquals(1, h.api.calls.size)
        gate.complete(NetworkResult.Success(h.pair()))
        runCurrent()
        assertIs<AuthSessionState.Active>(h.manager.state.value)
    }

    @Test
    fun farFromExpiryNothingIsLaunched() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = 15.minutes))

        repeat(3) { h.manager.currentTokens() }
        runCurrent()

        assertTrue(h.api.calls.isEmpty())
    }

    @Test
    fun schedulerFiresTwoMinutesBeforeExpiry() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = 15.minutes))
        assertIs<SessionCheck.Active>(h.manager.ensureSession())
        val fresh = h.pair()
        h.api.enqueueSuccess(fresh)

        advance(13.minutes - 1.milliseconds)
        assertTrue(h.api.calls.isEmpty())

        advance(1.milliseconds)
        assertEquals(1, h.api.calls.size)
        assertEquals(fresh, h.store.stored)
    }

    @Test
    fun successReArmsTheTimerForTheNewToken() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = 15.minutes))
        h.manager.ensureSession()
        // Minted when the call happens, so the new token's expiry is relative to that moment.
        h.api.enqueue { NetworkResult.Success(h.pair(accessValidFor = 15.minutes)) }
        advance(13.minutes)
        assertEquals(1, h.api.calls.size)

        h.api.enqueue { NetworkResult.Success(h.pair(accessValidFor = 15.minutes)) }
        advance(13.minutes - 1.milliseconds)
        assertEquals(1, h.api.calls.size)
        advance(1.milliseconds)
        assertEquals(2, h.api.calls.size)
    }

    @Test
    fun aFreshTokenTheClockAlreadyConsidersExpiredDoesNotLoop() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        // Device clock hours ahead: everything the server issues already looks expired here.
        h.api.enqueueSuccess(h.pair(accessValidFor = (-1).hours))

        assertEquals(RefreshOutcome.Success, h.manager.refresh(RefreshReason.Reactive401))

        repeat(20) {
            advance(30.seconds)
            h.manager.currentTokens()
            runCurrent()
        }

        assertEquals(1, h.api.calls.size, "no proactive loop under clock skew - the reactive 401 path owns it")
        assertIs<AuthSessionState.Active>(h.manager.state.value)
        assertTrue(h.log.lines.any { "proactive refresh disabled" in it })
    }

    @Test
    fun proactiveTimerNeverFiresBeforeThePostSuccessFloor() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        // Server answers with an access token that has only 90 s left (clock skew of a minute or two).
        h.api.enqueueSuccess(h.pair(accessValidFor = 90.seconds))
        h.manager.refresh(RefreshReason.Reactive401)
        assertEquals(1, h.api.calls.size)

        advance(59.seconds)
        assertEquals(1, h.api.calls.size)
        advance(1.seconds)
        assertEquals(2, h.api.calls.size, "next attempt at the 60 s floor, not immediately")
    }
}

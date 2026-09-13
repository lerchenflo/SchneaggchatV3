package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.AuthTestHarness
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.RefreshResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.advance
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** R3, cases 1-3, 6, 53. */
class SingleFlightTest {

    @Test
    fun concurrentCallersShareOneApiCall() = runTest {
        val h = AuthTestHarness(this)
        val old = h.storeSession(h.pair(accessValidFor = (-1).minutes))
        val gate = CompletableDeferred<RefreshResult>()
        h.api.enqueueGate(gate)

        val results = mutableListOf<RefreshOutcome>()
        val jobs = List(5) {
            launch { results += h.manager.refresh(RefreshReason.Reactive401, presentedRefreshToken = old.refreshToken) }
        }
        runCurrent()

        assertEquals(1, h.api.calls.size, "exactly one network refresh for five concurrent 401s")
        assertEquals(old.refreshToken, h.api.calls.single().refreshToken)

        val fresh = h.pair()
        gate.complete(NetworkResult.Success(fresh))
        runCurrent()

        assertTrue(jobs.all { it.isCompleted })
        assertEquals(5, results.size)
        assertTrue(results.all { it == RefreshOutcome.Success }, "all waiters share the outcome: $results")
        assertEquals(fresh, h.store.stored)
        assertEquals(listOf(fresh), h.sink.active)
        assertEquals(fresh, h.manager.currentTokens())
    }

    @Test
    fun stalePresentedRefreshTokenIsAnsweredFromMirror() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())

        val outcome = h.manager.refresh(RefreshReason.Reactive401, presentedRefreshToken = "rotated-away")

        assertEquals(RefreshOutcome.Success, outcome)
        assertTrue(h.api.calls.isEmpty(), "no rotation spent on a token that is no longer current")
    }

    @Test
    fun stalePresentedAccessTokenIsAnsweredFromMirror() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())

        val outcome = h.manager.refresh(RefreshReason.Reactive401, presentedAccessToken = "access-from-before-the-rotation")

        assertEquals(RefreshOutcome.Success, outcome)
        assertTrue(h.api.calls.isEmpty())
    }

    @Test
    fun blankPresentedTokenMeansNoPreference() = runTest {
        val h = AuthTestHarness(this)
        val old = h.storeSession(h.pair())

        h.manager.refresh(RefreshReason.Reactive401, presentedRefreshToken = "", presentedAccessToken = "")

        assertEquals(1, h.api.calls.size)
        assertEquals(old.refreshToken, h.api.calls.single().refreshToken)
    }

    @Test
    fun socketHandshakeAndHttpRefreshShareTheFlight() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = (-1).minutes))
        val gate = CompletableDeferred<RefreshResult>()
        h.api.enqueueGate(gate)

        val outcomes = mutableListOf<RefreshOutcome>()
        launch { outcomes += h.manager.refresh(RefreshReason.Reactive401) }
        launch { outcomes += h.manager.refresh(RefreshReason.SocketHandshake) }
        runCurrent()
        assertEquals(1, h.api.calls.size)

        gate.complete(NetworkResult.Success(h.pair()))
        runCurrent()
        assertEquals(listOf<RefreshOutcome>(RefreshOutcome.Success, RefreshOutcome.Success), outcomes)
    }

    @Test
    fun reactiveRefreshRightAfterSuccessIsAnsweredFromMirror() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.api.enqueueSuccess(h.pair())
        assertEquals(RefreshOutcome.Success, h.manager.refresh(RefreshReason.Reactive401))

        // Hard floor (1 s): a 401 for a token the server issued a moment ago is not staleness.
        assertEquals(RefreshOutcome.Success, h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(1, h.api.calls.size)

        advance(1.seconds)
        h.manager.refresh(RefreshReason.Reactive401)
        assertEquals(2, h.api.calls.size)
    }

    @Test
    fun optionalRefreshWithinPostSuccessFloorIsAnsweredFromMirror() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.api.enqueueSuccess(h.pair())
        h.manager.refresh(RefreshReason.Reactive401)

        advance(30.seconds)
        assertEquals(RefreshOutcome.Success, h.manager.refresh(RefreshReason.SocketHandshake))
        assertEquals(RefreshOutcome.Success, h.manager.refresh(RefreshReason.Proactive))
        assertEquals(1, h.api.calls.size, "socket/proactive reasons wait out the 60 s post-success floor")

        advance(30.seconds)
        h.manager.refresh(RefreshReason.SocketHandshake)
        assertEquals(2, h.api.calls.size)
    }
}

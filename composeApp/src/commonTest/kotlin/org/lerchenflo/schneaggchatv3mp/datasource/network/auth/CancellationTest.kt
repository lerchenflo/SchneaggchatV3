package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.AuthTestHarness
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.RefreshResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/** R4, D1, cases 4, 5, 32, 56. */
class CancellationTest {

    @Test
    fun cancellingTheWaiterDoesNotCancelTheRefresh() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = (-1).minutes))
        val gate = CompletableDeferred<RefreshResult>()
        h.api.enqueueGate(gate)

        var outcome: RefreshOutcome? = null
        val waiter = launch { outcome = h.manager.refresh(RefreshReason.Reactive401) }
        runCurrent()
        assertEquals(1, h.api.calls.size)

        waiter.cancel()
        runCurrent()
        assertTrue(waiter.isCancelled)
        assertTrue(waiter.isCompleted, "a cancelled waiter must not stay parked behind the refresh")
        assertNull(outcome)

        val fresh = h.pair()
        gate.complete(NetworkResult.Success(fresh))
        runCurrent()

        assertEquals(fresh, h.store.stored, "the refresh outlived its waiter and persisted the rotated pair")
        assertEquals(fresh, h.manager.currentTokens())
        assertEquals(listOf(fresh), h.sink.active)
    }

    @Test
    fun allWaitersCancelledStillPersists() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = (-1).minutes))
        val gate = CompletableDeferred<RefreshResult>()
        h.api.enqueueGate(gate)

        val waiters = List(3) { launch { h.manager.refresh(RefreshReason.Reactive401) } }
        runCurrent()
        waiters.forEach { it.cancel() }
        runCurrent()
        assertTrue(waiters.all { it.isCancelled })

        val fresh = h.pair()
        gate.complete(NetworkResult.Success(fresh))
        runCurrent()

        // The server already rotated: the pair must land even with nobody waiting (cases 5, 32).
        assertEquals(fresh, h.store.stored)
        assertEquals(1, h.store.writes)
    }

    @Test
    fun laterCallerGetsTheResultOfTheRefreshThatOutlivedItsWaiter() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = (-1).minutes))
        val gate = CompletableDeferred<RefreshResult>()
        h.api.enqueueGate(gate)

        val waiter = launch { h.manager.refresh(RefreshReason.Reactive401) }
        runCurrent()
        waiter.cancel()
        runCurrent()

        val fresh = h.pair()
        gate.complete(NetworkResult.Success(fresh))
        runCurrent()

        assertEquals(RefreshOutcome.Success, h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(1, h.api.calls.size, "answered from the mirror, no second rotation")
    }

    @Test
    fun waiterCancelledWhileAnotherStillWaitsDoesNotDisturbIt() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = (-1).minutes))
        val gate = CompletableDeferred<RefreshResult>()
        h.api.enqueueGate(gate)

        var survivorOutcome: RefreshOutcome? = null
        val leaver = launch { h.manager.refresh(RefreshReason.Reactive401) }
        launch { survivorOutcome = h.manager.refresh(RefreshReason.SocketHandshake) }
        runCurrent()
        leaver.cancel()
        runCurrent()

        gate.complete(NetworkResult.Success(h.pair()))
        runCurrent()

        assertEquals(RefreshOutcome.Success, survivorOutcome)
        assertEquals(1, h.api.calls.size)
    }
}

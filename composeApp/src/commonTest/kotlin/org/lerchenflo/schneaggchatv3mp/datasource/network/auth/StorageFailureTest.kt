package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.AuthTestHarness
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.RefreshResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.advance
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** D5, cases 27-30, 52, 55. */
class StorageFailureTest {

    @Test
    fun writeFailureIsRetryableKeepsTheOldPairAndDoesNotGrowTheBackoff() = runTest {
        val h = AuthTestHarness(this)
        val old = h.storeSession(h.pair())
        h.manager.ensureSession()
        h.store.throwOnWrite = true
        h.api.enqueueSuccess(h.pair())

        val outcome = assertIs<RefreshOutcome.Retryable>(h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(2.seconds, outcome.retryAfter)
        assertEquals(old, h.manager.currentTokens(), "the mirror keeps the pair the server still accepts via replay")
        assertEquals(old, h.store.stored)
        assertEquals(0, h.backoff.attempt, "no backoff growth: a long wait would only delay the recovery")
        assertTrue(h.sink.active.size == 1, "the dropped pair was never announced")

        // Storage recovers; the scheduled retry presents the OLD token -> server replay -> persisted.
        h.store.throwOnWrite = false
        val fresh = h.pair()
        h.api.enqueueSuccess(fresh)
        advance(2.seconds)

        assertEquals(2, h.api.calls.size)
        assertEquals(old.refreshToken, h.api.calls[1].refreshToken)
        assertEquals(fresh, h.store.stored)
    }

    @Test
    fun secondConsecutiveLossIsWhatInvalidates() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        h.store.throwOnWrite = true
        h.api.enqueueSuccess(h.pair())            // rotation 1, response lost client-side
        h.api.enqueueSuccess(h.pair())            // replay recovery, lost again
        h.api.enqueueError(NetworkingError.Unauthorized())   // server has moved on

        assertIs<RefreshOutcome.Retryable>(h.manager.refresh(RefreshReason.Reactive401))
        advance(2.seconds)
        assertEquals(2, h.api.calls.size)
        assertIs<AuthSessionState.Degraded>(h.manager.state.value)
        advance(2.seconds)
        assertEquals(3, h.api.calls.size)

        assertEquals(AuthSessionState.Invalidated(InvalidationReason.RejectedByServer), h.manager.state.value)
        assertEquals(listOf(InvalidationReason.RejectedByServer), h.sink.invalidations)
    }

    @Test
    fun readFailureIsNotCachedAsNoSession() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.store.throwOnRead = true

        assertEquals(SessionCheck.NoSession, h.manager.ensureSession())
        assertEquals(1, h.store.reads)
        assertEquals(0, h.store.clears, "a read failure must never clear storage")

        h.store.throwOnRead = false
        assertIs<SessionCheck.Active>(h.manager.ensureSession())
        assertEquals(2, h.store.reads)
    }

    @Test
    fun readFailureOnRefreshIsRetryableNotInvalidated() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.store.throwOnRead = true

        val outcome = assertIs<RefreshOutcome.Retryable>(h.manager.refresh(RefreshReason.Reactive401))
        assertIs<NetworkingError.Unknown>(outcome.error)
        assertTrue(h.api.calls.isEmpty())
        assertEquals(0, h.store.clears)
        assertTrue(h.sink.invalidations.isEmpty())
    }

    @Test
    fun emptyStoreIsNoSessionAndSilent() = runTest {
        val h = AuthTestHarness(this)

        assertEquals(SessionCheck.NoSession, h.manager.ensureSession())
        val outcome = assertIs<RefreshOutcome.Invalidated>(h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(InvalidationReason.NoSession, outcome.reason)
        assertTrue(h.sink.invalidations.isEmpty())
        assertTrue(h.api.calls.isEmpty())
    }

    @Test
    fun loginDuringAnInFlightRefreshWins() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        val gate = CompletableDeferred<RefreshResult>()
        h.api.enqueueGate(gate)

        var outcome: RefreshOutcome? = null
        launch { outcome = h.manager.refresh(RefreshReason.Reactive401) }
        runCurrent()

        val loginPair = h.pair()
        h.manager.onLoggedIn(loginPair)

        gate.complete(NetworkResult.Success(h.pair()))
        runCurrent()

        assertEquals(loginPair, h.store.stored, "the generation counter, not wall-clock order, decides")
        assertEquals(loginPair, h.manager.currentTokens())
        assertEquals(1, h.store.writes, "the superseded refresh never wrote")
        assertEquals(RefreshOutcome.Success, outcome, "the waiter is told to re-read, and finds the newer pair")
        assertEquals(listOf(loginPair), h.sink.active.drop(1))
    }

    @Test
    fun loginWriteFailurePropagatesAndChangesNothing() = runTest {
        val h = AuthTestHarness(this)
        val old = h.storeSession(h.pair())
        h.manager.ensureSession()
        h.store.throwOnWrite = true

        assertFailsWith<IllegalStateException> { h.manager.onLoggedIn(h.pair()) }

        assertEquals(old, h.manager.currentTokens())
        assertEquals(old, h.store.stored)
        assertEquals(1, h.sink.active.size)
    }

    @Test
    fun clearFailureDuringInvalidationIsLoggedNotThrown() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        h.store.throwOnClear = true
        h.api.enqueueError(NetworkingError.Unauthorized())

        assertIs<RefreshOutcome.Invalidated>(h.manager.refresh(RefreshReason.Reactive401))

        assertNull(h.manager.currentTokens(), "in-memory session is gone even if the keystore delete failed")
        assertEquals(listOf(InvalidationReason.RejectedByServer), h.sink.invalidations)
        assertTrue(h.log.lines.any { "storage clear failed" in it })
    }
}

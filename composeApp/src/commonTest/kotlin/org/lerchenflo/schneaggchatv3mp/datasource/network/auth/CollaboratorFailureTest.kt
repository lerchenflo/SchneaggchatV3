package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.AuthTestHarness
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.advance
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * A throwing collaborator never breaks the state machine and never escapes into the app scope
 * (which has no exception handler: an escaped throw would take the process down on Android).
 * `runTest` fails on any uncaught exception in `backgroundScope`, so these tests also prove that
 * the manager's own coroutines stay clean.
 */
class CollaboratorFailureTest {

    @Test
    fun aThrowingLogDoesNotFailTheRefreshOrItsWaiters() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        h.log.failing = true
        val fresh = h.pair()
        h.api.enqueueSuccess(fresh)

        assertEquals(RefreshOutcome.Success, h.manager.refresh(RefreshReason.Reactive401))

        assertEquals(fresh, h.store.stored)
        assertEquals(fresh, h.manager.currentTokens())
        assertEquals(listOf(h.store.stored), h.sink.active.drop(1))
    }

    @Test
    fun aThrowingLogDoesNotKillTheSchedulerOrTheTriggers() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        h.log.failing = true

        assertIs<RefreshOutcome.Retryable>(h.manager.refresh(RefreshReason.Reactive401))   // offline fallback
        advance(2.seconds)
        assertEquals(2, h.api.calls.size, "the scheduled retry ran although reporting it threw")

        h.online.value = false
        runCurrent()
        h.online.value = true
        runCurrent()
        assertEquals(3, h.api.calls.size, "the connectivity trigger survived its own log failure")

        advance(6.seconds)                                   // past the reset-trigger debounce; scheduled retries keep running meanwhile
        val beforeResume = h.api.calls.size
        h.resumed.tryEmit(Unit)
        runCurrent()
        assertEquals(beforeResume + 1, h.api.calls.size, "the app-resumed trigger survived its own log failure")
    }

    @Test
    fun aThrowingSinkDegradesTheOutcomeInsteadOfFailingTheWaiters() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        h.sink.failOnSessionActive = true
        val fresh = h.pair()
        h.api.enqueueSuccess(fresh)

        val outcome = assertIs<RefreshOutcome.Retryable>(h.manager.refresh(RefreshReason.Reactive401))

        assertIs<NetworkingError.Unknown>(outcome.error)
        assertEquals(fresh, h.store.stored, "the pair was persisted before the sink threw")
        assertEquals(fresh, h.manager.currentTokens(), "and the next request uses it")
        assertTrue(h.log.lines.any { "commit failed" in it })
        assertIs<AuthSessionState.Active>(h.manager.state.value)
    }

    @Test
    fun aThrowingSinkOnHydrationDoesNotLeakOutOfCurrentTokens() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = 1.seconds))
        h.sink.failOnSessionActive = true
        h.api.enqueueSuccess(h.pair())

        // The proactive refresh launched from the request path commits with a throwing sink; the
        // launch is guarded, so the test scope sees no exception and the caller got its tokens.
        val tokens = h.manager.currentTokens()
        runCurrent()

        assertEquals(1, h.api.calls.size)
        assertEquals(h.store.stored, h.manager.currentTokens())
        assertTrue(tokens != null)
    }
}

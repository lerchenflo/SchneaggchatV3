package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.AuthTestHarness
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.RefreshResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.TestJwt
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.advance
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** R11, D7, D11, cases 7, 8, 34-36, 38-43, 50. */
class SessionLifecycleTest {

    @Test
    fun coldStartWithValidTokensHydratesWithoutNetwork() = runTest {
        val h = AuthTestHarness(this)
        val stored = h.storeSession(h.pair())

        val check = assertIs<SessionCheck.Active>(h.manager.ensureSession())

        assertEquals(TestJwt.DEFAULT_SUBJECT, check.userId)
        assertEquals(listOf(stored), h.sink.active)
        assertTrue(h.api.calls.isEmpty())
        assertEquals(AuthSessionState.Active(TestJwt.DEFAULT_SUBJECT), h.manager.state.value)
        runCurrent()
        assertTrue(h.api.calls.isEmpty(), "still nothing after the scheduler had a chance to run")
    }

    @Test
    fun coldStartWithStaleAccessTokenSchedulesExactlyOneRefresh() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = (-1).minutes))
        val gate = CompletableDeferred<RefreshResult>()
        h.api.enqueueGate(gate)

        assertIs<SessionCheck.Active>(h.manager.ensureSession())
        // The first authenticated request may 401 at the same time - single-flight makes it one.
        launch { h.manager.refresh(RefreshReason.Reactive401) }
        runCurrent()

        assertEquals(1, h.api.calls.size)
        gate.complete(NetworkResult.Success(h.pair()))
        runCurrent()
        assertIs<AuthSessionState.Active>(h.manager.state.value)
    }

    @Test
    fun coldStartOfflineWaitsForConnectivity() = runTest {
        val h = AuthTestHarness(this)
        h.online.value = false
        h.storeSession(h.pair(accessValidFor = (-1).minutes))
        h.api.enqueueSuccess(h.pair())

        assertIs<SessionCheck.Active>(h.manager.ensureSession())
        runCurrent()
        assertTrue(h.api.calls.isEmpty(), "offline: the app opens with the cached session, no attempt")

        h.online.value = true
        runCurrent()
        assertEquals(1, h.api.calls.size, "one refresh the moment connectivity returns")
    }

    @Test
    fun logoutDuringAnInFlightRefreshDiscardsTheResult() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        val gate = CompletableDeferred<RefreshResult>()
        h.api.enqueueGate(gate)

        var outcome: RefreshOutcome? = null
        launch { outcome = h.manager.refresh(RefreshReason.Reactive401) }
        runCurrent()

        h.manager.clearSession()
        gate.complete(NetworkResult.Success(h.pair()))
        runCurrent()

        assertNull(h.store.stored, "the logout is not undone by the late refresh")
        assertNull(h.manager.currentTokens())
        assertEquals(0, h.store.writes)
        assertEquals(RefreshOutcome.Invalidated(InvalidationReason.LoggedOut), outcome)
        assertTrue(h.sink.invalidations.isEmpty(), "a user logout is not a server invalidation")
    }

    @Test
    fun serverInvalidationIsReportedExactlyOnceToConcurrentObservers() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        val gate = CompletableDeferred<RefreshResult>()
        h.api.enqueueGate(gate)

        val outcomes = mutableListOf<RefreshOutcome>()
        repeat(3) { launch { outcomes += h.manager.refresh(RefreshReason.Reactive401) } }
        runCurrent()
        gate.complete(NetworkResult.Error(NetworkingError.Unauthorized()))
        runCurrent()

        assertEquals(3, outcomes.size)
        assertTrue(outcomes.all { it == RefreshOutcome.Invalidated(InvalidationReason.RejectedByServer) })
        assertEquals(listOf(InvalidationReason.RejectedByServer), h.sink.invalidations, "one toast, one wipe, one navigation")
        assertNull(h.store.stored)

        // Anyone observing the same 401 afterwards finds no session and stays silent.
        assertEquals(RefreshOutcome.Invalidated(InvalidationReason.NoSession), h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(1, h.sink.invalidations.size)
    }

    @Test
    fun clearSessionResetsEverythingForTheNextUser() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(sub = "user-1"))
        h.manager.ensureSession()
        h.manager.refresh(RefreshReason.Reactive401)
        advance(2.seconds)
        assertEquals(2, h.backoff.attempt)

        h.manager.clearSession()
        assertNull(h.manager.currentTokens())
        assertEquals(0, h.backoff.attempt)
        assertEquals(AuthSessionState.Invalidated(InvalidationReason.LoggedOut), h.manager.state.value)
        advance(10.minutes)
        assertEquals(2, h.api.calls.size, "no timer survives the logout")

        val next = h.pair(sub = "user-2")
        h.manager.onLoggedIn(next)
        assertEquals(AuthSessionState.Active("user-2"), h.manager.state.value)
        assertEquals(next, h.manager.currentTokens())
        assertEquals(next, h.store.stored)

        advance(1.seconds)
        val outcome = assertIs<RefreshOutcome.Retryable>(h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(2.seconds, outcome.retryAfter, "backoff starts from zero for the new session")
    }

    @Test
    fun clearSessionIsIdempotent() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()

        h.manager.clearSession()
        h.manager.clearSession()

        assertEquals(2, h.store.clears)
        assertNull(h.manager.currentTokens())
        assertTrue(h.sink.invalidations.isEmpty())
    }

    @Test
    fun clearSessionPropagatesStorageErrorsAfterForgettingTheSession() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        h.store.throwOnClear = true

        assertFailsWith<IllegalStateException> { h.manager.clearSession() }

        assertNull(h.manager.currentTokens(), "in-memory session is gone before the throw reaches the caller")
        assertEquals(AuthSessionState.Invalidated(InvalidationReason.LoggedOut), h.manager.state.value)
    }

    @Test
    fun deleteAppDataPathRehydratesFromStorageAgain() = runTest {
        val h = AuthTestHarness(this)
        val stored = h.storeSession(h.pair())

        assertIs<SessionCheck.Active>(h.manager.ensureSession())
        // MiscSettings "delete all app data" wipes SessionCache but keeps the tokens and routes
        // through AutoLoginCredChecker again -> a second ensureSession must hydrate again.
        assertIs<SessionCheck.Active>(h.manager.ensureSession())

        assertEquals(listOf(stored, stored), h.sink.active)
        assertTrue(h.api.calls.isEmpty())
    }

    @Test
    fun expiryWhileActiveInvalidatesWithNotification() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = 15.minutes, refreshValidFor = 20.minutes))
        h.manager.ensureSession()
        // Proactive refresh at 13 min keeps failing (offline all along), the refresh token expires at 20 min.
        h.online.value = false
        runCurrent()
        advance(21.minutes)

        val outcome = assertIs<RefreshOutcome.Invalidated>(h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(InvalidationReason.RefreshTokenExpired, outcome.reason)
        // Was active in this process -> the user is told.
        assertEquals(listOf(InvalidationReason.RefreshTokenExpired), h.sink.invalidations)
        assertNull(h.store.stored)
    }

    @Test
    fun stateWalksThroughTheExpectedTransitions() = runTest {
        val h = AuthTestHarness(this)
        assertEquals(AuthSessionState.Unknown, h.manager.state.value)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        assertIs<AuthSessionState.Active>(h.manager.state.value)

        val gate = CompletableDeferred<RefreshResult>()
        h.api.enqueueGate(gate)
        launch { h.manager.refresh(RefreshReason.Reactive401) }
        runCurrent()
        assertEquals(AuthSessionState.Refreshing, h.manager.state.value)

        gate.complete(NetworkResult.Error(NetworkingError.ServerError()))
        runCurrent()
        assertIs<AuthSessionState.Degraded>(h.manager.state.value)

        h.api.enqueueSuccess(h.pair())
        advance(2.seconds)
        assertIs<AuthSessionState.Active>(h.manager.state.value)
    }
}

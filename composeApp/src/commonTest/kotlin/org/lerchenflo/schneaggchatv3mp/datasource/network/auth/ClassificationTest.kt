package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.TokenPair
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.AuthTestHarness
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.advance
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** R6, D3, cases 14, 15, 16, 21, 24, 49, 51. */
class ClassificationTest {

    private suspend fun TestScope.activeHarness(): AuthTestHarness {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        assertIs<SessionCheck.Active>(h.manager.ensureSession())
        return h
    }

    @Test
    fun retryableErrorsKeepTheSession() = runTest {
        val retryable = listOf(
            NetworkingError.NoInternetConnection,
            NetworkingError.NetworkTimeout(),
            NetworkingError.ServerError(message = "502 during deploy"),
            NetworkingError.TooManyRequests(),
        )
        for (error in retryable) {
            val h = activeHarness()
            h.api.enqueueError(error)

            val outcome = assertIs<RefreshOutcome.Retryable>(h.manager.refresh(RefreshReason.Reactive401), "$error")
            assertEquals(error, outcome.error)
            assertNotNull(h.store.stored, "$error must keep the credentials")
            assertTrue(h.sink.invalidations.isEmpty(), "$error must not invalidate")
            assertIs<AuthSessionState.Degraded>(h.manager.state.value)
        }
    }

    @Test
    fun brokenErrorsKeepTheSessionAndDoNotRetryForever() = runTest {
        val broken = listOf(
            NetworkingError.BadRequest(),
            NetworkingError.Forbidden(message = "blocked by gateway"),
            NetworkingError.NotFound(message = "wrong server url"),
            NetworkingError.Conflict(),
            NetworkingError.PayloadTooLarge(),
            NetworkingError.SerializationError(message = "captive portal html"),
            NetworkingError.Unknown(message = "unmapped"),
        )
        for (error in broken) {
            val h = activeHarness()
            h.api.enqueueError(error)

            val outcome = assertIs<RefreshOutcome.Broken>(h.manager.refresh(RefreshReason.Reactive401), "$error")
            assertEquals(error, outcome.error)
            assertNotNull(h.store.stored, "$error must keep the credentials")
            assertTrue(h.sink.invalidations.isEmpty())
            val degraded = assertIs<AuthSessionState.Degraded>(h.manager.state.value)
            assertEquals(h.clock.now() + 2.minutes, degraded.nextAttemptAt, "$error sits at the backoff cap")
        }
    }

    @Test
    fun onlyAnUnauthorizedRefreshInvalidates() = runTest {
        val h = activeHarness()
        h.api.enqueueError(NetworkingError.Unauthorized())

        val outcome = assertIs<RefreshOutcome.Invalidated>(h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(InvalidationReason.RejectedByServer, outcome.reason)
        assertNull(h.store.stored, "credentials are gone")
        assertNull(h.manager.currentTokens())
        assertEquals(listOf(InvalidationReason.RejectedByServer), h.sink.invalidations)
        assertEquals(AuthSessionState.Invalidated(InvalidationReason.RejectedByServer), h.manager.state.value)
    }

    @Test
    fun threeConsecutiveBrokenOutcomesRaiseServerUnreachableOnce() = runTest {
        val h = activeHarness()
        repeat(4) { h.api.enqueueError(NetworkingError.NotFound()) }

        assertIs<RefreshOutcome.Broken>(h.manager.refresh(RefreshReason.Reactive401))
        assertTrue(h.sink.unreachable.isEmpty())

        advance(2.minutes)
        assertEquals(2, h.api.calls.size)
        assertTrue(h.sink.unreachable.isEmpty())

        advance(2.minutes)
        assertEquals(3, h.api.calls.size)
        assertEquals(1, h.sink.unreachable.size, "surfaced after the third permanent failure")

        advance(2.minutes)
        assertEquals(4, h.api.calls.size)
        assertEquals(1, h.sink.unreachable.size, "not repeated within the same streak")
        assertNotNull(h.store.stored, "Broken never clears credentials")
        assertTrue(h.sink.invalidations.isEmpty())
    }

    @Test
    fun brokenStreakIsResetByARetryableOutcome() = runTest {
        val h = activeHarness()
        h.api.enqueueError(NetworkingError.NotFound())
        h.api.enqueueError(NetworkingError.NotFound())
        h.api.enqueueError(NetworkingError.NoInternetConnection)
        repeat(3) { h.api.enqueueError(NetworkingError.NotFound()) }

        h.manager.refresh(RefreshReason.Reactive401)   // broken 1
        advance(2.minutes)                              // broken 2
        advance(2.minutes)                              // retryable -> streak reset; backoff attempt is 3 -> 8 s
        assertEquals(3, h.api.calls.size)
        assertTrue(h.sink.unreachable.isEmpty())
        advance(8.seconds)                              // broken 1'
        advance(2.minutes)                              // broken 2'
        assertEquals(5, h.api.calls.size)
        assertTrue(h.sink.unreachable.isEmpty(), "the streak restarted after the retryable outcome")
        advance(2.minutes)                              // broken 3'
        assertEquals(6, h.api.calls.size)
        assertEquals(1, h.sink.unreachable.size)
    }

    @Test
    fun apiThrowingIsBroken() = runTest {
        val h = activeHarness()
        h.api.enqueueThrow(IllegalStateException("boom"))

        val outcome = assertIs<RefreshOutcome.Broken>(h.manager.refresh(RefreshReason.Reactive401))
        assertIs<NetworkingError.Unknown>(outcome.error)
        assertNotNull(h.store.stored)
    }

    @Test
    fun successWithUnusablePairIsBrokenAndNothingIsPersisted() = runTest {
        val h = activeHarness()
        val before = h.store.stored
        h.api.enqueueSuccess(TokenPair(accessToken = "", refreshToken = ""))

        val outcome = assertIs<RefreshOutcome.Broken>(h.manager.refresh(RefreshReason.Reactive401))
        assertIs<NetworkingError.SerializationError>(outcome.error)
        assertEquals(before, h.store.stored, "garbage is never persisted")
        assertEquals(0, h.store.writes)
    }

    @Test
    fun successWithUnparseableTokensIsBroken() = runTest {
        val h = activeHarness()
        h.api.enqueueSuccess(TokenPair(accessToken = "not.a.jwt", refreshToken = "not.a.jwt"))

        assertIs<RefreshOutcome.Broken>(h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(0, h.store.writes)
    }
}

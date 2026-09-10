package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.TokenPair
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.AuthTestHarness
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.TestJwt
import org.lerchenflo.schneaggchatv3mp.utilities.JwtUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/** Cases 20, 25, 26, 47, 48. */
class TokenIntegrityTest {

    @Test
    fun testTokensParseLikeRealOnes() = runTest {
        val h = AuthTestHarness(this)
        val exp = h.nowMillis() + 60_000
        val token = TestJwt.token(sub = "abc", expEpochMillis = exp)

        assertEquals("abc", JwtUtils.getUserIdFromToken(token))
        assertEquals(exp / 1000 * 1000, JwtUtils.expiresAtEpochMillis(token))
        assertTrue(JwtUtils.isValidAt(token, h.nowMillis()))
        assertFalse(JwtUtils.isValidAt(token, exp))
    }

    @Test
    fun bothExpiredIsExpiredWithoutNetworkAndSilently() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = (-40).days, refreshValidFor = (-10).days))

        assertEquals(SessionCheck.Expired, h.manager.ensureSession())

        assertTrue(h.api.calls.isEmpty(), "no request spent on a token the client can see is dead")
        assertNull(h.store.stored, "cleared so nothing can retry against it")
        // Never active in this process: loadSavedLoginConfig shows its own message.
        assertTrue(h.sink.invalidations.isEmpty())
        assertEquals(AuthSessionState.Invalidated(InvalidationReason.RefreshTokenExpired), h.manager.state.value)
    }

    @Test
    fun malformedRefreshTokenInStorageIsExpiredNotACrash() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(TokenPair(accessToken = h.accessToken(), refreshToken = TestJwt.malformed()))

        assertEquals(SessionCheck.Expired, h.manager.ensureSession())
        assertNull(h.store.stored)
        assertEquals(AuthSessionState.Invalidated(InvalidationReason.MalformedToken), h.manager.state.value)
        assertTrue(h.sink.invalidations.isEmpty())
    }

    @Test
    fun malformedAccessTokenWithAValidRefreshTokenIsSimplyRefreshed() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(TokenPair(accessToken = TestJwt.malformed(), refreshToken = h.refreshToken()))
        val fresh = h.pair()
        h.api.enqueueSuccess(fresh)

        assertIs<SessionCheck.Active>(h.manager.ensureSession())
        runCurrent()

        assertEquals(1, h.api.calls.size)
        assertEquals(fresh, h.store.stored)
    }

    @Test
    fun missingExpCountsAsExpired() = runTest {
        val h = AuthTestHarness(this)
        val noExp = TestJwt.token(expEpochMillis = null)
        assertFalse(JwtUtils.isValidAt(noExp, h.nowMillis()))
        assertNull(JwtUtils.expiresAtEpochMillis(noExp))

        h.storeSession(TokenPair(accessToken = h.accessToken(), refreshToken = TestJwt.token(expEpochMillis = null, type = "refresh_token")))
        assertEquals(SessionCheck.Expired, h.manager.ensureSession())
    }

    @Test
    fun unchangedRefreshTokenFromReplayRecoveryIsAccepted() = runTest {
        val h = AuthTestHarness(this)
        val stored = h.storeSession(h.pair())
        h.manager.ensureSession()
        val replayed = TokenPair(accessToken = h.accessToken(), refreshToken = stored.refreshToken)
        h.api.enqueueSuccess(replayed)

        assertEquals(RefreshOutcome.Success, h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(replayed, h.store.stored)
        assertEquals(stored.refreshToken, h.manager.currentTokens()?.refreshToken)
    }

    @Test
    fun tokensForAnotherUserInvalidateInsteadOfSwitchingAccounts() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(sub = "user-1"))
        h.manager.ensureSession()
        h.api.enqueueSuccess(h.pair(sub = "user-2"))

        val outcome = assertIs<RefreshOutcome.Invalidated>(h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(InvalidationReason.ForeignSubject, outcome.reason)
        assertNull(h.store.stored)
        assertEquals(0, h.store.writes)
        assertEquals(listOf(InvalidationReason.ForeignSubject), h.sink.invalidations)
    }

    @Test
    fun refreshWithALocallyExpiredRefreshTokenInvalidatesWithoutNetwork() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = (-1).minutes, refreshValidFor = (-1).minutes))

        val outcome = assertIs<RefreshOutcome.Invalidated>(h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(InvalidationReason.RefreshTokenExpired, outcome.reason)
        assertTrue(h.api.calls.isEmpty())
    }
}

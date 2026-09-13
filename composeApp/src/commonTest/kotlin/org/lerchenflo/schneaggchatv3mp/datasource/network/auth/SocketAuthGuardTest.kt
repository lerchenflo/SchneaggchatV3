package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.AuthTestHarness
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.advance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** R10, D4, cases 22, 37. */
class SocketAuthGuardTest {

    @Test
    fun firstFailureWithALocallyValidTokenIsNotARefresh() {
        val guard = SocketAuthGuard()
        assertFalse(guard.onHandshakeFailed(accessTokenExpiredLocally = false))
        assertEquals(1, guard.consecutiveFailures)
    }

    @Test
    fun secondConsecutiveFailureForcesARefreshDespiteTheLocalClock() {
        val guard = SocketAuthGuard()
        guard.onHandshakeFailed(accessTokenExpiredLocally = false)
        // Clock skew / secret rotation: the server's opinion wins (D4).
        assertTrue(guard.onHandshakeFailed(accessTokenExpiredLocally = false))
    }

    @Test
    fun aLocallyExpiredTokenRefreshesOnTheFirstFailure() {
        val guard = SocketAuthGuard()
        assertTrue(guard.onHandshakeFailed(accessTokenExpiredLocally = true))
    }

    @Test
    fun successResetsTheCount() {
        val guard = SocketAuthGuard()
        guard.onHandshakeFailed(accessTokenExpiredLocally = false)
        guard.onHandshakeSucceeded()
        assertEquals(0, guard.consecutiveFailures)
        assertFalse(guard.onHandshakeFailed(accessTokenExpiredLocally = false))
    }

    @Test
    fun aReconnectStormSpendsAtMostOneNetworkRefreshPerBackoffWindow() = runTest {
        // Desktop minimised for hours, server reaped the socket, network is down: the socket
        // loop retries every few seconds and asks for a refresh on every failure after the first.
        val h = AuthTestHarness(this)
        h.storeSession(h.pair())
        h.manager.ensureSession()
        val guard = SocketAuthGuard()

        var refreshAsks = 0
        repeat(20) {
            advance(3.seconds)
            if (guard.onHandshakeFailed(accessTokenExpiredLocally = false)) {
                refreshAsks++
                h.manager.refresh(RefreshReason.SocketHandshake)
            }
        }

        assertEquals(19, refreshAsks, "the guard asked on every failure after the first")
        // 60 s of storm; backoff windows 2, 4, 8, 16, 32 s -> at most one network attempt each,
        // plus the manager's own scheduled retries which are the same windows.
        assertTrue(h.api.calls.size <= 7, "network refreshes: ${h.api.calls.size}")
        assertTrue(h.api.calls.size >= 2, "the guard's asks did reach the network")
        assertTrue(h.sink.invalidations.isEmpty())
    }
}

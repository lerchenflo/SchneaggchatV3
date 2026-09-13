package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

/**
 * Decides whether a failed WebSocket handshake should be answered with a token refresh
 * (AUTH_SESSION_REBUILD_PLAN R10). A handshake fails for plenty of reasons that have nothing to
 * do with the token (server down, no route, rate limited), so the first failure is never a
 * refresh - unless the client itself can see the access token is expired. From the second
 * consecutive failure on, a refresh is requested regardless of what the local clock says, so a
 * token the server rejects (clock skew, secret rotation, revocation) is healed without waiting
 * for an unrelated HTTP 401.
 *
 * Pure: no I/O, not thread-safe - owned by the single reconnect loop in `SocketConnectionManager`.
 */
class SocketAuthGuard(
    private val failureThreshold: Int = 2,
) {
    var consecutiveFailures: Int = 0
        private set

    fun onHandshakeSucceeded() {
        consecutiveFailures = 0
    }

    /**
     * Records a failed handshake and returns whether the caller should ask `AuthSessionManager`
     * for a refresh before the next attempt.
     */
    fun onHandshakeFailed(accessTokenExpiredLocally: Boolean): Boolean {
        consecutiveFailures++
        return accessTokenExpiredLocally || consecutiveFailures >= failureThreshold
    }

    fun reset() {
        consecutiveFailures = 0
    }
}

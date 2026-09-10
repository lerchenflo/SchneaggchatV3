@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.TokenPair
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Durable token storage. The only writable copy of the session (AUTH_SESSION_REBUILD_PLAN R1);
 * `AuthSessionManager` is its only writer.
 */
interface AuthSessionStore {
    /** The stored pair, or null when no session is stored (blank refresh token). May throw. */
    suspend fun read(): TokenPair?
    suspend fun write(tokens: TokenPair)
    suspend fun clear()
}

/** `POST /auth/refresh`. Must never go through the authenticated client (it would recurse). */
interface AuthRefreshApi {
    suspend fun refresh(refreshToken: String): NetworkResult<TokenPair, NetworkingError>
}

/**
 * Where the manager reports session transitions. The real implementation updates `SessionCache`
 * and raises app-wide actions/errors; tests record the calls.
 */
interface AuthEventSink {
    /** A usable pair is in place (hydrated from storage, logged in, or refreshed). Derived views update from it. */
    fun onSessionActive(tokens: TokenPair)

    /** The session died while it was active in this process. Fired at most once per session. */
    fun onSessionInvalidated(reason: InvalidationReason)

    /** Several consecutive refreshes failed permanently (wrong server URL, blocking proxy). Credentials are kept. */
    fun onServerUnreachable(error: NetworkingError)
}

/** Time source for token expiry and backoff so tests can drive virtual time. */
fun interface AuthClock {
    fun now(): Instant
}

/**
 * Logging seam so every outcome can be asserted in tests and lands in `LoggingRepository` in the
 * app. [debug] is for the high-frequency no-op paths (gate short-circuits) that must not flood
 * the persisted log; the real implementation only prints those.
 */
interface AuthLog {
    suspend fun debug(message: String)
    suspend fun info(message: String)
    suspend fun warn(message: String)
    suspend fun error(message: String)
}

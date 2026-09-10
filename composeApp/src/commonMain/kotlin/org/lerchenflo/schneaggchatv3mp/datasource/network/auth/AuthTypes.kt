@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Outcome of one refresh attempt. Callers react very differently to each: `Retryable` and
 * `Broken` keep the session, `Invalidated` means it is gone (storage already cleared).
 */
sealed interface RefreshOutcome {
    /**
     * A usable token pair is in place - either freshly persisted, or already there because
     * another caller rotated first / a newer login superseded this attempt. Re-read the tokens.
     */
    data object Success : RefreshOutcome

    /**
     * The refresh token may still be good; try again after [retryAfter]. Offline, timeout, 5xx,
     * 429, storage write failure.
     */
    data class Retryable(val error: NetworkingError, val retryAfter: Duration) : RefreshOutcome

    /**
     * Permanently failing, but not proof the session is dead: 400, 403, 404, serialization
     * failure (captive portal, empty body), unknown errors. Retried at the maximum backoff and
     * surfaced to the user after a few in a row; never clears credentials.
     */
    data class Broken(val error: NetworkingError) : RefreshOutcome

    /** The session is dead. Storage and the in-memory mirror have been cleared. */
    data class Invalidated(val reason: InvalidationReason) : RefreshOutcome
}

enum class InvalidationReason {
    /** Nothing stored to refresh. */
    NoSession,
    /** The stored refresh token's own `exp` is in the past. */
    RefreshTokenExpired,
    /** The stored refresh token does not parse (truncated write, storage corruption). */
    MalformedToken,
    /** `/auth/refresh` answered 401. */
    RejectedByServer,
    /** The server returned a pair for a different user (`sub` mismatch). */
    ForeignSubject,
    /** Logout cleared the session while a refresh was in flight. */
    LoggedOut,
}

/**
 * Why a refresh was requested. Purely for logging and for the gates in `AuthSessionManager`:
 * optional refreshes are skipped while offline and while a just-issued token is fresh, reactive
 * ones are attempted regardless (a 401 proves the network works).
 */
enum class RefreshReason(
    /** Attempted even while the app-wide online flag is false. */
    val attemptedOffline: Boolean,
    /** Skipped (answered `Success` from cache) within the post-success floor. */
    val honoursPostSuccessFloor: Boolean,
) {
    /** A normal request came back 401 (Ktor Auth plugin). */
    Reactive401(attemptedOffline = true, honoursPostSuccessFloor = false),
    /** The WebSocket handshake failed repeatedly or with a locally expired token. */
    SocketHandshake(attemptedOffline = false, honoursPostSuccessFloor = true),
    /** The access token is about to expire. */
    Proactive(attemptedOffline = false, honoursPostSuccessFloor = true),
    /** The manager's own retry timer after a failed attempt. */
    Scheduled(attemptedOffline = false, honoursPostSuccessFloor = true),
    /** Connectivity came back after being offline. */
    ConnectivityRestored(attemptedOffline = true, honoursPostSuccessFloor = false),
    /** The app returned to the foreground. */
    AppResumed(attemptedOffline = true, honoursPostSuccessFloor = false),
    /** Explicit user action (retry / pull-to-refresh). */
    Manual(attemptedOffline = true, honoursPostSuccessFloor = false),
}

/** Observable session state, for diagnostics and future UI (e.g. a "reconnecting" banner). */
sealed interface AuthSessionState {
    /** Nothing evaluated yet in this process. */
    data object Unknown : AuthSessionState
    data class Active(val userId: String) : AuthSessionState
    data object Refreshing : AuthSessionState
    /** The last refresh failed; the session is kept and retried at [nextAttemptAt]. */
    data class Degraded(val error: NetworkingError, val nextAttemptAt: Instant) : AuthSessionState
    data class Invalidated(val reason: InvalidationReason) : AuthSessionState
}

/** Result of [AuthSessionManager.ensureSession] - a classification of what is stored, never a network round trip. */
sealed interface SessionCheck {
    data class Active(val userId: String) : SessionCheck
    data object NoSession : SessionCheck
    /** A pair was stored but its refresh token is expired or unreadable; it has been cleared. */
    data object Expired : SessionCheck
}

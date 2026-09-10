@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.TokenPair
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.utilities.JwtUtils
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The client-side session state machine (AUTH_SESSION_REBUILD_PLAN section 4). Owns the only
 * in-memory copy of the token pair, is the only caller of [AuthRefreshApi], and is the only
 * writer of [AuthSessionStore].
 *
 * Guarantees:
 * - single source of truth: the mirror is written only together with storage, under [mutex], after
 *   the write succeeded (R1);
 * - single-flight: concurrent callers share one in-flight refresh; a caller presenting a token that
 *   is no longer the current one gets [RefreshOutcome.Success] without a network call (R3);
 * - cancellable waiting, uncancellable committing: the refresh runs as a [Deferred] in [scope]
 *   (never cancelled), waiters `await()` it plainly, the persist is `NonCancellable` (R4);
 * - bounded: one refresh never takes longer than [Config.refreshTimeout] (R5);
 * - backoff with reset triggers: success, successful authenticated request, connectivity restored,
 *   app resumed, fresh login, manual (R7); `retryAfterSeconds` from a 429 is honoured (D10);
 * - offline is a state: optional refreshes are not attempted while [onlineFlow] is false, and the
 *   moment it flips back exactly one refresh fires if one is needed (R12);
 * - generation counter: [onLoggedIn] and [clearSession] bump it, so an in-flight refresh started
 *   under an older generation can never overwrite a newer login or undo a logout (cases 7, 8, 30).
 *
 * Every collaborator is injected; nothing here touches Koin, `SessionCache` or wall-clock time.
 */
class AuthSessionManager(
    private val store: AuthSessionStore,
    private val api: AuthRefreshApi,
    private val sink: AuthEventSink,
    private val clock: AuthClock,
    private val log: AuthLog,
    private val scope: CoroutineScope,
    private val onlineFlow: StateFlow<Boolean>,
    appResumedEvents: Flow<Unit>,
    private val backoff: RefreshBackoff = RefreshBackoff(),
    private val config: Config = Config(),
) {

    data class Config(
        /** Own timeout for one `/auth/refresh`; well under the HTTP client's 30 s request timeout (R5). */
        val refreshTimeout: Duration = 12.seconds,
        /** Refresh proactively once the access token has less than this left (R8). */
        val proactiveThreshold: Duration = 2.minutes,
        /**
         * After a success, optional reasons (socket, proactive, scheduled) are answered from the
         * mirror for this long (case 23).
         */
        val postSuccessFloor: Duration = 60.seconds,
        /** Hard floor between any two attempts, whatever the reason. */
        val hardFloor: Duration = 1.seconds,
        /** A reset trigger (connectivity, foreground) may fire at most one refresh per this window (case 13). */
        val resetTriggerDebounce: Duration = 5.seconds,
        /** Consecutive `Broken` outcomes before [AuthEventSink.onServerUnreachable] fires (D3). */
        val brokenThreshold: Int = 3,
        /** Retry delay after a failed token write; short so replay recovery is not delayed (case 27). */
        val storageFailureRetry: Duration = 2.seconds,
    )

    private val mutex = Mutex()

    // ---- guarded by mutex (writes); volatile for lock-free reads on the request path ----
    @Volatile private var mirror: TokenPair? = null
    @Volatile private var mirrorLoaded = false
    @Volatile private var backoffArmed = false

    private var generation = 0
    private var inFlight: Deferred<RefreshOutcome>? = null
    private var schedulerJob: Job? = null
    private var cooldownUntil: Instant? = null
    private var lastError: NetworkingError? = null
    private var lastSuccessAt: Instant? = null
    private var lastResetTriggeredRefreshAt: Instant? = null
    private var consecutiveBroken = 0
    private var brokenNotified = false
    private var attemptCounter = 0
    /**
     * Access token the local clock already considered expired when the server issued it
     * (case 23); no proactive refresh is scheduled for it.
     */
    @Volatile private var proactiveDisabledFor: String? = null
    /** True once a session was active in this process; gates [AuthEventSink.onSessionInvalidated]. */
    private var wasActive = false

    private val _state = MutableStateFlow<AuthSessionState>(AuthSessionState.Unknown)
    val state: StateFlow<AuthSessionState> = _state.asStateFlow()

    init {
        scope.launch {
            // drop(1): the initial value is not a transition. Every later `true` follows a `false`
            // because StateFlow conflates equal values.
            onlineFlow.drop(1).filter { it }.collect { onResetTrigger(RefreshReason.ConnectivityRestored) }
        }
        scope.launch {
            appResumedEvents.collect { onResetTrigger(RefreshReason.AppResumed) }
        }
    }

    // ------------------------------------------------------------------ public surface

    /**
     * The current token pair - the in-memory mirror, loaded from storage on first use. Never
     * awaits the network; at most launches a proactive refresh when the access token is about to
     * expire (R8). This is what Ktor's `loadTokens` calls on every request.
     */
    suspend fun currentTokens(): TokenPair? {
        val tokens = loadMirror() ?: return null
        maybeLaunchProactive(tokens)
        return tokens
    }

    /**
     * Classifies the stored session without touching the network (cases 34-36): hydrates the
     * derived views when a valid refresh token is stored, clears an expired/unreadable one, and
     * schedules a refresh if the access token is already stale.
     */
    suspend fun ensureSession(): SessionCheck {
        loadMirror() ?: return SessionCheck.NoSession
        return mutex.withLock {
            val current = mirror ?: return@withLock SessionCheck.NoSession
            val now = clock.now()
            val refreshExp = JwtUtils.expiresAtEpochMillis(current.refreshToken)
            if (refreshExp == null) {
                invalidateLocked(InvalidationReason.MalformedToken)
                return@withLock SessionCheck.Expired
            }
            if (refreshExp <= now.toEpochMilliseconds()) {
                invalidateLocked(InvalidationReason.RefreshTokenExpired)
                return@withLock SessionCheck.Expired
            }
            val userId = JwtUtils.getUserIdFromToken(current.refreshToken)
            wasActive = true
            _state.value = AuthSessionState.Active(userId)
            sink.onSessionActive(current)
            if (accessTokenNeedsRefresh(current, now)) {
                scheduleLocked(now, RefreshReason.Scheduled)
            } else {
                scheduleProactiveLocked(current, now)
            }
            log.info("AuthSession: hydrated from storage (user=$userId)")
            SessionCheck.Active(userId)
        }
    }

    /**
     * Refreshes the session, or joins the refresh that is already running.
     *
     * @param presentedRefreshToken the refresh token the caller acted on, if any. When it is no
     * longer the current one, someone already rotated and the caller only needs to re-read.
     * @param presentedAccessToken the access token the failing request was sent with, if known.
     * Same rule: a stale access token means the mirror already holds a newer pair.
     */
    suspend fun refresh(
        reason: RefreshReason,
        presentedRefreshToken: String? = null,
        presentedAccessToken: String? = null,
    ): RefreshOutcome {
        loadMirror()
        val (deferred, immediate) = mutex.withLock<Pair<Deferred<RefreshOutcome>?, RefreshOutcome?>> {
            val current = mirror
            if (current == null && !mirrorLoaded) {
                // Storage could not be read (case 28): not a dead session, just unknown right now.
                val error = NetworkingError.Unknown(message = "token storage unreadable")
                return@withLock null to RefreshOutcome.Retryable(error, config.storageFailureRetry)
            }
            if (current == null || current.refreshToken.isBlank()) {
                return@withLock null to invalidateLocked(InvalidationReason.NoSession)
            }

            val presentedRefresh = presentedRefreshToken?.takeIf { it.isNotBlank() }
            val presentedAccess = presentedAccessToken?.takeIf { it.isNotBlank() }
            if ((presentedRefresh != null && presentedRefresh != current.refreshToken) ||
                (presentedAccess != null && presentedAccess != current.accessToken)
            ) {
                log.debug("AuthSession: refresh($reason) answered from mirror, presented token already rotated")
                return@withLock null to RefreshOutcome.Success
            }

            val now = clock.now()
            val refreshExp = JwtUtils.expiresAtEpochMillis(current.refreshToken)
            if (refreshExp == null) {
                return@withLock null to invalidateLocked(InvalidationReason.MalformedToken)
            }
            if (refreshExp <= now.toEpochMilliseconds()) {
                return@withLock null to invalidateLocked(InvalidationReason.RefreshTokenExpired)
            }

            inFlight?.takeIf { it.isActive }?.let { return@withLock it to null }

            // An explicit user action is one of the backoff reset triggers (R7): it is attempted
            // now, not held back by a cooldown from earlier automatic attempts.
            if (reason == RefreshReason.Manual) resetBackoffLocked()

            if (!reason.attemptedOffline && !onlineFlow.value) {
                log.debug("AuthSession: refresh($reason) skipped, offline")
                val skipped = RefreshOutcome.Retryable(NetworkingError.NoInternetConnection, Duration.ZERO)
                return@withLock null to skipped
            }

            cooldownUntil?.let { until ->
                if (now < until) {
                    val remaining = until - now
                    log.debug(
                        "AuthSession: refresh($reason) suppressed, backoff active ${remaining.inWholeSeconds} s more"
                    )
                    val suppressed = RefreshOutcome.Retryable(lastError ?: NetworkingError.Unknown(), remaining)
                    return@withLock null to suppressed
                }
            }

            lastSuccessAt?.let { success ->
                val sinceSuccess = now - success
                val floor = if (reason.honoursPostSuccessFloor) config.postSuccessFloor else config.hardFloor
                if (sinceSuccess < floor) {
                    log.debug(
                        "AuthSession: refresh($reason) answered from mirror, " +
                            "last success ${sinceSuccess.inWholeMilliseconds} ms ago"
                    )
                    return@withLock null to RefreshOutcome.Success
                }
            }

            val startedUnderGeneration = generation
            attemptCounter++
            val attempt = attemptCounter
            _state.value = AuthSessionState.Refreshing
            val job = scope.async { runRefresh(current, startedUnderGeneration, reason, attempt) }
            inFlight = job
            job to null
        }

        immediate?.let { return it }
        // Plain await: cancelling this waiter leaves the deferred (and its NonCancellable persist)
        // running in `scope` - a second waiter may still need the result (R4).
        return deferred!!.await()
    }

    /** Seeds the session after a password login or registration. Throws if the pair cannot be persisted (case 55). */
    suspend fun onLoggedIn(tokens: TokenPair) {
        require(tokens.refreshToken.isNotBlank()) { "cannot log in with a blank refresh token" }
        mutex.withLock {
            withContext(NonCancellable) { store.write(tokens) }
            generation++
            inFlight = null
            mirror = tokens
            mirrorLoaded = true
            val now = clock.now()
            resetBackoffLocked()
            lastSuccessAt = now
            lastResetTriggeredRefreshAt = null
            val userId = JwtUtils.getUserIdFromToken(tokens.refreshToken)
            wasActive = true
            _state.value = AuthSessionState.Active(userId)
            sink.onSessionActive(tokens)
            scheduleProactiveLocked(tokens, now)
            log.info("AuthSession: logged in (user=$userId)")
        }
    }

    /**
     * Forgets the session: mirror, durable credentials, backoff, timers. Bumps the generation so
     * an in-flight refresh cannot resurrect it (case 8). Silent - a user logout is not an
     * invalidation. Storage errors propagate after the in-memory state is already gone.
     */
    suspend fun clearSession() {
        mutex.withLock {
            generation++
            inFlight = null
            schedulerJob?.cancel()
            schedulerJob = null
            mirror = null
            mirrorLoaded = true
            resetBackoffLocked()
            lastSuccessAt = null
            lastResetTriggeredRefreshAt = null
            wasActive = false
            _state.value = AuthSessionState.Invalidated(InvalidationReason.LoggedOut)
            withContext(NonCancellable) { store.clear() }
        }
        log.info("AuthSession: cleared")
    }

    /**
     * Backoff reset trigger (R7): any successful authenticated request proves the network works.
     * Called on every response of the authenticated client, so it is a plain flag check unless a
     * backoff is actually armed.
     */
    fun onAuthenticatedRequestSucceeded() {
        if (!backoffArmed) return
        scope.launch {
            mutex.withLock {
                if (!backoffArmed) return@withLock
                resetBackoffLocked()
                log.info("AuthSession: backoff reset by a successful request")
            }
        }
    }

    // ------------------------------------------------------------------ internals

    private suspend fun loadMirror(): TokenPair? {
        mirror?.let { return it }
        if (mirrorLoaded) return null
        return mutex.withLock {
            mirror?.let { return@withLock it }
            if (mirrorLoaded) return@withLock null
            val read = try {
                store.read()
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                // Not cached as "no session": a transient keystore failure must not lock the user
                // out for the rest of the process lifetime (cases 28, 52).
                log.error("AuthSession: storage read failed: ${e.message}")
                return@withLock null
            }
            mirrorLoaded = true
            mirror = read?.takeIf { it.refreshToken.isNotBlank() }
            mirror
        }
    }

    private fun accessTokenNeedsRefresh(tokens: TokenPair, now: Instant): Boolean {
        val exp = JwtUtils.expiresAtEpochMillis(tokens.accessToken) ?: return true
        return exp - now.toEpochMilliseconds() <= config.proactiveThreshold.inWholeMilliseconds
    }

    private fun maybeLaunchProactive(tokens: TokenPair) {
        if (!onlineFlow.value) return
        if (tokens.accessToken == proactiveDisabledFor) return
        val now = clock.now()
        if (!accessTokenNeedsRefresh(tokens, now)) return
        // Unlocked peek; refresh() re-checks everything under the lock. This only avoids
        // launching a coroutine per request while a backoff window or the post-success floor is active.
        cooldownUntil?.let { if (now < it) return }
        lastSuccessAt?.let { if (now - it < config.postSuccessFloor) return }
        if (inFlight?.isActive == true) return
        scope.launch { refresh(RefreshReason.Proactive) }
    }

    private suspend fun runRefresh(
        current: TokenPair,
        startedUnderGeneration: Int,
        reason: RefreshReason,
        attempt: Int,
    ): RefreshOutcome {
        val result: NetworkResult<TokenPair, NetworkingError>? = try {
            withTimeoutOrNull(config.refreshTimeout) { api.refresh(current.refreshToken) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(NetworkingError.Unknown(message = e.message))
        }
        return mutex.withLock { commitLocked(current, startedUnderGeneration, reason, attempt, result) }
    }

    private suspend fun commitLocked(
        current: TokenPair,
        startedUnderGeneration: Int,
        reason: RefreshReason,
        attempt: Int,
        result: NetworkResult<TokenPair, NetworkingError>?,
    ): RefreshOutcome {
        if (startedUnderGeneration != generation) {
            // A login or logout happened while this refresh was in flight - its result must not
            // overwrite the newer session (case 7) or undo the logout (case 8).
            log.info("AuthSession: refresh($reason) #$attempt discarded, session generation changed")
            return if (mirror != null) {
                RefreshOutcome.Success
            } else {
                RefreshOutcome.Invalidated(InvalidationReason.LoggedOut)
            }
        }

        val now = clock.now()
        return when (result) {
            null -> {
                val timeout = NetworkingError.NetworkTimeout(message = "refresh exceeded ${config.refreshTimeout}")
                failRetryable(timeout, null, reason, attempt, now)
            }

            is NetworkResult.Error -> when (val error = result.error) {
                is NetworkingError.Unauthorized -> {
                    log.warn("AuthSession: refresh($reason) #$attempt rejected by server (401)")
                    invalidateLocked(InvalidationReason.RejectedByServer)
                }
                is NetworkingError.NoInternetConnection,
                is NetworkingError.NetworkTimeout,
                is NetworkingError.ServerError,
                is NetworkingError.TooManyRequests -> {
                    failRetryable(error, RetryAfter.parse(error), reason, attempt, now)
                }
                is NetworkingError.BadRequest,
                is NetworkingError.Forbidden,
                is NetworkingError.NotFound,
                is NetworkingError.Conflict,
                is NetworkingError.PayloadTooLarge,
                is NetworkingError.SerializationError,
                is NetworkingError.Unknown -> failBroken(error, reason, attempt, now)
            }

            is NetworkResult.Success -> commitSuccessLocked(current, result.data, reason, attempt, now)
        }
    }

    private suspend fun commitSuccessLocked(
        current: TokenPair,
        pair: TokenPair,
        reason: RefreshReason,
        attempt: Int,
        now: Instant,
    ): RefreshOutcome {
        val newSubject = JwtUtils.getUserIdFromToken(pair.refreshToken)
        val newRefreshExp = JwtUtils.expiresAtEpochMillis(pair.refreshToken)
        val unusable = pair.accessToken.isBlank() || pair.refreshToken.isBlank() ||
            newSubject.isBlank() || newRefreshExp == null
        if (unusable) {
            // 200 with garbage (empty body survives as blank fields, captive portals as parse failures
            // upstream). Never persist something the next attempt could not use (case 51).
            val error = NetworkingError.SerializationError(message = "refresh returned an unusable token pair")
            return failBroken(error, reason, attempt, now)
        }
        val currentSubject = JwtUtils.getUserIdFromToken(current.refreshToken)
        if (currentSubject.isNotBlank() && newSubject != currentSubject) {
            log.error("AuthSession: refresh($reason) #$attempt returned tokens for another user")
            return invalidateLocked(InvalidationReason.ForeignSubject)
        }

        try {
            withContext(NonCancellable) { store.write(pair) }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            // The server already rotated. Keep the OLD pair in the mirror so the next attempt
            // presents it and is rescued by the server's one-hop replay (case 27). Short retry,
            // no backoff growth - a long wait would only push the recovery out.
            log.error("AuthSession: refresh($reason) #$attempt token write failed: ${e.message}")
            val error = NetworkingError.Unknown(message = "token storage write failed: ${e.message}")
            lastError = error
            cooldownUntil = now + config.storageFailureRetry
            backoffArmed = true
            _state.value = AuthSessionState.Degraded(error, now + config.storageFailureRetry)
            scheduleLocked(now + config.storageFailureRetry, RefreshReason.Scheduled)
            return RefreshOutcome.Retryable(error, config.storageFailureRetry)
        }

        mirror = pair
        mirrorLoaded = true
        resetBackoffLocked()
        lastSuccessAt = now
        wasActive = true
        _state.value = AuthSessionState.Active(newSubject)
        sink.onSessionActive(pair)
        scheduleProactiveLocked(pair, now)
        log.info("AuthSession: refresh($reason) #$attempt succeeded")
        return RefreshOutcome.Success
    }

    private suspend fun failRetryable(
        error: NetworkingError,
        retryAfter: Duration?,
        reason: RefreshReason,
        attempt: Int,
        now: Instant,
    ): RefreshOutcome {
        val delay = backoff.nextDelay(retryAfter)
        lastError = error
        cooldownUntil = now + delay
        backoffArmed = true
        consecutiveBroken = 0
        brokenNotified = false
        _state.value = AuthSessionState.Degraded(error, now + delay)
        scheduleLocked(now + delay, RefreshReason.Scheduled)
        log.warn(
            "AuthSession: refresh($reason) #$attempt retryable (${error.errorCode}): ${error.message}; " +
                "backoff ${backoff.attempt}, next in ${delay.inWholeSeconds} s"
        )
        return RefreshOutcome.Retryable(error, delay)
    }

    private suspend fun failBroken(
        error: NetworkingError,
        reason: RefreshReason,
        attempt: Int,
        now: Instant,
    ): RefreshOutcome {
        val delay = backoff.capDelay()
        lastError = error
        cooldownUntil = now + delay
        backoffArmed = true
        consecutiveBroken++
        _state.value = AuthSessionState.Degraded(error, now + delay)
        scheduleLocked(now + delay, RefreshReason.Scheduled)
        log.error(
            "AuthSession: refresh($reason) #$attempt broken (${error.errorCode}): ${error.message}; " +
                "$consecutiveBroken in a row, next in ${delay.inWholeSeconds} s"
        )
        if (consecutiveBroken >= config.brokenThreshold && !brokenNotified) {
            brokenNotified = true
            sink.onServerUnreachable(error)
        }
        return RefreshOutcome.Broken(error)
    }

    private suspend fun invalidateLocked(reason: InvalidationReason): RefreshOutcome {
        val hadSession = mirror != null
        val notify = wasActive
        generation++
        inFlight = null
        schedulerJob?.cancel()
        schedulerJob = null
        mirror = null
        mirrorLoaded = true
        resetBackoffLocked()
        lastSuccessAt = null
        wasActive = false
        _state.value = AuthSessionState.Invalidated(reason)
        if (hadSession) {
            try {
                withContext(NonCancellable) { store.clear() }
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                log.error("AuthSession: storage clear failed after invalidation: ${e.message}")
            }
        }
        if (notify) {
            log.warn("AuthSession: session invalidated ($reason)")
            sink.onSessionInvalidated(reason)
        }
        return RefreshOutcome.Invalidated(reason)
    }

    private fun resetBackoffLocked() {
        backoff.reset()
        cooldownUntil = null
        lastError = null
        consecutiveBroken = 0
        brokenNotified = false
        backoffArmed = false
    }

    /**
     * (Re)arms the single retry/proactive timer. Runs in [scope]; the refresh it fires goes
     * through every gate again.
     */
    private fun scheduleLocked(at: Instant, reason: RefreshReason) {
        schedulerJob?.cancel()
        schedulerJob = scope.launch {
            val wait = at - clock.now()
            if (wait.isPositive()) delay(wait)
            refresh(reason)
        }
    }

    private suspend fun scheduleProactiveLocked(tokens: TokenPair, now: Instant) {
        schedulerJob?.cancel()
        schedulerJob = null
        proactiveDisabledFor = null
        val exp = JwtUtils.expiresAtEpochMillis(tokens.accessToken) ?: return
        val expiresAt = Instant.fromEpochMilliseconds(exp)
        if (expiresAt <= now) {
            // The server just issued this token and the local clock already considers it expired:
            // the clock is ahead. Scheduling from it would refresh in a loop; the reactive 401 path
            // still covers real expiry (case 23).
            proactiveDisabledFor = tokens.accessToken
            log.warn("AuthSession: fresh access token already expired by the local clock, proactive refresh disabled")
            return
        }
        val earliest = now + config.postSuccessFloor
        val target = expiresAt - config.proactiveThreshold
        scheduleLocked(if (target < earliest) earliest else target, RefreshReason.Proactive)
    }

    private suspend fun onResetTrigger(reason: RefreshReason) {
        val shouldRefresh = mutex.withLock {
            resetBackoffLocked()
            val tokens = mirror ?: return@withLock false
            val now = clock.now()
            val degraded = _state.value is AuthSessionState.Degraded
            if (!degraded && !accessTokenNeedsRefresh(tokens, now)) return@withLock false
            lastResetTriggeredRefreshAt?.let { last ->
                if (now - last < config.resetTriggerDebounce) return@withLock false
            }
            lastResetTriggeredRefreshAt = now
            true
        }
        if (shouldRefresh) {
            log.info("AuthSession: $reason, refreshing")
            scope.launch { refresh(reason) }
        }
    }
}

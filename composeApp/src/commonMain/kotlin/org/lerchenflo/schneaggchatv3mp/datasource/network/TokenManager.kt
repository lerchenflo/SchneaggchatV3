@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.datasource.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.clearAuthTokens
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.di.HTTPCLIENTTYPE
import org.lerchenflo.schneaggchatv3mp.utilities.JwtUtils
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Outcome of a token-refresh attempt. Unlike a plain `RequestError?`, this distinguishes
 * "try again later" (offline, rate-limited, server error) from "the session is actually dead"
 * (server rejected the refresh token) - callers need to react very differently to each.
 */
sealed interface RefreshResult {
    /** New tokens were fetched and persisted. */
    data object Success : RefreshResult

    /** The refresh could not complete right now, but the refresh token itself may still be valid. */
    data class Retryable(val error: NetworkingError) : RefreshResult

    /** The server rejected the refresh token (or none is stored) - the session is gone. */
    data object Invalidated : RefreshResult
}

class TokenManager(
    private val preferenceManager: Preferencemanager,
    private val loggingRepository: LoggingRepository,
) {
    /** How long a Retryable failure suppresses further network attempts for the same refresh token. */
    private val retryCooldownDuration = 5.seconds

    /** Refresh proactively once the access token has less than this much validity left. */
    private val proactiveRefreshThresholdMinutes = 2L

    private data class Cooldown(val refreshToken: String, val until: Instant, val error: NetworkingError)

    private val refreshMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var inFlight: Deferred<RefreshResult>? = null
    private var cooldown: Cooldown? = null

    suspend fun loadBearerTokens(): BearerTokens? {
        val tokens = preferenceManager.getTokens()
        if (tokens.refreshToken.isBlank()) return null

        // Refresh proactively once the access token is close to expiry instead of waiting for a
        // 401 - saves a wasted round trip on the very next request and keeps the (Auth-plugin-less)
        // WebSocket client from ever needing to connect with an about-to-expire token.
        val remainingMinutes = JwtUtils.getTokenValidRemainingMinutes(tokens.accessToken)
        if (remainingMinutes < proactiveRefreshThresholdMinutes && JwtUtils.isTokenDateValid(tokens.refreshToken)) {
            if (refreshTokens(tokens.refreshToken) == RefreshResult.Success) {
                val refreshed = preferenceManager.getTokens()
                return BearerTokens(refreshed.accessToken, refreshed.refreshToken)
            }
        }

        return BearerTokens(tokens.accessToken, tokens.refreshToken)
    }

    suspend fun refreshTokens(oldRefreshToken: String? = null): RefreshResult {
        val outcome: Pair<Deferred<RefreshResult>?, RefreshResult?> = refreshMutex.withLock {
            val currentTokens = preferenceManager.getTokens()

            // If the token that caused the 401 is different from the token we currently have
            // in preferences, another caller already refreshed while we waited for the lock.
            if (oldRefreshToken != null && currentTokens.refreshToken != oldRefreshToken) {
                return@withLock null to RefreshResult.Success
            }

            if (currentTokens.refreshToken.isBlank()) {
                // Nothing to refresh - there is no session to keep alive.
                return@withLock null to RefreshResult.Invalidated
            }

            val activeCooldown = cooldown
            if (activeCooldown != null && activeCooldown.refreshToken == currentTokens.refreshToken) {
                if (Clock.System.now() < activeCooldown.until) {
                    // A refresh for this exact token failed with a retryable error very recently.
                    // Don't hammer the (rate-limited) endpoint again - report the cached failure.
                    return@withLock null to RefreshResult.Retryable(activeCooldown.error)
                }
                cooldown = null
            }

            val active = inFlight
            if (active != null && active.isActive) {
                active to null
            } else {
                val deferred = scope.async { doRefresh(currentTokens.refreshToken) }
                inFlight = deferred
                deferred.invokeOnCompletion {
                    // Only clear inFlight if it still points at *this* deferred - avoids a newer
                    // in-flight refresh being wiped out by an older one's completion callback.
                    scope.launch {
                        refreshMutex.withLock {
                            if (inFlight === deferred) inFlight = null
                        }
                    }
                }
                deferred to null
            }
        }

        val (deferred, immediateResult) = outcome
        return immediateResult ?: withContext(NonCancellable) { deferred!!.await() }
    }

    private suspend fun recordCooldown(refreshToken: String, error: NetworkingError) {
        // doRefresh runs outside the mutex; take it so this write is visible to the
        // cooldown check in refreshTokens(), which reads under the same lock.
        refreshMutex.withLock {
            cooldown = Cooldown(refreshToken, Clock.System.now() + retryCooldownDuration, error)
        }
    }

    private suspend fun doRefresh(refreshToken: String): RefreshResult {
        return try {
            val networkUtils = KoinPlatform.getKoin().get<NetworkUtils>()

            when (val result = networkUtils.refresh(refreshToken)) {
                is NetworkResult.Error -> {
                    val error = result.error
                    if (error is NetworkingError.Unauthorized) {
                        loggingRepository.logError("TokenManager: Refresh token rejected by server (401)")
                        AppRepository.ActionChannel.sendActionSuspend(AppRepository.ActionChannel.ActionEvent.AuthInvalidated)
                        RefreshResult.Invalidated
                    } else {
                        loggingRepository.logWarning("TokenManager: Refresh failed retryably (${error.errorCode}): ${error.message}")
                        recordCooldown(refreshToken, error)
                        RefreshResult.Retryable(error)
                    }
                }

                is NetworkResult.Success -> {
                    // Persist tokens before returning - the HTTP client caches whatever
                    // loadBearerTokens() returns right after this call completes.
                    withContext(NonCancellable) {
                        KoinPlatform.getKoin().get<AppRepository>().onNewTokenPair(result.data)
                    }
                    // Drop the Auth plugin's cached BearerTokens so a refresh triggered outside
                    // the plugin (Login action, socket reconnect) doesn't leave the authenticated
                    // client sending the old access token until its next 401.
                    KoinPlatform.getKoin().get<HttpClient>(named(HTTPCLIENTTYPE.AUTHENTICATED)).clearAuthTokens()
                    RefreshResult.Success
                }
            }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive() // rethrow cancellation, don't swallow it as a retryable failure
            loggingRepository.logError("TokenManager: Exception during refresh: ${e.stackTraceToString()}")
            val error = NetworkingError.Unknown(message = e.message)
            recordCooldown(refreshToken, error)
            RefreshResult.Retryable(error)
        }
    }
}

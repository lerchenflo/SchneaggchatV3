package org.lerchenflo.schneaggchatv3mp.datasource.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.RequestError
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.di.HTTPCLIENTTYPE
import kotlin.time.Clock
import kotlin.time.TimeSource

class TokenManager(
    private val preferenceManager: Preferencemanager,
    private val loggingRepository: LoggingRepository,
) {
    companion object {
        private val refreshMutex = Mutex()
    }

    suspend fun loadBearerTokens(): BearerTokens? {
        val tokens = preferenceManager.getTokens()
        return if (tokens.refreshToken.isBlank()) {
            null
        } else {
            BearerTokens(tokens.accessToken, tokens.refreshToken)
        }
    }

    suspend fun refreshTokens(oldRefreshToken: String? = null): RequestError? {

        return refreshMutex.withLock {

            val currentTokens = preferenceManager.getTokens()
            
            // If the token that caused the 401 is different from the token we currently have 
            // in preferences, it means another thread ALREADY refreshed the token while we 
            // were waiting for the Mutex lock!
            if (oldRefreshToken != null && currentTokens.refreshToken != oldRefreshToken) {
                loggingRepository.logInfo("TokenManager: Token refresh skipped - already refreshed by another thread")
                return@withLock null
            }

            if (oldRefreshToken == null && currentTokens.refreshToken.isNotBlank()) {
                // Another thread likely already refreshed — bail out
                return@withLock null
            }

            if (currentTokens.refreshToken.isBlank()) {
                loggingRepository.logError("TokenManager: Token refresh aborted - no refresh token available")
                return@withLock null
            }

            loggingRepository.logDebug("TokenManager: Sending refresh request to server")

            try {
                // Use the existing "auth" HttpClient to avoid circular dependency
                val authClient = KoinPlatform.getKoin().get<HttpClient>(qualifier = named(HTTPCLIENTTYPE.NOT_AUTHENTICATED))
                val serverUrl = preferenceManager.buildServerUrl("/auth/refresh")

                val response = authClient.post(serverUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(NetworkUtils.RefreshRequest(currentTokens.refreshToken))
                }
                

                if (response.status.value == 401) {
                    loggingRepository.logError("TokenManager: Authentication invalidated (401) - refresh token likely expired")
                    AppRepository.ActionChannel.sendActionSuspend(AppRepository.ActionChannel.ActionEvent.AuthInvalidated)
                    return@withLock null
                }

                if (response.status.value == 409) {
                    loggingRepository.logInfo("TokenManager: Concurrent refresh detected - reloading tokens from storage")
                    return@withLock null  // skip saving, just fall through to loadBearerTokens()
                }

                if (!response.status.isSuccess()) {
                    val errorBody = response.body<String>()
                    loggingRepository.logError("TokenManager: HTTP error ${response.status.value}: ${errorBody.take(200)}")
                    return@withLock null
                }

                val rawBody = response.body<String>()

                val responseTokens = runCatching {
                    Json.decodeFromString<NetworkUtils.TokenPair>(rawBody)
                }.getOrNull() ?: run {
                    loggingRepository.logError("TokenManager: Invalid response format - JSON parsing failed")
                    return@withLock null
                }

                // Save new tokens via AppRepository to ensure single source of truth
                loggingRepository.logDebug("TokenManager: Saving new tokens to storage")

                withContext(NonCancellable) {
                    KoinPlatform.getKoin().get<AppRepository>().onNewTokenPair(responseTokens)
                }

                loggingRepository.logInfo("TokenManager: Tokens saved successfully")

                null
            } catch (e: Exception) {
                loggingRepository.logError("TokenManager: Exception stack trace: ${e.stackTraceToString()}")
                null
            }
        }
    }
}

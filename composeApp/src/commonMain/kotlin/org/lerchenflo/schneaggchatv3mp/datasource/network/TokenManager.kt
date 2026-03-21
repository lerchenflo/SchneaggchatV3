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

class TokenManager(
    private val preferenceManager: Preferencemanager,
    private val loggingRepository: LoggingRepository,
) {
    private val refreshMutex = Mutex()

    suspend fun loadBearerTokens(): BearerTokens? {
        val tokens = preferenceManager.getTokens()
        return if (tokens.refreshToken.isBlank()) {
            null
        } else {
            BearerTokens(tokens.accessToken, tokens.refreshToken)
        }
    }

    suspend fun refreshTokens(): RequestError? {
        return refreshMutex.withLock {
            loggingRepository.logDebug("Token refresh started")
            
            val currentTokens = preferenceManager.getTokens()
            if (currentTokens.refreshToken.isBlank()) {
                loggingRepository.logWarning("Token refresh aborted: No refresh token available")
                return@withLock null
            }

            loggingRepository.logDebug("Token refresh: Sending request to server")

            try {
                // Use the existing "auth" HttpClient to avoid circular dependency
                val authClient = KoinPlatform.getKoin().get<HttpClient>(qualifier = named("auth"))
                val response = authClient.post(preferenceManager.buildServerUrl("/auth/refresh")) {
                    contentType(ContentType.Application.Json)
                    setBody(NetworkUtils.RefreshRequest(currentTokens.refreshToken))
                }
                
                loggingRepository.logDebug("Token refresh: Received response with status ${response.status.value}")
                
                if (response.status.value == 401) {
                    loggingRepository.logError("Token refresh failed: Authentication invalidated (401)")
                    AppRepository.ActionChannel.sendActionSuspend(AppRepository.ActionChannel.ActionEvent.AuthInvalidated)
                    return@withLock null
                }

                if (response.status.value == 409) {
                    loggingRepository.logWarning("Token refresh conflict: Another refresh in progress (409)")
                    // Another in-flight refresh likely succeeded and persisted new tokens.
                    // Give it a moment to finish writing, then reload from storage.
                    delay(200)
                    val tokensAfter = preferenceManager.getTokens()
                    if (tokensAfter.refreshToken.isNotBlank()) {
                        loggingRepository.logDebug("Token refresh: Reloaded tokens after conflict")
                        withContext(NonCancellable) {
                            KoinPlatform.getKoin().get<AppRepository>().onNewTokenPair(tokensAfter)
                        }
                    } else {
                        loggingRepository.logError("Token refresh: No tokens available after conflict resolution")
                    }
                    return@withLock null
                }

                if (!response.status.isSuccess()) {
                    loggingRepository.logError("Token refresh failed: HTTP ${response.status.value}")
                    return@withLock null
                }

                val rawBody = response.body<String>()
                loggingRepository.logDebug("Token refresh: Parsing response body")
                
                val responseTokens = runCatching {
                    Json.decodeFromString<NetworkUtils.TokenPair>(rawBody)
                }.getOrNull() ?: run {
                    loggingRepository.logError("Token refresh failed: Invalid response format")
                    return@withLock null
                }

                loggingRepository.logInfo("Token refresh successful: New tokens received")
                
                // Save new tokens via AppRepository to ensure single source of truth
                loggingRepository.logDebug("Token refresh: Saving new tokens")
                withContext(NonCancellable) {
                    KoinPlatform.getKoin().get<AppRepository>().onNewTokenPairSync(responseTokens)
                }
                loggingRepository.logInfo("Token refresh: Tokens saved successfully")

                null
            } catch (e: Exception) {
                loggingRepository.logError("Token refresh failed: ${e.message}")
                null
            }
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.datasource.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.authProviders
import io.ktor.client.plugins.auth.clearAuthTokens
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.defaultTransformers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.RequestError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkError
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager

class TokenManager(
    private val preferenceManager: Preferencemanager,
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
            println("TOKENREFRESH: START")
            
            val currentTokens = preferenceManager.getTokens()
            if (currentTokens.refreshToken.isBlank()) {
                println("TOKENREFRESH: EXIT - No refresh token")
                return@withLock null
            }

            try {
                // Use the existing "auth" HttpClient to avoid circular dependency
                val authClient = KoinPlatform.getKoin().get<HttpClient>(qualifier = named("auth"))
                val response = authClient.post(preferenceManager.buildServerUrl("/auth/refresh")) {
                    contentType(ContentType.Application.Json)
                    setBody(NetworkUtils.RefreshRequest(currentTokens.refreshToken))
                }
                
                if (response.status.value == 401) {
                    println("TOKENREFRESH: 401 LOGOUT")
                    AppRepository.ActionChannel.sendActionSuspend(AppRepository.ActionChannel.ActionEvent.AuthInvalidated)
                    return@withLock null
                }

                val rawBody = response.body<String>()
                val responseTokens = runCatching {
                    Json.decodeFromString<NetworkUtils.TokenPair>(rawBody)
                }.getOrNull() ?: run {
                    println("TOKENREFRESH: FAILED - Invalid response")
                    return@withLock null
                }

                println("TOKENREFRESH: SUCCESS")
                
                // Save new tokens
                preferenceManager.saveTokens(responseTokens)
                SessionCache.updateTokens(responseTokens)
                
                // Clear auth tokens from HTTP client to force reload
                //KoinPlatform.getKoin().get<HttpClient>(qualifier = named("api")).clearAuthTokens()
                
                null
            } catch (e: Exception) {
                println("TOKENREFRESH: FAILED - ${e.message}")
                null
            }
        }
    }
}

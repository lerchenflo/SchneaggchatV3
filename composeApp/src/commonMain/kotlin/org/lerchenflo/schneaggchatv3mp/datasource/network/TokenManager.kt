package org.lerchenflo.schneaggchatv3mp.datasource.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LogType
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.RequestError
import org.lerchenflo.schneaggchatv3mp.utilities.JwtUtils
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager

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

    suspend fun refreshBearerTokens(client: HttpClient, oldTokens: BearerTokens?): BearerTokens? {
        return refreshMutex.withLock {
            val current = loadBearerTokens()
            if (current != null && oldTokens != null && current.refreshToken != oldTokens.refreshToken) {
                return@withLock current
            }

            loggingRepository.log(
                "HttpClient refreshing tokens; OldTokens: Access: ${oldTokens?.accessToken} Refresh: ${oldTokens?.refreshToken}",
                LogType.DEBUG
            )

            val refreshRequest = NetworkUtils.RefreshRequest(oldTokens?.refreshToken ?: "")

            val response = client.post(preferenceManager.buildServerUrl("/auth/refresh")) {
                contentType(ContentType.Application.Json)
                setBody(refreshRequest)
                markAsRefreshTokenRequest()
            }

            val rawBody = response.body<String>()

            val responseTokens = runCatching {
                Json.decodeFromString<NetworkUtils.TokenPair>(rawBody)
            }.getOrNull() ?: return@withLock null

            preferenceManager.saveTokens(responseTokens)

            val userId = JwtUtils.getUserIdFromToken(responseTokens.refreshToken)
            if (userId.isNotBlank()) {
                SessionCache.updateOwnId(userId)
            }
            SessionCache.updateTokenPair(responseTokens)

            BearerTokens(
                accessToken = responseTokens.accessToken,
                refreshToken = responseTokens.refreshToken
            )
        }
    }

    suspend fun refreshTokenPairLocked(networkUtils: NetworkUtils): RequestError? {
        return refreshMutex.withLock {
            when (val result = networkUtils.refresh(preferenceManager.getTokens().refreshToken)) {
                is org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult.Error<*> -> result.error
                is org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult.Success<NetworkUtils.TokenPair> -> {
                    preferenceManager.saveTokens(result.data)
                    val userId = JwtUtils.getUserIdFromToken(result.data.refreshToken)
                    if (userId.isNotBlank()) {
                        preferenceManager.saveOWNID(userId)
                        SessionCache.updateOwnId(userId)
                    }
                    SessionCache.updateTokenPair(result.data)
                    null
                }
            }
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.datasource.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.authProviders
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

    suspend fun refreshBearerTokens(client: HttpClient, oldTokens: BearerTokens?): BearerTokens? {
        return refreshMutex.withLock {

            println("BEARERTOKENREFRESH: START")

            val current = loadBearerTokens()
            if (current != null && oldTokens != null && current.refreshToken != oldTokens.refreshToken) {
                println("BEARERTOKENREFRESH: EXIT 1")
                return@withLock current
            }

            val refreshRequest = NetworkUtils.RefreshRequest(oldTokens?.refreshToken ?: "")

            val response = client.post(preferenceManager.buildServerUrl("/auth/refresh")) {
                contentType(ContentType.Application.Json)
                setBody(refreshRequest)
            }

            if (response.status.value == 401) {
                println("BEARERTOKENREFRESH: 401 LOGOUT")

                AppRepository.ActionChannel.sendActionSuspend(AppRepository.ActionChannel.ActionEvent.AuthInvalidated)

                println("BEARERTOKENREFRESH: LOGOUT FINISHED")

                return@withLock null
            }

            val rawBody = response.body<String>()

            val responseTokens = runCatching {
                Json.decodeFromString<NetworkUtils.TokenPair>(rawBody)
            }.getOrNull() ?: run {
                println("BEARERTOKENREFRESH: EXIT 2")

                return@withLock null
            }

            preferenceManager.saveTokens(responseTokens)


            SessionCache.updateTokens(responseTokens)

            println("BEARERTOKENREFRESH: FINISHED")

            BearerTokens(
                accessToken = responseTokens.accessToken,
                refreshToken = responseTokens.refreshToken
            )
        }
    }

    suspend fun refreshTokenPairLocked(networkUtils: NetworkUtils): RequestError? {
        return refreshMutex.withLock {
            when (val result = networkUtils.refresh(preferenceManager.getTokens().refreshToken)) {
                is NetworkResult.Error<*> -> {
                    println("token refresh failed: ${result.error}")
                    result.error
                }
                is NetworkResult.Success<NetworkUtils.TokenPair> -> {
                    preferenceManager.saveTokens(result.data)

                    SessionCache.updateTokens(result.data)

                    null
                }
            }
        }
    }
}

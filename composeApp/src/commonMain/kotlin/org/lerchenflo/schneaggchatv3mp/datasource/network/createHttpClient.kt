package org.lerchenflo.schneaggchatv3mp.datasource.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthSessionManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.RefreshOutcome
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.RefreshReason

/**
 * Client without authentication: login, registration, token refresh, logout, external fetches.
 * Takes no session dependency on purpose - the session manager's refresh API is built on this
 * client, so giving it the manager would be a construction cycle.
 */
fun createHttpClient(engine: HttpClientEngine): HttpClient {
    return HttpClient(engine) {
        installCommon()
    }
}

/**
 * Client for every authenticated endpoint. The bearer provider holds no token copy of its own
 * (`cacheTokens = false`): every request reads the session manager's in-memory mirror, so a
 * refresh that happened outside this client (socket handshake, scheduler) is picked up on the
 * very next request without a 401 round trip, and there is no cache to clear on logout.
 */
fun createAuthenticatedHttpClient(
    engine: HttpClientEngine,
    authSession: AuthSessionManager,
): HttpClient {
    return HttpClient(engine) {
        installCommon()

        install(WebSockets) {
            pingIntervalMillis = 3_000
        }

        install(Auth) {
            bearer {
                cacheTokens = false

                loadTokens {
                    authSession.currentTokens()?.toBearerTokens()
                }

                refreshTokens {
                    // The token the failing request actually carried. If the mirror already holds
                    // a newer pair (rotated by the socket path or another request), the manager
                    // answers Success without spending a rotation, and the retry uses the mirror.
                    val sentAccessToken = response.request.headers[HttpHeaders.Authorization]
                        ?.removePrefix("Bearer ")
                        ?.trim()

                    val outcome = authSession.refresh(
                        reason = RefreshReason.Reactive401,
                        presentedRefreshToken = oldTokens?.refreshToken,
                        presentedAccessToken = sentAccessToken,
                    )

                    // Only hand tokens back when a usable pair is in place. Returning the same
                    // stale tokens after a failed refresh would make Ktor retry with them and
                    // re-enter this callback on every subsequent request - a refresh storm.
                    // null: the 401 is returned to the caller and Ktor stops.
                    when (outcome) {
                        RefreshOutcome.Success -> authSession.currentTokens()?.toBearerTokens()
                        is RefreshOutcome.Retryable,
                        is RefreshOutcome.Broken,
                        is RefreshOutcome.Invalidated -> null
                    }
                }
            }
        }

        // R7: any successful authenticated request proves the network works - reset the refresh
        // backoff so the next 401 is not held back by a stale cooldown.
        install(authenticatedRequestSucceededHook(authSession))
    }
}

private fun NetworkUtils.TokenPair.toBearerTokens() = BearerTokens(accessToken, refreshToken)

private fun authenticatedRequestSucceededHook(authSession: AuthSessionManager) =
    createClientPlugin("AuthenticatedRequestSucceededHook") {
        onResponse { response ->
            if (response.status.isSuccess()) authSession.onAuthenticatedRequestSucceeded()
        }
    }

private fun HttpClientConfig<*>.installCommon() {
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                //println("KTOR LOG: $message")
            }
        }
        level = LogLevel.NONE
    }

    //Json
    install(ContentNegotiation) {
        json(
            json = AppJson.instance
        )
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 30000
        connectTimeoutMillis = 10000
        socketTimeoutMillis = 60000
    }
}


/**
 * Interval between WebSocket keepalive pings sent by the client. Detects a silently dead socket
 * (network switch, laptop sleep, tunnel) within roughly one interval instead of never - without
 * pings the connection state stays "Connected" while nothing flows, and no reconnect happens.
 *
 * Only the Darwin engine (iOS) honours the WebSockets plugin's pingIntervalMillis. The OkHttp
 * engine (Android, desktop) ignores it and only pings when the interval is set on the OkHttp
 * client itself, so the OkHttp-based socket modules pass this same value to
 * OkHttp.create { config { pingInterval(...) } }.
 */
const val SOCKET_PING_INTERVAL_MS = 20_000L

fun createSocketHttpClient(engine: HttpClientEngine): HttpClient {
    return HttpClient(engine) {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {}
            }
            level = LogLevel.NONE
        }
        install(ContentNegotiation) {
            json(json = AppJson.instance)
        }
        install(WebSockets) {
            pingIntervalMillis = SOCKET_PING_INTERVAL_MS
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 60000
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.datasource.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager


fun createHttpClient(
    engine: HttpClientEngine,
    preferencemanager: Preferencemanager,
    useAuth: Boolean
    ) : HttpClient {

    return HttpClient(engine) {
        install(Logging){
            level = LogLevel.ALL
        }


        //Json
        install(ContentNegotiation) {
            json(
                json = Json {
                    ignoreUnknownKeys = true
                }
            )
        }

        if (useAuth){
            install(Auth){
                bearer {

                    loadTokens {
                        val tokens = preferencemanager.getTokens()

                        if (tokens.refreshToken == ""){
                            null
                        } else {
                            BearerTokens(tokens.accessToken, tokens.refreshToken)
                        }
                    }

                    refreshTokens {

                        val response: NetworkUtils.TokenPair = client.post("${SERVERURL}/auth/refresh") {
                            contentType(ContentType.Application.Json)
                            setBody(oldTokens?.refreshToken)
                            markAsRefreshTokenRequest()
                        }.body()

                        // Save new tokens
                        preferencemanager.saveTokens(response)

                        BearerTokens(response.accessToken, response.refreshToken)
                    }


                }
            }
        }



        install(HttpTimeout) {
            requestTimeoutMillis = 20000  // 30 seconds for the entire request
            connectTimeoutMillis = 3000  // 10 seconds to establish connection
            socketTimeoutMillis = 30000   // 30 seconds between TCP packets
        }
    }
}
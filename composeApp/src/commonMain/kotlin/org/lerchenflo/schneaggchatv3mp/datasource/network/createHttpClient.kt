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
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager


fun createHttpClient(
    engine: HttpClientEngine,
    preferenceManager: Preferencemanager,
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
                    //https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-polymorphic-serializer/
                    serializersModule = SerializersModule {
                        polymorphic(NetworkUtils.UserResponse::class) {
                            subclass(NetworkUtils.UserResponse.SimpleUserResponse::class)
                            subclass(NetworkUtils.UserResponse.FriendUserResponse::class)
                            subclass(NetworkUtils.UserResponse.SelfUserResponse::class)
                        }
                    }
                }
            )
        }

        if (useAuth){
            install(Auth){
                bearer {

                    loadTokens {
                        val tokens = preferenceManager.getTokens()

                        if (tokens.refreshToken == ""){
                            null
                        } else {
                            BearerTokens(tokens.accessToken, tokens.refreshToken)
                        }
                    }

                    refreshTokens {

                        println("Authenticated Client refreshing tokens with token $oldTokens")

                        val response = client.post(preferenceManager.buildServerUrl("/auth/refresh")) {
                            contentType(ContentType.Application.Json)
                            setBody(oldTokens?.refreshToken)
                            markAsRefreshTokenRequest()
                        }
                        println("Response tokens: ${response.body<String>()}")
                        val responseTokens = response.body<NetworkUtils.TokenPair>()

                        println("AUTH Httpclient is refreshing tokens...")

                        // Save new tokens
                        preferenceManager.saveTokens(responseTokens)

                        BearerTokens(
                            accessToken = responseTokens.accessToken,
                            refreshToken = responseTokens.refreshToken
                        )
                    }

                    /*
                    sendWithoutRequest { request ->
                        val tokens = SessionCache.tokens
                        tokens != null
                    }

                     */


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
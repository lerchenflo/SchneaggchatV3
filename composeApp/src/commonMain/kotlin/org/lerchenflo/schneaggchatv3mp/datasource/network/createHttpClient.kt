package org.lerchenflo.schneaggchatv3mp.datasource.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json

fun createHttpClient(
    engine: HttpClientEngine,
    tokenManager: TokenManager,
    useAuth: Boolean
) : HttpClient {


    return HttpClient(engine) {
        install(Logging){
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

        if (useAuth){

            install(WebSockets) {
                pingIntervalMillis = 3_000
            }


            install(Auth){
                bearer {

                    loadTokens {
                        //println("HTTPCLIENT: Loading Tokens...")
                        val tokens = tokenManager.loadBearerTokens()
                        tokens
                    }

                    refreshTokens {
                        //println("HTTPCLIENT: Automatic token refresh triggered")
                        
                        // Pass the failing refresh token downstream
                        val oldRefreshToken = this.oldTokens?.refreshToken
                        tokenManager.refreshTokens(oldRefreshToken)
                        
                        // Load fresh tokens after refresh
                        //println("HTTPCLIENT: Refresh finished, loading")
                        tokenManager.loadBearerTokens()
                    }

                }
            }
        }



        install(HttpTimeout) {
            requestTimeoutMillis = 5000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 5000
        }

        /*
        install(HttpRequestRetry) {
            //retryOnServerErrors(maxRetries = 3)
            retryOnException(maxRetries = 3)
            exponentialDelay() // 1s, 2s, 4s delays between retries

            modifyRequest { request ->
                // Optional: log retry attempts
                println("Retrying request: ${request.url}")
            }
        }

         */
    }
}


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
            pingIntervalMillis = 3_000
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 60000
        }
    }
}





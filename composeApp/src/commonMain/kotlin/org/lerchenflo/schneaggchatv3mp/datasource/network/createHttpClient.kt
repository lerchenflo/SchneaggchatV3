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
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.datasource.network.TokenManager

fun createHttpClient(
    engine: HttpClientEngine,
    tokenManager: TokenManager,
    useAuth: Boolean
) : HttpClient {

    return HttpClient(engine) {
        install(Logging){
            logger = object : Logger {
                override fun log(message: String) {
                    //TODO: Comment out if not debugging networking
                    //println("KTOR LOG: $message")
                }
            }
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

                        polymorphic(NetworkUtils.NotificationResponse::class) {
                            subclass(NetworkUtils.NotificationResponse.MessageNotificationResponse::class)
                            subclass(NetworkUtils.NotificationResponse.FriendRequestNotificationResponse::class)
                            subclass(NetworkUtils.NotificationResponse.SystemNotificationResponse::class)
                        }
                    }
                }
            )
        }

        if (useAuth){
            install(Auth){
                bearer {

                    loadTokens {
                        tokenManager.loadBearerTokens()
                    }

                    refreshTokens {
                        tokenManager.refreshBearerTokens(client, oldTokens)
                    }

                }
            }
        }



        install(HttpTimeout) {
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 3000
            socketTimeoutMillis = 30000
        }
    }
}

private val RefreshTokenRequestAttributeKey = AttributeKey<Unit>("RefreshTokenRequest")

fun HttpRequestBuilder.markAsRefreshTokenRequest() {
    attributes.put(RefreshTokenRequestAttributeKey, Unit)
}
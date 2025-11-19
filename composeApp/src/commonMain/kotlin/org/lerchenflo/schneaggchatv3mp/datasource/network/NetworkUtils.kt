package org.lerchenflo.schneaggchatv3mp.datasource.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.*
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


//val SERVERURL = "https://schneaggchatv3.lerchenflo.eu"
const val SERVERURL = "http://192.168.0.4:8080"

@OptIn(ExperimentalTime::class)
class NetworkUtils(
    private val httpClient: HttpClient,
    private val authHttpClient: HttpClient //For auth without the bearer
) {

    private suspend inline fun <reified T, reified R> safeAuthPost(
        endpoint: String,
        body: T
    ): NetworkResult<R, NetworkError> {
        return try {
            val response = authHttpClient.post("$SERVERURL$endpoint") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            if (response.status.isSuccess()) {
                NetworkResult.Success(response.body())
            } else {
                NetworkResult.Error(mapHttpStatusToError(response.status.value))
            }
        } catch (e: UnresolvedAddressException) {
            NetworkResult.Error(NetworkError.NO_INTERNET)
        } catch (e: HttpRequestTimeoutException) {
            NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: SocketTimeoutException) {
            NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: SerializationException) {
            NetworkResult.Error(NetworkError.SERIALIZATION)
        } catch (e: Exception) {
            e.printStackTrace()
            NetworkResult.Error(NetworkError.UNKNOWN)
        }
    }








    // Base methods that return HttpResponse
    private suspend inline fun <reified T> get(endpoint: String): HttpResponse {
        return httpClient.get("$SERVERURL$endpoint")
    }

    private suspend inline fun <reified T> post(endpoint: String, body: T): HttpResponse {
        return httpClient.post("$SERVERURL$endpoint") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    private suspend inline fun <reified T> put(endpoint: String, body: T): HttpResponse {
        return httpClient.put("$SERVERURL$endpoint") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    private suspend inline fun delete(endpoint: String): HttpResponse {
        return httpClient.delete("$SERVERURL$endpoint")
    }

    // Helper function to map HTTP status codes to NetworkError
    private fun mapHttpStatusToError(statusCode: Int): NetworkError {
        return when (statusCode) {
            401 -> NetworkError.UNAUTHORIZED
            408 -> NetworkError.REQUEST_TIMEOUT
            409 -> NetworkError.CONFLICT
            413 -> NetworkError.PAYLOAD_TOO_LARGE
            429 -> NetworkError.TOO_MANY_REQUESTS
            in 500..599 -> NetworkError.SERVER_ERROR
            else -> NetworkError.UNKNOWN
        }
    }

    // Safe wrapper methods with try-catch and response code checking
    private suspend inline fun <reified T> safeGet(endpoint: String): NetworkResult<T, NetworkError> {
        return try {
            val response = get<T>(endpoint)

            if (response.status.isSuccess()) {
                NetworkResult.Success(response.body())
            } else {
                NetworkResult.Error(mapHttpStatusToError(response.status.value))
            }
        } catch (e: UnresolvedAddressException) {
            NetworkResult.Error(NetworkError.NO_INTERNET)
        } catch (e: HttpRequestTimeoutException) {
            NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: SocketTimeoutException) {
            NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: SerializationException) {
            NetworkResult.Error(NetworkError.SERIALIZATION)
        } catch (e: Exception) {
            e.printStackTrace()
            NetworkResult.Error(NetworkError.UNKNOWN)
        }
    }

    private suspend inline fun <reified T, reified R> safePost(endpoint: String, body: T): NetworkResult<R, NetworkError> {
        return try {
            val response = post(endpoint, body)

            if (response.status.isSuccess()) {
                NetworkResult.Success(response.body())
            } else {
                NetworkResult.Error(mapHttpStatusToError(response.status.value))
            }
        } catch (e: UnresolvedAddressException) {
            NetworkResult.Error(NetworkError.NO_INTERNET)
        } catch (e: HttpRequestTimeoutException) {
            NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: SocketTimeoutException) {
            NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: SerializationException) {
            NetworkResult.Error(NetworkError.SERIALIZATION)
        } catch (e: Exception) {
            e.printStackTrace()
            NetworkResult.Error(NetworkError.UNKNOWN)
        }
    }

    private suspend inline fun <reified T, reified R> safePut(endpoint: String, body: T): NetworkResult<R, NetworkError> {
        return try {
            val response = put(endpoint, body)

            if (response.status.isSuccess()) {
                NetworkResult.Success(response.body())
            } else {
                NetworkResult.Error(mapHttpStatusToError(response.status.value))
            }
        } catch (e: UnresolvedAddressException) {
            NetworkResult.Error(NetworkError.NO_INTERNET)
        } catch (e: HttpRequestTimeoutException) {
            NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: SocketTimeoutException) {
            NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: SerializationException) {
            NetworkResult.Error(NetworkError.SERIALIZATION)
        } catch (e: Exception) {
            e.printStackTrace()
            NetworkResult.Error(NetworkError.UNKNOWN)
        }
    }

    private suspend inline fun <reified T> safeDelete(endpoint: String): NetworkResult<T, NetworkError> {
        return try {
            val response = delete(endpoint)

            if (response.status.isSuccess()) {
                NetworkResult.Success(response.body())
            } else {
                NetworkResult.Error(mapHttpStatusToError(response.status.value))
            }
        } catch (e: UnresolvedAddressException) {
            NetworkResult.Error(NetworkError.NO_INTERNET)
        } catch (e: HttpRequestTimeoutException) {
            NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: SocketTimeoutException) {
            NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: SerializationException) {
            NetworkResult.Error(NetworkError.SERIALIZATION)
        } catch (e: Exception) {
            e.printStackTrace()
            NetworkResult.Error(NetworkError.UNKNOWN)
        }
    }




    /*
    **************************************************************************

    AUTHENTICATION

    **************************************************************************
     */

    @Serializable
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String
    )

    @Serializable
    data class LoginRequest(
        val username: String,
        val password: String
    )

    suspend fun login(username: String, password: String): NetworkResult<TokenPair, NetworkError> {
        return safeAuthPost<LoginRequest, TokenPair>(
            endpoint = "/auth/login",
            body = LoginRequest(username = username, password = password)
        )
    }



    //TODO Add profilepicture after picture selection implemented
    @Serializable
    data class RegisterRequest(
        val username: String,
        val password: String,
        val email: String,
        val birthDate: String,
    )

    suspend fun register(
        username: String,
        password: String,
        email: String,
        birthDate: String,
        profilePicBytes: ByteArray,
        fileName: String = "profile.jpg"
    ): NetworkResult<Unit, NetworkError> {
        return try {
            val response = authHttpClient.submitFormWithBinaryData(
                url = "$SERVERURL/auth/register",
                formData = formData {
                    append("username", username)
                    append("password", password)
                    append("email", email)
                    append("birthDate", birthDate)
                    append("profilepic", profilePicBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                }
            )

            if (response.status.isSuccess()) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(mapHttpStatusToError(response.status.value))
            }
        } catch (e: UnresolvedAddressException) {
            NetworkResult.Error(NetworkError.NO_INTERNET)
        } catch (e: HttpRequestTimeoutException) {
            NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: SocketTimeoutException) {
            NetworkResult.Error(NetworkError.REQUEST_TIMEOUT)
        } catch (e: SerializationException) {
            NetworkResult.Error(NetworkError.SERIALIZATION)
        } catch (e: Exception) {
            e.printStackTrace()
            NetworkResult.Error(NetworkError.UNKNOWN)
        }
    }

    @Serializable
    data class RefreshRequest(
        val refreshToken: String
    )

    suspend fun refresh(refreshToken: String): NetworkResult<TokenPair, NetworkError> {
        println("Refreshing token...")
        return safeAuthPost<RefreshRequest, TokenPair>(
            endpoint = "/auth/refresh",
            body = RefreshRequest(refreshToken = refreshToken)
        )
    }





    /*
    **************************************************************************

    User sync

    **************************************************************************
     */
    @Serializable
    data class IdTimeStamp(val id: String, val timeStamp: String)

    suspend fun getProfilePicForUserId(userId: String) : NetworkResult<ByteArray, NetworkError> {
        return safeGet(
            endpoint = "/users/profilepic/$userId"
        )
    }


    @Serializable
    data class UserSyncResponse(val updatedUsers: List<UserResponse>, val deletedUsers: List<String>)

    @Serializable
    sealed interface UserResponse {

        //Common data which every response contains
        val id: String
        val username: String
        val userDescription: String
        val userStatus: String
        val updatedAt: Instant

        //Response for a user (Not yourself and not your friend)
        @Serializable
        data class SimpleUserResponse(
            override val id: String,
            override val username: String,
            override val userDescription: String,
            override val userStatus: String,
            override val updatedAt: Instant,

            //Custom to simpleuserresponse:
            val commonFriendCount: Int,

            ) : UserResponse

        //Response for a friend (He accepted your request)
        @Serializable
        data class FriendUserResponse(
            override val id: String,
            override val username: String,
            override val userDescription: String,
            override val userStatus: String,
            override val updatedAt: Instant,


            //Custom to friend response:
            val birthDate: String,



            ) : UserResponse

        //Response for yourself (You request your own data)
        @Serializable
        data class SelfUserResponse(
            override val id: String,
            override val username: String,
            override val userDescription: String,
            override val userStatus: String,
            override val updatedAt: Instant,


            //Custom to friend response
            val birthDate: String,

            //Custom to own user response:
            val email: String,
            val createdAt: Instant,


            //TODO: User profile pic privacy settings??


        ) : UserResponse
    }


    suspend fun userIdSync(userIds: List<IdTimeStamp>) : NetworkResult<UserSyncResponse, NetworkError> {
        return safePost(
            endpoint = "/users/sync",
            body = userIds
        )
    }



    /*
    **************************************************************************

    Group sync

    **************************************************************************
     */


    suspend fun getProfilePicForGroupId(groupId: String) : NetworkResult<ByteArray, NetworkError> {
        return safeGet(
            endpoint = "/groups/profilepic/$groupId"
        )
    }
}
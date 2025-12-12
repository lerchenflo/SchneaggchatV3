package org.lerchenflo.schneaggchatv3mp.datasource.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.RequestError
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class NetworkUtils(
    private val httpClient: HttpClient,
    private val authHttpClient: HttpClient, //For auth without the bearer
    private val preferenceManager: Preferencemanager
) {


    // Base methods that return HttpResponse
    private suspend inline fun <reified T> get(endpoint: String): HttpResponse {
        return httpClient.get(preferenceManager.buildServerUrl(endpoint))
    }

    private suspend inline fun <reified T> post(endpoint: String, body: T): HttpResponse {
        return httpClient.post(preferenceManager.buildServerUrl(endpoint)) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    private suspend inline fun <reified T> put(endpoint: String, body: T): HttpResponse {
        return httpClient.put(preferenceManager.buildServerUrl(endpoint)) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    private suspend inline fun delete(endpoint: String): HttpResponse {
        return httpClient.delete(preferenceManager.buildServerUrl(endpoint))
    }

    private suspend inline fun <reified R> safeCall(
        crossinline block: suspend () -> HttpResponse
    ): NetworkResult<R, NetworkError> {
        return try {
            val response = block()

            SessionCache.updateOnline(true)

            if (response.status.isSuccess()) {
                NetworkResult.Success(response.body())
            } else {
                NetworkResult.Error(mapHttpStatusToError(response.status.value, response.body()))
            }
        } catch (e: UnresolvedAddressException) {
            SessionCache.updateOnline(false)
            NetworkResult.Error(NetworkError.NoInternet())
        } catch (e: HttpRequestTimeoutException) {
            SessionCache.updateOnline(false)
            NetworkResult.Error(NetworkError.RequestTimeout())
        } catch (e: SocketTimeoutException) {
            SessionCache.updateOnline(false)
            NetworkResult.Error(NetworkError.RequestTimeout())
        } catch (e: ConnectTimeoutException) {
            SessionCache.updateOnline(false)
            NetworkResult.Error(NetworkError.RequestTimeout())
        } catch (e: SerializationException) {
            NetworkResult.Error(NetworkError.Serialization(message = e.message))
        } catch (e: Exception) {
            e.printStackTrace()
            SessionCache.updateOnline(false)
            NetworkResult.Error(NetworkError.Unknown(message = e.message))
        }
    }

    // Helper function to map HTTP status codes to NetworkError
    private fun mapHttpStatusToError(statusCode: Int, message: String?): NetworkError {
        return when (statusCode) {
            401 -> NetworkError.Unauthorized(message = message)
            404 -> NetworkError.NotFound(message = message)
            408 -> NetworkError.RequestTimeout(message = message)
            409 -> NetworkError.Conflict(message = message)
            413 -> NetworkError.PayloadTooLarge(message = message)
            429 -> NetworkError.TooManyRequests(message = message)
            in 500..599 -> NetworkError.ServerError(message = message)
            else -> NetworkError.Unknown(message = message)
        }
    }

    // Now all the safe methods use the single safeCall function
    private suspend inline fun <reified T> safeGet(endpoint: String): NetworkResult<T, NetworkError> {
        return safeCall {
            httpClient.get(preferenceManager.buildServerUrl(endpoint))
        }
    }

    private suspend inline fun <reified T, reified R> safePost(
        endpoint: String,
        body: T
    ): NetworkResult<R, NetworkError> {
        return safeCall {
            httpClient.post(preferenceManager.buildServerUrl(endpoint)) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    private suspend inline fun <reified T, reified R> safePut(
        endpoint: String,
        body: T
    ): NetworkResult<R, NetworkError> {
        return safeCall {
            httpClient.put(preferenceManager.buildServerUrl(endpoint)) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    private suspend inline fun <reified T> safeDelete(endpoint: String): NetworkResult<T, NetworkError> {
        return safeCall {
            httpClient.delete(preferenceManager.buildServerUrl(endpoint))
        }
    }

    private suspend inline fun <reified T, reified R> safeAuthPost(
        endpoint: String,
        body: T
    ): NetworkResult<R, NetworkError> {
        return safeCall {
            authHttpClient.post(preferenceManager.buildServerUrl(endpoint)) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    private suspend inline fun <reified T, reified R> safeAuthGet(
        endpoint: String,
        body: T
    ): NetworkResult<R, NetworkError> {
        return safeCall {
            authHttpClient.get(preferenceManager.buildServerUrl(endpoint)) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }




    suspend fun testServer(serverUrl: String) : NetworkResult<String, NetworkError> {
        return safeCall {
            authHttpClient.get(serverUrl + "/public/test")
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
                url = preferenceManager.buildServerUrl("/auth/register"),
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
                NetworkResult.Error(mapHttpStatusToError(response.status.value, response.body()))
            }
        } catch (e: UnresolvedAddressException) {
            NetworkResult.Error(NetworkError.NoInternet())
        } catch (e: HttpRequestTimeoutException) {
            NetworkResult.Error(NetworkError.RequestTimeout())
        } catch (e: SocketTimeoutException) {
            NetworkResult.Error(NetworkError.RequestTimeout())
        } catch (e: SerializationException) {
            NetworkResult.Error(NetworkError.Serialization())
        } catch (e: Exception) {
            e.printStackTrace()
            NetworkResult.Error(NetworkError.Unknown(message = e.message))
        }
    }

    suspend fun sendEmailVerify(){
        safePost<Any, Any>(
            endpoint = "/users/verificationemail",
            body = ""
        )
    }


    @Serializable
    data class RefreshRequest(
        val refreshToken: String
    )

    suspend fun refresh(refreshToken: String): NetworkResult<TokenPair, NetworkError> {
        return safeAuthPost<RefreshRequest, TokenPair>(
            endpoint = "/auth/refresh",
            body = RefreshRequest(refreshToken = refreshToken)
        )
    }


    suspend fun setFirebaseToken(token: String) : NetworkResult<Any, NetworkError> {
        return safePost(
            endpoint = "/users/setfirebasetoken?token=$token",
            body = ""
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
    enum class FriendshipStatus { PENDING, ACCEPTED, DECLINED, BLOCKED }


    @Serializable
    sealed interface UserResponse {

        //Common data which every response contains
        val id: String
        val username: String
        val updatedAt: Long


        //Response for a user (Not yourself and not your friend)
        @Serializable
        @SerialName("simple")
        data class SimpleUserResponse(
            override val id: String,
            override val username: String,
            override val updatedAt: Long,

            //Custom to simpleuserresponse:
            val friendShipStatus: FriendshipStatus?,
            val requesterId: String,

            ) : UserResponse

        //Response for a friend (He accepted your request)
        @Serializable
        @SerialName("friend")
        data class FriendUserResponse(
            override val id: String,
            override val username: String,

            override val updatedAt: Long,

            val requesterId: String?, //Who requested the friendship

            //Custom to friend response:
            val birthDate: String,
            val userDescription: String,
            val userStatus: String,



            ) : UserResponse

        //Response for yourself (You request your own data)
        @Serializable
        @SerialName("self")
        data class SelfUserResponse(
            override val id: String,
            override val username: String,

            override val updatedAt: Long,


            //Custom to friend response
            val birthDate: String,
            val userDescription: String,
            val userStatus: String,

            //Custom to own user response:
            val email: String,
            val emailVerifiedAt: Long?,
            val createdAt: Long,


        ) : UserResponse
    }

    suspend fun userIdSync(userIds: List<IdTimeStamp>) : NetworkResult<UserSyncResponse, NetworkError> {
        return safePost(
            endpoint = "/users/sync",
            body = userIds
        )
    }

    @Serializable
    data class NewFriendsUserResponse(
        val id: String,
        val username: String,
        val commonFriendCount: Int,
    )

    suspend fun getAvailableUsers(searchterm: String) : NetworkResult<List<NewFriendsUserResponse>, NetworkError> {
        return safeGet(
            endpoint = "/users/availableusers?searchterm=$searchterm"
        )
    }

    suspend fun sendFriendRequest(friendId: String) : NetworkResult<Any, NetworkError> {
        return safeGet(
            endpoint = "/users/addfriend/$friendId",
        )
    }

    suspend fun denyFriendRequest(friendId: String) : NetworkResult<Any, NetworkError> {
        return safeGet(
            endpoint = "/users/denyfriend/$friendId"
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




    /*
    **************************************************************************

    Messages

    **************************************************************************
     */

    @Serializable
    data class MessageRequest(
        val messageId: String?, //Objectid

        val receiverId: String,
        val groupMessage: Boolean,
        val msgType: MessageType,
        val content: String,
        val answerId: String?,
    )

    @Serializable
    data class MessageResponse(
        val messageId: String, //Objectid
        val senderId: String,
        val receiverId: String,
        val groupMessage: Boolean,
        val msgType: MessageType,
        val content: String,
        val answerId: String?,

        val sendDate: Long,
        val lastChanged: Long,
        val deleted: Boolean,
        val readers: List<ReaderResponse>
    )

    @Serializable
    data class ReaderResponse(
        val userId: String,
        val readAt: Long
    )

    suspend fun sendTextMessageToServer(empfaenger: String, gruppe: Boolean, content: String, answerid: String?) : NetworkResult<MessageResponse, NetworkError> {
        val messageRequest = MessageRequest(
            messageId = null,
            receiverId = empfaenger,
            groupMessage = gruppe,
            msgType = MessageType.TEXT,
            content = content,
            answerId = answerid,
        )

        println("MessageRequest: $messageRequest")

        return safePost(
            endpoint = "/messages/send/text",
            body = messageRequest
        )
    }

    @Serializable
    data class MessageSyncResponse(
        val updatedMessages: List<MessageResponse>,
        val deletedMessages: List<String>,
        val moreMessages: Boolean
    )

    suspend fun messageSync(messageIds: List<IdTimeStamp>, page: Int) : NetworkResult<MessageSyncResponse, RequestError>{
        return safePost(
            endpoint = "/messages/sync?page=$page&page_size=400",
            body = messageIds
        )
    }

    suspend fun setMessagesRead(chatId: String, group: Boolean, timeStamp: Long) : NetworkResult<String, RequestError>{
        return safePost(
            endpoint = "/messages/setread?userid=$chatId&group=$group&timestamp=$timeStamp",
            body = "",
        )
    }

}
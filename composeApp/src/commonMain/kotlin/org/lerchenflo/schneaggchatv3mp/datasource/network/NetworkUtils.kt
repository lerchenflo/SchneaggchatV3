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
import kotlinx.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.RequestError
import org.lerchenflo.schneaggchatv3mp.utilities.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.poll.PollVisibility
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.PollResponse
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import org.lerchenflo.schneaggchatv3mp.utilities.UiText.*
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.accepted
import schneaggchatv3mp.composeapp.generated.resources.blocked
import schneaggchatv3mp.composeapp.generated.resources.declined
import schneaggchatv3mp.composeapp.generated.resources.pending
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class NetworkUtils(
    private val httpClient: HttpClient,
    private val authHttpClient: HttpClient, //For auth without the bearer
    private val preferenceManager: Preferencemanager,
    private val loggingRepository: LoggingRepository
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

            // If we got a response (even an error response), we have connectivity
            SessionCache.updateOnline(true)

            if (response.status.isSuccess()) {
                NetworkResult.Success(response.body())
            } else {
                NetworkResult.Error(mapHttpStatusToError(response.status.value, response.body<String>()))
            }
        } catch (e: UnresolvedAddressException) {
            // DNS resolution failed - definitely offline
            println("Going offline: DNS resolution failed - ${e.message}")
            loggingRepository.logWarning("Going offline: UnresolvedAddressException - ${e.message}")
            SessionCache.updateOnline(false)
            NetworkResult.Error(NetworkError.NoInternet())
        } catch (e: ConnectTimeoutException) {
            // Could be offline or slow network - mark as offline
            println("Going offline: Connection timeout - ${e.message}")
            loggingRepository.logWarning("Going offline: ConnectTimeoutException - ${e.message}")
            SessionCache.updateOnline(false)
            NetworkResult.Error(NetworkError.RequestTimeout())
        } catch (e: SocketTimeoutException) {
            // Socket timeout - conservative approach marks as offline
            println("Going offline: Socket timeout - ${e.message}")
            loggingRepository.logWarning("Going offline: SocketTimeoutException - ${e.message}")
            SessionCache.updateOnline(false)
            NetworkResult.Error(NetworkError.RequestTimeout())
        } catch (e: HttpRequestTimeoutException) {
            // Request timeout - conservative approach marks as offline
            println("Going offline: HTTP request timeout - ${e.message}")
            loggingRepository.logWarning("Going offline: HttpRequestTimeoutException - ${e.message}")
            SessionCache.updateOnline(false)
            NetworkResult.Error(NetworkError.RequestTimeout())
        } catch (e: IOException) {
            // IO exceptions (including UnknownHostException) indicate network problems
            println("Going offline: IO exception - ${e.message}")
            loggingRepository.logWarning("Going offline: IOException - ${e.message}")
            e.printStackTrace()
            SessionCache.updateOnline(false)
            NetworkResult.Error(NetworkError.NoInternet())
        } catch (e: SerializationException) {
            // Serialization errors are NOT network issues - don't change online state
            println("Serialization error (staying online): ${e.message}")
            loggingRepository.logWarning("SerializationException (not changing online state): ${e.message}")
            NetworkResult.Error(NetworkError.Serialization(message = e.message))
        } catch (e: Exception) {
            // Unknown errors are NOT necessarily network issues - don't mark as offline
            println("Unknown exception (staying online): ${e.message}")
            loggingRepository.logWarning("NetworkUtils safeCall failed (not changing online state): ${e.message}")
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
            authHttpClient.get("$serverUrl/public/test")
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
        val refreshToken: String,
        val encryptionKey: String? = null
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
                println("Create response body: ${response.body<String>()}")
                NetworkResult.Error(mapHttpStatusToError(response.status.value, response.body<String>()))
            }
        } catch (e: UnresolvedAddressException) {
            NetworkResult.Error(NetworkError.NoInternet())
        } catch (e: HttpRequestTimeoutException) {
            NetworkResult.Error(NetworkError.RequestTimeout())
        } catch (e: SocketTimeoutException) {
            NetworkResult.Error(NetworkError.RequestTimeout())
        } catch (e: SerializationException) {
            NetworkResult.Error(NetworkError.Serialization())
        } catch (e: IOException) { // This catches UnknownHostException too
            SessionCache.updateOnline(false)
            NetworkResult.Error(NetworkError.NoInternet())
        }catch (e: Exception) {
            loggingRepository.logWarning("NetworkUtils register failed: ${e.message}")
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

    User

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
    enum class FriendshipStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        BLOCKED;

        fun toUiText(): UiText = when (this) {
            PENDING -> StringResourceText(Res.string.pending)
            ACCEPTED -> StringResourceText(Res.string.accepted)
            DECLINED -> StringResourceText(Res.string.declined)
            BLOCKED -> StringResourceText(Res.string.blocked)
        }
    }


    @Serializable
    sealed interface UserResponse {

        //Common data which every response contains
        val id: String
        val username: String
        val updatedAt: Long

        val profilePicUpdatedAt: Long



        //Response for a user (Not yourself and not your friend)
        @Serializable
        @SerialName("simple")
        data class SimpleUserResponse(
            override val id: String,
            override val username: String,
            override val updatedAt: Long,
            override val profilePicUpdatedAt: Long,

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
            override val profilePicUpdatedAt: Long,


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
            override val profilePicUpdatedAt: Long,


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

    suspend fun removeFriend(friendId: String) : NetworkResult<Any, NetworkError> {
        return safeGet(
            endpoint = "/users/removefriend/$friendId"
        )
    }

    suspend fun changeUsername(newUsername: String) : NetworkResult<Any, NetworkError> {
        return safePost(
            endpoint = "/users/changeusername",
            body = newUsername
        )
    }

    @Serializable
    data class PasswordChangeRequest(
        val oldPassword: String,
        val newPassword: String
    )

    suspend fun changePassword(oldPassword: String, newPassword: String) : NetworkResult<Any, NetworkError> {
        return safePost(
            endpoint = "/users/changepassword",
            body = PasswordChangeRequest(
                oldPassword = oldPassword,
                newPassword = newPassword
            )
        )
    }

    suspend fun changeProfilePic(newProfilePic: ByteArray, fileName: String = "profile.jpg"): NetworkResult<Any, NetworkError> {
        return try {
            val response = httpClient.submitFormWithBinaryData(
                url = preferenceManager.buildServerUrl("/users/setprofilepic"),
                formData = formData {
                    append("profilepic", newProfilePic, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                }
            )

            if (response.status.isSuccess()) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(mapHttpStatusToError(response.status.value, response.body<String>()))
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
        } catch (e: IOException) {
            SessionCache.updateOnline(false)
            NetworkResult.Error(NetworkError.NoInternet())
        } catch (e: Exception) {
            loggingRepository.logWarning("NetworkUtils changeProfilePic failed: ${e.message}")
            NetworkResult.Error(NetworkError.Unknown(message = e.message))
        }
    }

    @Serializable
    data class UserRequest(
        val userId: String,
        val newDescription: String?,
        val newStatus: String?,
        val newEmail: String?,
        val newBirthDate: String?,
        val newNickName: String?
    )
    
    suspend fun changeProfile(userId: String, newStatus: String?, newDescription: String?, newEmail: String?, newBirthDate: String?, newNickName: String?): NetworkResult<Any, NetworkError> {
        return safePost(
            endpoint = "/users/changeprofile",
            body = UserRequest(
                userId = userId,
                newDescription = newDescription,
                newStatus = newStatus,
                newEmail = newEmail,
                newBirthDate = newBirthDate,
                newNickName = newNickName
            )
        )
    }


    /*
    **************************************************************************

    Notifications

    **************************************************************************
     */

    @Serializable
    sealed interface NotificationResponse {

        @Serializable
        @SerialName("message")
        data class MessageNotificationResponse(
            val msgId: String,
            val senderName: String,
            val groupMessage: Boolean,
            val groupName: String,
            val encodedContent: String
        ) : NotificationResponse

        //Response for a friend request notification
        @Serializable
        @SerialName("friend_request")
        data class FriendRequestNotificationResponse(
            val requesterId: String,
            val requesterName: String,
            val accepted: Boolean
        ) : NotificationResponse

        //Response for a system notification
        @Serializable
        @SerialName("system")
        data class SystemNotificationResponse(
            val title: String,
            val message: String,
            val priority: String = "normal"
        ) : NotificationResponse
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

    @Serializable
    data class GroupResponse(
        val id: String,
        val name: String,
        val description: String,

        val updatedAt: Long,
        val profilePicUpdatedAt: Long,

        val createdAt: Long,
        val creatorId: String,
        val members: List<GroupMemberResponse>
    )

    @Serializable
    data class GroupMemberResponse(
        val userid: String,
        val joinedAt: String,
        val admin: Boolean,
        val color: Int,
        val memberName: String = ""
    )

    suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>, //Userids
        profilePicBytes: ByteArray,
    ): NetworkResult<GroupResponse, NetworkError> {
        val fileName = "image.png"
        return try {
            val response = httpClient.submitFormWithBinaryData(
                url = preferenceManager.buildServerUrl("/groups/create"),
                formData = formData {
                    append("name", name)
                    append("description", description)
                    append("memberlist", memberIds.joinToString(","))
                    append("profilepic", profilePicBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                }
            )
            println("Response string: ${response.body<String>()}")

            if (response.status.isSuccess()) {
                NetworkResult.Success(response.body())
            } else {
                NetworkResult.Error(mapHttpStatusToError(response.status.value, response.body<String>()))
            }
        } catch (e: UnresolvedAddressException) {
            NetworkResult.Error(NetworkError.NoInternet())
        } catch (e: HttpRequestTimeoutException) {
            NetworkResult.Error(NetworkError.RequestTimeout())
        } catch (e: SocketTimeoutException) {
            NetworkResult.Error(NetworkError.RequestTimeout())
        } catch (e: SerializationException) {
            NetworkResult.Error(NetworkError.Serialization())
        } catch (e: IOException) { // This catches UnknownHostException too
            SessionCache.updateOnline(false)
            NetworkResult.Error(NetworkError.NoInternet())
        }catch (e: Exception) {
            loggingRepository.logWarning("NetworkUtils createGroup failed: ${e.message}")
            NetworkResult.Error(NetworkError.Unknown(message = e.message))
        }
    }

    @Serializable
    data class GroupSyncResponse(
        val updatedGroups: List<GroupResponse>,
        val deletedGroups: List<String>
    )

    suspend fun groupIdSync(groupIds: List<IdTimeStamp>) : NetworkResult<GroupSyncResponse, NetworkError> {
        return safePost(
            endpoint = "/groups/sync",
            body = groupIds
        )
    }


    suspend fun changeGroupProfilePic(newProfilePic: ByteArray, groupId: String, fileName: String = "profile.jpg"): NetworkResult<Any, NetworkError> {
        return try {
            val response = httpClient.submitFormWithBinaryData(
                url = preferenceManager.buildServerUrl("/groups/setprofilepic?groupid=$groupId"),
                formData = formData {
                    append("profilepic", newProfilePic, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                }
            )

            if (response.status.isSuccess()) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(mapHttpStatusToError(response.status.value, response.body<String>()))
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
        } catch (e: IOException) {
            SessionCache.updateOnline(false)
            NetworkResult.Error(NetworkError.NoInternet())
        } catch (e: Exception) {
            loggingRepository.logWarning("NetworkUtils changeGroupProfilePic failed: ${e.message}")
            NetworkResult.Error(NetworkError.Unknown(message = e.message))
        }
    }

    suspend fun changeGroupDescription(newDescription: String, groupId: String) : NetworkResult<Any, NetworkError> {
        return safePost(
            endpoint = "/groups/setdescription?groupid=$groupId",
            body = newDescription
        )
    }


    enum class GroupMemberAction {
        ADD_USER,
        REMOVE_USER,
        MAKE_ADMIN,
        REMOVE_ADMIN
    }

    @Serializable
    data class GroupActionRequest(
        val action: GroupMemberAction,
        val groupMemberId: String,
        val groupId: String
    )
    
    suspend fun changeGroupMembers(action: GroupMemberAction, memberId: String, groupId: String): NetworkResult<Any, NetworkError> {
        return safePost(
            endpoint = "/groups/changemembers",
            body = GroupActionRequest(
                action = action,
                groupMemberId = memberId,
                groupId = groupId
            )
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
    data class PollMessageRequest(
        val receiverId: String,
        val groupMessage: Boolean,
        val msgType: MessageType,
        val answerId: String?,
        val poll: PollCreateRequest
    )

    @Serializable
    data class PollCreateRequest(
        val title: String,
        val description: String?,

        val maxAnswers: Int?, // null = unlimited
        val customAnswersEnabled: Boolean,
        val maxAllowedCustomAnswers: Int?, // null = unlimited

        val visibility: PollVisibility,

        val closeDate: Long?,

        val voteOptions: List<PollVoteOptionCreateRequest>,
    )

    /**
     * Data class needed for appending options when creating a poll
     */
    @Serializable
    data class PollVoteOptionCreateRequest(
        //Ids get assigned by the server
        val text: String,
    )
    

    /**
     * Vote in a poll
     */
    @Serializable
    data class PollVoteRequest(
        val messageId: String,
        val id: String?, //Pass if available, else this is a new custom option
        val text: String?, //Pass if the id is null (New custom option with this text)
        val selected: Boolean, //Did the user select or unselect this item
    )


    @Serializable
    data class MessageResponse(
        val messageId: String, //Objectid
        val senderId: String,
        val receiverId: String,
        val groupMessage: Boolean,
        val msgType: MessageType,

        val content: String,
        val pollResponse: PollResponse?,

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


    suspend fun sendPollMessageToServer(empfaenger: String, gruppe: Boolean, content: PollCreateRequest, answerid: String?) : NetworkResult<MessageResponse, NetworkError> {
        val pollRequest = PollMessageRequest(
            receiverId = empfaenger,
            groupMessage = gruppe,
            msgType = MessageType.POLL,
            answerId = answerid,
            poll = content
        )

        println("PollMessageRequest: $pollRequest")

        return safePost(
            endpoint = "/messages/send/poll",
            body = pollRequest
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


    @Serializable
    data class EditMessageRequest(
        val messageId: String,
        val newContent: String,
    )

    suspend fun editMessage(messageId: String, newContent: String): NetworkResult<MessageResponse, RequestError> {
        return safePost(
            endpoint = "/messages/edit",
            body = EditMessageRequest(
                messageId = messageId,
                newContent = newContent
            )
        )
    }

    suspend fun deleteMessage(messageId: String): NetworkResult<Any, RequestError> {
        return safeDelete(
            endpoint = "/messages/delete?messageid=$messageId"
        )
    }

}
package org.lerchenflo.schneaggchatv3mp.datasource.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVisibility
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventJoinRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventJoinResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackCommentRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackCommentResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackCreateRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackEntryDetailResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackEntryResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackListResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackStatusRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackVoteRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.GameScoreResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.GlobalRankingResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.HighscoresResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.SubmitGameScoresBatchRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventParticipationRequest
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.MapEntryRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.MapEntryResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.MapSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.PollResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.SystemEventResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.SubmitGameScoreRequest
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.safeNetworkCall
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.PinnedChat
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.DEVICETYPE
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class NetworkUtils(
    private val httpClient: HttpClient,
    private val authHttpClient: HttpClient, //For auth without the bearer
    private val preferenceManager: Preferencemanager,
    private val loggingRepository: Logger,
    private val appVersion: AppVersion
) {


    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    // Without the bearer token, see authHttpClient
    private val authServer = ServerClient(authHttpClient, preferenceManager, loggingRepository)

    /** GETs [url] as given (not a server endpoint), without the bearer token. */
    private suspend inline fun <reified R> safeAuthGet(url: String): NetworkResult<R, NetworkingError> {
        return safeNetworkCall(loggingRepository) {
            authHttpClient.get(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }


    suspend fun testServer(serverUrl: String) : NetworkResult<String, NetworkingError> {
        return safeNetworkCall(loggingRepository) {
            authHttpClient.get("$serverUrl/public/test")
        }
    }


    suspend fun getChangeLog(githubUrl: String) : NetworkResult<String, NetworkingError> {
        return safeAuthGet(url = githubUrl)
    }

    suspend fun getGitHubVersionJson(url: String) : NetworkResult<String, NetworkingError> {
        return safeAuthGet(url = url)
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
    )

    @Serializable
    data class LoginRequest(
        val username: String,
        val password: String,
        val deviceName: String,
        val deviceType: DEVICETYPE
    )



    suspend fun login(username: String, password: String): NetworkResult<TokenPair, NetworkingError> {
        return authServer.safePost<LoginRequest, TokenPair>(
            endpoint = "/auth/login",
            body = LoginRequest(username = username, password = password, deviceName = appVersion.getDeviceName(), deviceType = appVersion.getDeviceType())
        )
    }

    suspend fun register(
        username: String,
        password: String,
        email: String,
        birthDate: String,
        profilePicBytes: ByteArray,
        phoneNumber: String? = null,
        language: String? = null,
        fileName: String = "profile.jpg"
    ): NetworkResult<Unit, NetworkingError> {
        return authServer.safePostMultipart(
            endpoint = "/auth/register",
            parts = {
                append("username", username)
                append("password", password)
                append("email", email)
                append("birthDate", birthDate)
                if (!phoneNumber.isNullOrBlank()) {
                    append("phoneNumber", phoneNumber)
                }
                if (!language.isNullOrBlank()) {
                    append("language", language)
                }
                appendProfilePic(profilePicBytes, fileName)
            }
        )
    }

    suspend fun sendEmailVerify(): NetworkResult<Unit, NetworkingError> {
        return server.safePost<String, Unit>(
            endpoint = "/users/verificationemail",
            body = ""
        )
    }


    @Serializable
    data class LogoutRequest(
        val refreshToken: String? = null,
        val notificationToken: String? = null,
        val isAndroid: Boolean? = null,
        val allDevices: Boolean = false,
    )

    /**
     * Ends this device's session on the server. Uses [authHttpClient]: the refresh token in the
     * body is what identifies the session, so an expired access token must not send this through
     * the Auth plugin and trigger a token refresh for a session we are about to kill anyway.
     *
     * [refreshToken] is null when the session is already dead (e.g. ended from another device's
     * device list) - the server then only removes the [notificationToken].
     */
    suspend fun logout(
        refreshToken: String?,
        notificationToken: String?,
        isAndroid: Boolean,
    ): NetworkResult<Unit, NetworkingError> {
        return authServer.safePost<LogoutRequest, Unit>(
            endpoint = "/auth/logout",
            body = LogoutRequest(
                refreshToken = refreshToken,
                notificationToken = notificationToken?.ifBlank { null },
                isAndroid = if (notificationToken.isNullOrBlank()) null else isAndroid,
            )
        )
    }

    /**
     * One logged-in device of the own account.
     * [deviceName] is the raw name the device sent at login (incl. its id suffix).
     */
    /**
     * Ends every session of the own account on the server, this device's included, and removes
     * all of the account's push tokens. Unlike [logout] this goes through the bearer client: the
     * server identifies the account by the refresh token or, failing that, the access token - so
     * it must not depend on the refresh token alone.
     */
    suspend fun logoutAllDevices(refreshToken: String?): NetworkResult<Unit, NetworkingError> {
        return server.safePost<LogoutRequest, Unit>(
            endpoint = "/auth/logout",
            body = LogoutRequest(
                refreshToken = refreshToken,
                allDevices = true,
            )
        )
    }

    @Serializable
    data class SessionResponse(
        val id: String,
        val deviceName: String? = null,
        val deviceType: DEVICETYPE? = null,
        val createdAt: Long,
        val lastUsedAt: Long,
    )

    suspend fun getSessions(): NetworkResult<List<SessionResponse>, NetworkingError> {
        return server.safeGet(endpoint = "/sessions")
    }

    /**
     * Logs one of the own devices out remotely.
     */
    suspend fun endSession(sessionId: String): NetworkResult<Unit, NetworkingError> {
        return server.safeDelete(endpoint = "/sessions/$sessionId")
    }

    @Serializable
    data class NotificationTokenRequest(
        val token: String,
        val isAndroid: Boolean
    )

    suspend fun setNotificationToken(token: String, isAndroid: Boolean) : NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/users/setnotificationtoken",
            body = NotificationTokenRequest(
                token = token,
                isAndroid = isAndroid
            )
        )
    }





    /*
    **************************************************************************

    User

    **************************************************************************
     */
    @Serializable
    data class IdTimeStamp(val id: String, val timeStamp: String)

    suspend fun getProfilePicForUserId(userId: String) : NetworkResult<ByteArray, NetworkingError> {
        return server.safeGet(
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
        BLOCKED
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

            //Optional phone number, shared with accepted friends
            val phoneNumber: String? = null,

            val nickName: String?,

            //Do i share the location with this friend
            val shareLocation: Boolean = false,
            //Do i share speed + heading (live driving telemetry) with this friend
            val shareSpeedHeading: Boolean = false,
            //Whether i share my snail trail (full 24h history) with this friend
            val shareSnailTrail: Boolean = false,

            //Do i allow this friend to wake me (alarm ringtone on my devices)
            val allowWake: Boolean = false,

            //Epoch millis this friend was last seen online, null if unknown/never
            val lastSeen: Long? = null,

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

            //Optional phone number, only visible to yourself and accepted friends
            val phoneNumber: String? = null,

            //Custom to own user response:
            val email: String,
            val emailVerifiedAt: Long?,
            val createdAt: Long,

            //Do i have location sharing enabled
            val locationShared: Boolean = false,

            //Master switch: do i allow anyone to wake me at all
            val allowWakeGlobal: Boolean = false,

            //Per-user app settings (theme, language, pinned chats, ...), synced across devices
            val settings: PersonalUserSettings = PersonalUserSettings(),

        ) : UserResponse
    }

    /**
     * Per-user app settings, synced across every device of this account. Mirrors the server's
     * PersonalUserSettings 1:1 - theme/language/mapStyle are sent as the enum's [Enum.name], since
     * the server treats them as opaque strings and never interprets them.
     */
    @Serializable
    data class PersonalUserSettings(
        val mdFormat: Boolean = true,
        val highlightTodaysMessageTimestamp: Boolean = false,

        val theme: String = "SYSTEM",
        val language: String = "SYSTEM",

        val mergeMapLocations: Boolean = true,
        val mergeMapUsers: Boolean = true,
        val mapStyle: String = "LIBERTY",

        val pinnedChats: List<PinnedChat> = emptyList(),

        // Quick reaction emojis for the message long-press popup. Null means "never customised",
        // so the client falls back to DEFAULT_QUICK_REACTIONS and keeps following that set as it
        // changes; an empty list is a deliberate "show none" and is stored as such.
        val quickReactions: List<String>? = null,

        val developerSettings: Boolean = false,

        // Epoch millis of the last time the contribute popup was shown on any device.
        // 0 means never seeded - the server backfills this at registration and via migration.
        val lastContributePopupShown: Long = 0L,
    )

    /**
     * Partial update for [PersonalUserSettings] - every field nullable, a null means "leave
     * unchanged" on the server, so only the fields that actually changed need to be sent.
     */
    @Serializable
    data class UserSettingsRequest(
        val mdFormat: Boolean? = null,
        val highlightTodaysMessageTimestamp: Boolean? = null,
        val theme: String? = null,
        val language: String? = null,
        val mergeMapLocations: Boolean? = null,
        val mergeMapUsers: Boolean? = null,
        val mapStyle: String? = null,
        val pinnedChats: List<PinnedChat>? = null,
        val quickReactions: List<String>? = null,
        // Null already means "leave unchanged" here, so resetting the list back to the app
        // defaults (a stored null) needs its own flag.
        val resetQuickReactions: Boolean? = null,
        val developerSettings: Boolean? = null,
        val lastContributePopupShown: Long? = null,
    )

    suspend fun updateSettings(request: UserSettingsRequest): NetworkResult<Unit, NetworkingError> {
        return server.safePost(
            endpoint = "/users/settings",
            body = request
        )
    }

    suspend fun userIdSync(userIds: List<IdTimeStamp>) : NetworkResult<UserSyncResponse, NetworkingError> {
        return server.safePost(
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

    suspend fun getAvailableUsers(searchterm: String) : NetworkResult<List<NewFriendsUserResponse>, NetworkingError> {
        return server.safeGet(
            endpoint = "/users/availableusers?searchterm=$searchterm"
        )
    }

    suspend fun sendFriendRequest(friendId: String) : NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/users/addfriend/$friendId",
            body = ""
        )
    }

    suspend fun denyFriendRequest(friendId: String) : NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/users/denyfriend/$friendId",
            body = ""
        )
    }

    suspend fun removeFriend(friendId: String) : NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/users/removefriend/$friendId",
            body = ""
        )
    }

    suspend fun changeUsername(newUsername: String) : NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/users/changeusername",
            body = newUsername
        )
    }

    @Serializable
    data class PasswordChangeRequest(
        val oldPassword: String,
        val newPassword: String
    )

    suspend fun changePassword(oldPassword: String, newPassword: String) : NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/users/changepassword",
            body = PasswordChangeRequest(
                oldPassword = oldPassword,
                newPassword = newPassword
            )
        )
    }

    suspend fun changeProfilePic(newProfilePic: ByteArray, fileName: String = "profile.jpg"): NetworkResult<Unit, NetworkingError> {
        return server.safePostMultipart(
            endpoint = "/users/setprofilepic",
            parts = { appendProfilePic(newProfilePic, fileName) }
        )
    }

    @Serializable
    data class UserRequest(
        val userId: String,
        val newDescription: String?,
        val newStatus: String?,
        val newEmail: String?,
        val newBirthDate: String?,
        val newNickName: String?,
        val newPhoneNumber: String? = null,
    )

    suspend fun changeProfile(userId: String, newStatus: String?, newDescription: String?, newEmail: String?, newBirthDate: String?, newNickName: String?, newPhoneNumber: String? = null): NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/users/changeprofile",
            body = UserRequest(
                userId = userId,
                newDescription = newDescription,
                newStatus = newStatus,
                newEmail = newEmail,
                newBirthDate = newBirthDate,
                newNickName = newNickName,
                newPhoneNumber = newPhoneNumber,
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
            val messageType: MessageType,
            val groupName: String,
            val content: String
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


    suspend fun getProfilePicForGroupId(groupId: String) : NetworkResult<ByteArray, NetworkingError> {
        return server.safeGet(
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
        val expiresAt: Long?,
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
    ): NetworkResult<GroupResponse, NetworkingError> {
        return server.safePostMultipart(
            endpoint = "/groups/create",
            parts = {
                append("name", name)
                append("description", description)
                append("memberlist", memberIds.joinToString(","))
                appendProfilePic(profilePicBytes, fileName = "image.png")
            }
        )
    }

    @Serializable
    data class GroupSyncResponse(
        val updatedGroups: List<GroupResponse>,
        val deletedGroups: List<String>
    )

    suspend fun groupIdSync(groupIds: List<IdTimeStamp>) : NetworkResult<GroupSyncResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/groups/sync",
            body = groupIds
        )
    }


    suspend fun changeGroupProfilePic(newProfilePic: ByteArray, groupId: String, fileName: String = "profile.jpg"): NetworkResult<Unit, NetworkingError> {
        return server.safePostMultipart(
            endpoint = "/groups/setprofilepic?groupid=$groupId",
            parts = { appendProfilePic(newProfilePic, fileName) }
        )
    }

    suspend fun changeGroupDescription(newDescription: String, groupId: String) : NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/groups/setdescription?groupid=$groupId",
            body = newDescription
        )
    }

    suspend fun changeGroupName(newName: String, groupId: String) : NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/groups/setGroupName?groupid=$groupId",
            body = newName
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
    
    suspend fun changeGroupMembers(action: GroupMemberAction, memberId: String, groupId: String): NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/groups/changemembers",
            body = GroupActionRequest(
                action = action,
                groupMemberId = memberId,
                groupId = groupId
            )
        )
    }

    @Serializable
    data class SetGroupExpiryRequest(
        val groupId: String,
        val expiresAt: Long?
    )

    //Pass null to clear the expiry (group never auto-expires)
    suspend fun changeGroupExpiry(groupId: String, expiresAt: Long?): NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/groups/setexpiry",
            body = SetGroupExpiryRequest(
                groupId = groupId,
                expiresAt = expiresAt
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
        val clientMessageId: String,
    )

    @Serializable
    data class ImageMessageRequest(
        val messageId: String?, //Objectid

        val receiverId: String,
        val groupMessage: Boolean,
        val msgType: MessageType,
        val content: String,
        val answerId: String?,
        val clientMessageId: String,
    )
    @Serializable
    data class AudioMessageRequest(
        val messageId: String?, //Objectid

        val receiverId: String,
        val groupMessage: Boolean,
        val msgType: MessageType,
        //val content: String,
        val answerId: String?,
        val clientMessageId: String,
    )

    @Serializable
    data class PollMessageRequest(
        val receiverId: String,
        val groupMessage: Boolean,
        val msgType: MessageType,
        val answerId: String?,
        val poll: PollCreateRequest,
        val clientMessageId: String,
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

        val allowDeleteOptions: Boolean = false,
        val showCheckboxes: Boolean = true,

        //Only read on sub polls: may users who didn't pick the parent option look at it
        val visibleToAll: Boolean = true,
    )

    /**
     * Data class needed for appending options when creating a poll
     */
    @Serializable
    data class PollVoteOptionCreateRequest(
        //Ids get assigned by the server
        val text: String,
        val maxVoters: Int? = null, // null = unlimited
        val subPoll: PollCreateRequest? = null, //Follow-up poll for users who pick this option
    )
    

    /**
     * Vote in a poll
     */
    @Serializable
    data class PollVoteRequest(
        val messageId: String,
        val id: String?, //Pass if available, else this is a new custom option
        val text: String?, //Pass if the id is null (New custom option with this text)
        val maxAllowedAnswers: Int?, //Pass if the user creates a custom entry and the poll supports restricting the entries for votes
        val selected: Boolean, //Did the user select or unselect this item
        val parentOptionId: String? = null, //New custom option only: the option whose sub poll gets it (null = root poll)
    )


    suspend fun votePoll(pollVoteRequest: PollVoteRequest) : NetworkResult<MessageResponse, NetworkingError>{
        return server.safePost(
            endpoint = "/messages/pollvote",
            body = pollVoteRequest,
        )
    }

    /**
     * Delete a vote option from a poll
     */
    @Serializable
    data class PollOptionDeleteRequest(
        val messageId: String,
        val optionId: String,
    )

    suspend fun deletePollOption(request: PollOptionDeleteRequest) : NetworkResult<MessageResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/messages/polloption/delete",
            body = request,
        )
    }


    @Serializable
    data class MessageResponse(
        val messageId: String, //Objectid
        val senderId: String,
        val receiverId: String,
        val groupMessage: Boolean,
        val msgType: MessageType,

        val content: String,
        val pollResponse: PollResponse?,
        val systemEventResponse: SystemEventResponse? = null,

        val answerId: String?,

        val sendDate: Long,
        val lastChanged: Long,
        val deleted: Boolean,
        val edited: Boolean = false,
        val readers: List<ReaderResponse>,
        val reactions: List<ReactionResponse> = emptyList(),
        val version: Long = 0L,
        // Only non-null when this response is about our own sent message - lets sync/socket
        // reconciliation find a still-pending local row before the send's own response arrives.
        val clientMessageId: String? = null,
    )

    @Serializable
    data class ReaderResponse(
        val userId: String,
        val readAt: Long
    )

    @Serializable
    data class ReactionResponse(
        val userId: String,
        val content: String,
        val reactedAt: Long = 0L,
    )

    suspend fun sendTextMessageToServer(messageId: String?, empfaenger: String, gruppe: Boolean, content: String, answerid: String?, clientMessageId: String) : NetworkResult<MessageResponse, NetworkingError> {
        val messageRequest = MessageRequest(
            messageId = messageId,
            receiverId = empfaenger,
            groupMessage = gruppe,
            msgType = MessageType.TEXT,
            content = content,
            answerId = answerid,
            clientMessageId = clientMessageId,
        )

        println("MessageRequest: $messageRequest")

        return server.safePost(
            endpoint = "/messages/send/text",
            body = messageRequest
        )
    }

    suspend fun sendImageMessageToServer(
        empfaenger: String,
        gruppe: Boolean,
        image: ByteArray,
        text: String,
        answerid: String?,
        clientMessageId: String
    ): NetworkResult<MessageResponse, NetworkingError> {

        val messageRequest = ImageMessageRequest(
            messageId = null,
            receiverId = empfaenger,
            groupMessage = gruppe,
            msgType = MessageType.IMAGE,
            content = text,
            answerId = answerid,
            clientMessageId = clientMessageId,
        )

        return server.safePostMultipart(
            endpoint = "/messages/send/image",
            parts = {
                appendFile(key = "image", bytes = image, fileName = "image.jpg", contentType = "image/jpeg")
                appendJsonPart(key = "request", serializer = ImageMessageRequest.serializer(), value = messageRequest)
            }
        )
    }

    suspend fun sendAudioMessageToServer(
        empfaenger: String,
        gruppe: Boolean,
        audio: ByteArray,
        answerid: String?,
        clientMessageId: String
    ): NetworkResult<MessageResponse, NetworkingError> {

        val messageRequest = AudioMessageRequest(
            messageId = null,
            receiverId = empfaenger,
            groupMessage = gruppe,
            msgType = MessageType.AUDIO,
            answerId = answerid,
            clientMessageId = clientMessageId,
        )

        return server.safePostMultipart(
            endpoint = "/messages/send/audio",
            parts = {
                appendFile(key = "audio", bytes = audio, fileName = "message_audio.m4a", contentType = "audio/mp4")
                appendJsonPart(key = "request", serializer = AudioMessageRequest.serializer(), value = messageRequest)
            }
        )
    }


    suspend fun sendPollMessageToServer(empfaenger: String, gruppe: Boolean, content: PollCreateRequest, answerid: String?, clientMessageId: String) : NetworkResult<MessageResponse, NetworkingError> {
        val pollRequest = PollMessageRequest(
            receiverId = empfaenger,
            groupMessage = gruppe,
            msgType = MessageType.POLL,
            answerId = answerid,
            poll = content,
            clientMessageId = clientMessageId,
        )

        println("PollMessageRequest: $pollRequest")

        return server.safePost(
            endpoint = "/messages/send/poll",
            body = pollRequest
        )
    }





    @Serializable
    data class MessageSyncResponse(
        val updatedMessages: List<MessageResponse>,
        val deletedMessages: List<String>,
        val newVersion: Long,
        val moreMessages: Boolean
    )

    suspend fun messageSync(since: Long) : NetworkResult<MessageSyncResponse, NetworkingError>{
        return server.safeGet(
            endpoint = "/messages/sync?since=$since&page_size=400",
        )
    }

    suspend fun setMessagesRead(chatId: String, group: Boolean, timeStamp: Long) : NetworkResult<String, NetworkingError>{
        return server.safePost(
            endpoint = "/messages/setread?userid=$chatId&group=$group&timestamp=$timeStamp",
            body = "",
        )
    }


    @Serializable
    data class EditMessageRequest(
        val messageId: String,
        val newContent: String,
    )

    suspend fun editMessage(messageId: String, newContent: String): NetworkResult<MessageResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/messages/edit",
            body = EditMessageRequest(
                messageId = messageId,
                newContent = newContent
            )
        )
    }

    suspend fun deleteMessage(messageId: String): NetworkResult<Any, NetworkingError> {
        return server.safeDelete(
            endpoint = "/messages/delete?messageid=$messageId"
        )
    }

    @Serializable
    data class ReactionRequest(
        val messageId: String,
        val content: String
    )

    suspend fun reactToMessage(messageId: String, content: String): NetworkResult<MessageResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/messages/react",
            body = ReactionRequest(messageId = messageId, content = content)
        )
    }

    suspend fun getImageForImageMessage(messageId: String) : NetworkResult<ByteArray, NetworkingError> {
        return server.safeGet(
            endpoint = "/messages/images/$messageId"
        )
    }

    suspend fun getAudioForAudioMessage(messageId: String) : NetworkResult<ByteArray, NetworkingError> {
        return server.safeGet(
            endpoint = "/messages/audios/$messageId"
        )
    }

    // ─── Map ──────────────────────────────────────────────────────────────────

    suspend fun mapSync(
        entries: List<IdTimeStamp>,
        page: Int,
    ): NetworkResult<MapSyncResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/map/sync?page=$page&page_size=400",
            body = entries,
        )
    }

    suspend fun upsertMapEntry(
        request: MapEntryRequest,
    ): NetworkResult<MapEntryResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/map/upsert",
            body = request)
    }

    suspend fun deleteMapEntry(entryId: String): NetworkResult<Unit, NetworkingError> {
        return server.safeDelete(endpoint = "/map/delete?entryid=$entryId")
    }

    // ─── Events ───────────────────────────────────────────────────────────────

    suspend fun eventSync(
        entries: List<IdTimeStamp>,
        page: Int,
    ): NetworkResult<EventSyncResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/events/sync?page=$page&page_size=400",
            body = entries,
        )
    }

    suspend fun upsertEvent(
        request: EventRequest,
        profilePic: ByteArray? = null,
    ): NetworkResult<EventResponse, NetworkingError> {
        return server.safePostMultipart(
            endpoint = "/events/upsert",
            parts = {
                appendJsonPart(key = "request", serializer = EventRequest.serializer(), value = request)
                profilePic?.let {
                    appendProfilePic(it, fileName = "eventtype.png", contentType = "image/png")
                }
            }
        )
    }

    suspend fun joinEvent(
        eventId: String
    ): NetworkResult<EventJoinResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/events/join",
            body = EventJoinRequest(
                eventId = eventId
            ),
        )
    }

    suspend fun setEventParticipation(
        eventId: String,
        status: EventParticipationStatus,
    ): NetworkResult<EventResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/events/participation",
            body = EventParticipationRequest(
                eventId = eventId,
                status = status,
            )
        )
    }

    suspend fun detachEvent(eventId: String, deleteGroup: Boolean = false, deleteEvent: Boolean = false): NetworkResult<Unit, NetworkingError> {
        return server.safeDelete(endpoint = "/events/delete?eventid=$eventId&deletegroup=$deleteGroup&deleteevent=$deleteEvent")
    }

    // Location data itself is now pushed/pulled over the WebSocket (see SocketConnectionMessage:
    // LocationUpdate / FriendLocationChange / FriendLocationsSnapshot), not via HTTP.

    @Serializable
    data class LocationShareRequest(
        val friendId: String,
        // Full desired per-friend map-sharing state - send all of these every time.
        val share: Boolean,
        val shareSpeedHeading: Boolean = false,
        val shareSnailTrail: Boolean = false,
    )

    /** Per-friend toggle: what we share with this specific friend (location, speed/heading, snail trail). */
    suspend fun shareLocation(
        friendId: String,
        share: Boolean,
        shareSpeedHeading: Boolean = false,
        shareSnailTrail: Boolean = false,
    ): NetworkResult<Unit, NetworkingError> {
        return server.safePost(
            endpoint = "/users/sharelocation",
            body = LocationShareRequest(friendId, share, shareSpeedHeading, shareSnailTrail)
        )
    }

    // ─── Wake ─────────────────────────────────────────────────────────────────

    @Serializable
    data class WakePermissionRequest(
        val friendId: String,
        val allowWake: Boolean,
    )

    /** Per-friend toggle: may this friend wake us (play an alarm on our devices). */
    suspend fun setWakePermission(friendId: String, allowWake: Boolean): NetworkResult<Unit, NetworkingError> {
        return server.safePost(
            endpoint = "/users/setwakepermission",
            body = WakePermissionRequest(friendId, allowWake)
        )
    }

    @Serializable
    data class WakeGlobalRequest(
        val enabled: Boolean,
    )

    /** Master switch: while off nobody can wake us, whatever the per-friend toggles say. */
    suspend fun setWakeGlobal(enabled: Boolean): NetworkResult<Unit, NetworkingError> {
        return server.safePost(
            endpoint = "/users/setwakeglobal",
            body = WakeGlobalRequest(enabled)
        )
    }

    /**
     * Mirrors the server's WakeOutcome - why a wake did or did not reach anybody.
     * Waking a non-friend is not represented here: the server rejects it with 403.
     */
    @Serializable
    enum class WakeOutcome {
        WOKEN,
        NOT_ALLOWED,
        NO_DEVICES,
    }

    @Serializable
    data class WakeRequest(
        val targetId: String,
        val isGroup: Boolean,
        val reason: String,
    )

    @Serializable
    data class WakeResponse(
        val outcome: WakeOutcome,
        val userCount: Int = 0,
        val deviceCount: Int = 0,
        val skippedCount: Int = 0,
    )

    /** Wake a friend or a group - plays an alarm on every consenting Android device. */
    suspend fun sendWake(targetId: String, isGroup: Boolean, reason: String): NetworkResult<WakeResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/wake/send",
            body = WakeRequest(targetId, isGroup, reason)
        )
    }

    // ─── Games ────────────────────────────────────────────────────────────────

    suspend fun getGameHighscores(gameId: String, difficulty: String, period: String): NetworkResult<HighscoresResponse, NetworkingError> {
        return server.safeGet(endpoint = "/games/highscores?gameid=$gameId&difficulty=$difficulty&period=$period")
    }

    /** Cross-game leaderboard: percentile points summed over every board the user played. */
    suspend fun getGlobalRanking(period: String): NetworkResult<GlobalRankingResponse, NetworkingError> {
        return server.safeGet(endpoint = "/games/globalranking?period=$period")
    }

    suspend fun submitGameScore(
        request: SubmitGameScoreRequest,
    ): NetworkResult<GameScoreResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/games/upsert",
            body = request)
    }

    suspend fun submitGameScoresBatch(
        request: SubmitGameScoresBatchRequest,
    ): NetworkResult<List<GameScoreResponse>, NetworkingError> {
        return server.safePost(
            endpoint = "/games/upsertbatch",
            body = request)
    }

    // ─── Recap ────────────────────────────────────────────────────────────────

    suspend fun getRecap(year: Int? = null): NetworkResult<RecapResponse, NetworkingError> {
        val endpoint = if (year != null) "/recap?year=$year" else "/recap"
        return server.safeGet(endpoint)
    }

    // ─── Feedback ─────────────────────────────────────────────────────────────

    suspend fun getFeedbackList(type: FeedbackType): NetworkResult<FeedbackListResponse, NetworkingError> {
        return server.safeGet(endpoint = "/feedback/list?type=${type.name}")
    }

    suspend fun getFeedbackDetails(entryId: String): NetworkResult<FeedbackEntryDetailResponse, NetworkingError> {
        return server.safeGet(endpoint = "/feedback/details?entryid=$entryId")
    }

    suspend fun createFeedback(request: FeedbackCreateRequest): NetworkResult<FeedbackEntryResponse, NetworkingError> {
        return server.safePost(endpoint = "/feedback/create", body = request)
    }

    suspend fun voteFeedback(request: FeedbackVoteRequest): NetworkResult<FeedbackEntryResponse, NetworkingError> {
        return server.safePost(endpoint = "/feedback/vote", body = request)
    }

    suspend fun commentFeedback(request: FeedbackCommentRequest): NetworkResult<FeedbackCommentResponse, NetworkingError> {
        return server.safePost(endpoint = "/feedback/comment", body = request)
    }

    suspend fun deleteFeedback(entryId: String): NetworkResult<Unit, NetworkingError> {
        return server.safeDelete(endpoint = "/feedback/delete?entryid=$entryId")
    }

    suspend fun deleteFeedbackComment(commentId: String): NetworkResult<Unit, NetworkingError> {
        return server.safeDelete(endpoint = "/feedback/deletecomment?commentid=$commentId")
    }

    suspend fun setFeedbackStatus(request: FeedbackStatusRequest): NetworkResult<FeedbackEntryResponse, NetworkingError> {
        return server.safePost(endpoint = "/feedback/status", body = request)
    }
}

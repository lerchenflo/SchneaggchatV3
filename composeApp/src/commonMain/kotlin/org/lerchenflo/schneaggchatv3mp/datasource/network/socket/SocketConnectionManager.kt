package org.lerchenflo.schneaggchatv3mp.datasource.network.socket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.AppJson
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.TokenManager
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.new_friend_accepted_noti
import schneaggchatv3mp.composeapp.generated.resources.new_friend_accepted_noti_body
import schneaggchatv3mp.composeapp.generated.resources.new_friend_request_noti
import schneaggchatv3mp.composeapp.generated.resources.new_friend_request_noti_body
import kotlin.collections.map

/**
 * Socket connection manager for real-time communication
 * Integrates with existing Ktor HTTP client infrastructure
 */
class SocketConnectionManager(
    private val httpClient: HttpClient,
    private val loggingRepository: LoggingRepository,
    private val appRepository: AppRepository,

    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository
) {

    companion object {
        fun getSocketUrl(url: String): String {
            // Remove trailing slashes
            val cleanUrl = url.trimEnd('/')

            // Convert protocol
            val wsUrl = when {
                cleanUrl.startsWith("https://") -> cleanUrl.replaceFirst("https://", "wss://")
                cleanUrl.startsWith("http://") -> cleanUrl.replaceFirst("http://", "ws://")
                cleanUrl.startsWith("wss://") || cleanUrl.startsWith("ws://") -> cleanUrl
                else -> {
                    println("Invalid URL format: $url. Expected http:// or https://")
                    // Assume https if no protocol specified
                    "wss://$cleanUrl"
                }
            }

            // Add /ws path if not already present
            val finalUrl = if (wsUrl.endsWith("/ws")) {
                wsUrl
            } else {
                "$wsUrl/ws"
            }

            println("WebSocket URL: $finalUrl")
            return finalUrl
        }
    }




    private var currentConnection: SocketConnection? = null
    
    /**
     * Establish WebSocket connection with authentication
     */
    suspend fun connect(
        serverUrl: String,
        onError: (Throwable) -> Unit,
        onClose: () -> Unit
    ): Boolean {
        return try {
            // Close existing connection if th e serverurl changed
            if (currentConnection?.serverUrl != serverUrl) {
                currentConnection?.close()
            }

            // Create WebSocket connection
            val connection = SocketConnection(
                httpClient = httpClient,
                serverUrl = serverUrl,
                onMessage = {
                    CoroutineScope(Dispatchers.IO).launch {
                        handleRemoteMessage(it)
                    }
                },
                onError = onError,
                onClose = onClose
            )
            
            if (connection.connect()) {
                currentConnection = connection
                //loggingRepository.logInfo("WebSocket connected to $serverUrl")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            loggingRepository.logError("Failed to connect WebSocket: ${e.message}")
            onError(e)
            false
        }
    }
    
    /**
     * Send message through current connection
     */
    suspend fun sendMessage(message: String): Boolean {
        return currentConnection?.send(message) ?: false
    }

    suspend fun handleRemoteMessage(message: String) {
        println("Recieved socket message: $message")
        try {
            val socketMessage = AppJson.instance.decodeFromString<SocketConnectionMessage>(message)

            when (socketMessage) {

                //A message got updated
                is SocketConnectionMessage.MessageChange -> {

                    val existing = messageRepository.getMessageById(socketMessage.message.messageId)
                    val message = Message(
                        localPK = existing?.localPK ?: 0L,
                        id = socketMessage.message.messageId,
                        msgType = socketMessage.message.msgType,
                        content = socketMessage.message.content,
                        senderId = socketMessage.message.senderId,
                        receiverId = socketMessage.message.receiverId,
                        sendDate = socketMessage.message.sendDate.toString(),
                        changeDate = socketMessage.message.lastChanged.toString(),
                        deleted = false,
                        groupMessage = socketMessage.message.groupMessage,
                        answerId = socketMessage.message.answerId,
                        sent = true,
                        myMessage = socketMessage.message.senderId == SessionCache.getOwnIdValue(),
                        readByMe =socketMessage.message.readers.any { it.userId == SessionCache.ownId.value },
                        readers = socketMessage.message.readers.map {
                            MessageReader(
                                readerEntryId = 0L,
                                messageId = socketMessage.message.messageId,
                                readerId = it.userId,
                                readDate = it.readAt.toString()
                            )
                        }
                    )

                    if (socketMessage.deleted) {
                        messageRepository.deleteMessage(socketMessage.message.messageId)
                    } else {
                        messageRepository.upsertMessage(message)
                    }

                    //THis is a new message, show a notification
                    if (socketMessage.newMessage) {
                        NotificationManager.showNotification(message)
                    }
                }

                //A user got updated
                is SocketConnectionMessage.UserChange -> {
                    if (socketMessage.deleted) {
                        userRepository.deleteUser(socketMessage.user.id)
                    } else {
                        when (val newUser = socketMessage.user) {
                            is NetworkUtils.UserResponse.FriendUserResponse -> {
                                val existing = userRepository.getUserById(newUser.id)
                                userRepository.upsertUser(UserDto(
                                    id = newUser.id,
                                    changedate = newUser.updatedAt,
                                    name = newUser.username,
                                    description = newUser.userDescription,
                                    status = newUser.userStatus,
                                    birthDate = newUser.birthDate,
                                    frienshipStatus = NetworkUtils.FriendshipStatus.ACCEPTED,
                                    requesterId = newUser.requesterId,

                                    // Preserve existing values:
                                    locationLat = existing?.locationLat,
                                    locationLong = existing?.locationLong,
                                    locationDate = existing?.locationDate,
                                    locationShared = existing?.locationShared ?: false,
                                    wakeupEnabled = existing?.wakeupEnabled ?: false,
                                    lastOnline = existing?.lastOnline,
                                    notisMuted = existing?.notisMuted ?: false,
                                    email = null,
                                    emailVerifiedAt = null,
                                    createdAt = null,
                                    profilePictureUrl = ""
                                ))
                            }
                            is NetworkUtils.UserResponse.SelfUserResponse -> {
                                val existing = userRepository.getUserById(newUser.id)
                                userRepository.upsertUser(UserDto(
                                    id = newUser.id,
                                    changedate = newUser.updatedAt,
                                    name = newUser.username,
                                    description = newUser.userDescription,
                                    status = newUser.userStatus,
                                    birthDate = newUser.birthDate,

                                    // Preserve existing values:
                                    locationLat = existing?.locationLat,
                                    locationLong = existing?.locationLong,
                                    locationDate = existing?.locationDate,
                                    locationShared = existing?.locationShared ?: false,
                                    wakeupEnabled = existing?.wakeupEnabled ?: false,
                                    lastOnline = existing?.lastOnline,
                                    frienshipStatus = null,
                                    requesterId = null,
                                    notisMuted = false,
                                    email = newUser.email,
                                    emailVerifiedAt = newUser.emailVerifiedAt,
                                    createdAt = newUser.createdAt,
                                    profilePictureUrl = ""

                                ))
                            }
                            is NetworkUtils.UserResponse.SimpleUserResponse -> {
                                userRepository.upsertUser(UserDto(
                                    id = newUser.id,
                                    changedate = newUser.updatedAt,
                                    name = newUser.username,
                                    description = null,
                                    status = null,

                                    locationLat = null,
                                    locationLong = null,
                                    locationDate = null,
                                    locationShared = false,
                                    wakeupEnabled = false,
                                    lastOnline = null,
                                    frienshipStatus = newUser.friendShipStatus,
                                    requesterId = newUser.requesterId,
                                    notisMuted = false,
                                    birthDate = null,
                                    email = null,
                                    emailVerifiedAt = null,
                                    createdAt = null,
                                    profilePictureUrl = ""
                                ))


                            }
                        }

                        appRepository.getProfilePicturesForUserIds(listOf(socketMessage.user.id))
                    }
                }

                //A group got updated
                is SocketConnectionMessage.GroupChange -> {
                    if (socketMessage.deleted) {
                        groupRepository.deleteGroup(socketMessage.group.id)
                    } else {

                        val existing = groupRepository.getGroupById(socketMessage.group.id)

                        groupRepository.upsertGroup(Group(
                            id = socketMessage.group.id,
                            name = socketMessage.group.name,
                            profilePictureUrl = "",
                            description = socketMessage.group.description,
                            createDate = socketMessage.group.createdAt,
                            changedate = socketMessage.group.updatedAt,
                            notisMuted = existing?.notisMuted ?: false,
                            members = socketMessage.group.members.map { groupMemberresp ->
                                GroupMember(
                                    groupId = socketMessage.group.id,
                                    userId = groupMemberresp.userid,
                                    joinDate = groupMemberresp.joinedAt,
                                    admin = groupMemberresp.admin,
                                    color = groupMemberresp.color,
                                    memberName = groupMemberresp.memberName
                                )
                            }
                        ))
                        appRepository.getProfilePicturesForGroupIds(listOf(socketMessage.group.id))
                    }
                }

                //Friend request was sent
                is SocketConnectionMessage.FriendRequest -> {
                    appRepository.dataSync()
                    if (socketMessage.accepted) {
                        NotificationManager.showNotification(
                            titletext = getString(Res.string.new_friend_accepted_noti),
                            bodytext = getString(Res.string.new_friend_accepted_noti_body, socketMessage.requestingUserName)
                        )
                    } else {
                        NotificationManager.showNotification(
                            titletext = getString(Res.string.new_friend_request_noti, socketMessage.requestingUserName),
                            bodytext = getString(Res.string.new_friend_request_noti_body, socketMessage.requestingUserName)
                        )
                    }

                }


            }
        } catch (e: Exception) {
            println("Failed to deserialize socket message: ${e.message}")
        }
    }
    
    /**
     * Close current connection
     */
    suspend fun close() {
        println("Closing socket connection")
        currentConnection?.close()
        currentConnection = null
    }
    
    /**
     * Check if connection is active
     */
    fun isConnected(): Boolean = currentConnection?.isActive() ?: false
}

/**
 * Individual WebSocket connection handler
 */
private class SocketConnection(
    private val httpClient: HttpClient,
    val serverUrl: String,
    private val onMessage: (String) -> Unit,
    private val onError: (Throwable) -> Unit,
    private val onClose: () -> Unit
) {
    private var session: WebSocketSession? = null
    private val _isActive = MutableStateFlow(false)

    suspend fun connect(): Boolean {
        return try {
            coroutineScope {
                async(Dispatchers.IO) {
                    try {
                        httpClient.webSocket(
                            request = {
                                url(serverUrl)
                            }
                        ) {
                            session = this
                            _isActive.value = true

                            try {
                                incoming.receiveAsFlow()
                                    .filterIsInstance<Frame.Text>()
                                    .map { it.readText() }
                                    .collect { message ->
                                        onMessage(message)
                                    }
                            } catch (e: Exception) {
                                onError(e)
                            } finally {
                                _isActive.value = false
                                session = null
                                onClose()
                            }
                        }
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }.await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun send(message: String): Boolean {
        return try {
            session?.send(Frame.Text(message)) ?: return false
            true
        } catch (e: Exception) {
            onError(e)
            false
        }
    }

    suspend fun close() {
        try {
            session?.close()
            _isActive.value = false
            session = null
        } catch (e: Exception) {
            // Ignore close errors
        }
    }

    fun isActive(): Boolean = _isActive.value
}
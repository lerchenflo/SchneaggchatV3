package org.lerchenflo.schneaggchatv3mp.datasource.network.socket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
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
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager.NotiId
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager.NotiIdType
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.new_friend_accepted_noti
import schneaggchatv3mp.composeapp.generated.resources.new_friend_accepted_noti_body
import schneaggchatv3mp.composeapp.generated.resources.new_friend_request_noti
import schneaggchatv3mp.composeapp.generated.resources.new_friend_request_noti_body

@Serializable()
sealed interface SocketConnectionMessage {

    @Serializable
    @SerialName("messagechange")
    data class MessageChange(
        val message: NetworkUtils.MessageResponse,
        val newMessage: Boolean,
        val deleted: Boolean
    ) : SocketConnectionMessage

    @Serializable
    @SerialName("userchange")

    data class UserChange(val user: NetworkUtils.UserResponse, val deleted: Boolean) : SocketConnectionMessage

    data class GroupChange(val group: NetworkUtils.GroupResponse, val deleted: Boolean) : SocketConnectionMessage

    @Serializable
    @SerialName("friendrequest")
    data class FriendRequest(
        val requestingUser: String,
        val requestingUserName: String,
        val accepted: Boolean
    ) : SocketConnectionMessage
}


suspend fun handleSocketConnectionMessage(message: String) {

    val appRepository = KoinPlatform.getKoin().get<AppRepository>()
    val userRepository = KoinPlatform.getKoin().get<UserRepository>()
    val messageRepository = KoinPlatform.getKoin().get<MessageRepository>()
    val groupRepository = KoinPlatform.getKoin().get<GroupRepository>()
    val globalViewModel = KoinPlatform.getKoin().get<GlobalViewModel>()

    //println("Recieved socket message: $message")
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
                    if (globalViewModel.selectedChat.value.id == message.senderId && globalViewModel.selectedChat.value.isGroup == message.groupMessage){
                        println("Notification is in current chat, skipping display of socketmessage")
                    } else {
                        NotificationManager.showNotification(message)
                    }
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
                                updatedAt = newUser.updatedAt,
                                name = newUser.username,
                                description = newUser.userDescription,
                                status = newUser.userStatus,
                                birthDate = newUser.birthDate,
                                frienshipStatus = NetworkUtils.FriendshipStatus.ACCEPTED,
                                requesterId = newUser.requesterId,
                                profilePicUpdatedAt = newUser.profilePicUpdatedAt,

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
                                profilePictureUrl = existing?.profilePictureUrl ?: ""
                            ))
                            //Update profile picture if user is new or the profile pic got updated
                            if (existing == null || existing.profilePicUpdatedAt < socketMessage.user.profilePicUpdatedAt) {
                                appRepository.getProfilePicturesForUserIds(listOf(socketMessage.user.id))
                            }
                        }
                        is NetworkUtils.UserResponse.SelfUserResponse -> {
                            val existing = userRepository.getUserById(newUser.id)
                            userRepository.upsertUser(UserDto(
                                id = newUser.id,
                                updatedAt = newUser.updatedAt,
                                name = newUser.username,
                                description = newUser.userDescription,
                                status = newUser.userStatus,
                                birthDate = newUser.birthDate,
                                profilePicUpdatedAt = newUser.profilePicUpdatedAt,

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

                            //Update profile picture if user is new or the profile pic got updated
                            if (existing == null || existing.profilePicUpdatedAt < socketMessage.user.profilePicUpdatedAt) {
                                appRepository.getProfilePicturesForUserIds(listOf(socketMessage.user.id))
                            }
                        }
                        is NetworkUtils.UserResponse.SimpleUserResponse -> {
                            val existing = userRepository.getUserById(newUser.id)
                            userRepository.upsertUser(UserDto(
                                id = newUser.id,
                                updatedAt = newUser.updatedAt,
                                profilePicUpdatedAt = newUser.profilePicUpdatedAt,
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

                            //Update profile picture if user is new or the profile pic got updated
                            if (existing == null || existing.profilePicUpdatedAt < socketMessage.user.profilePicUpdatedAt) {
                                appRepository.getProfilePicturesForUserIds(listOf(socketMessage.user.id))
                            }


                        }
                    }


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
                        updatedAt = socketMessage.group.updatedAt,
                        profilePicUpdatedAt = socketMessage.group.profilePicUpdatedAt,
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

                    //Update profile picture if group is new or the profile pic got updated
                    if (existing == null || existing.profilePicUpdatedAt < socketMessage.group.profilePicUpdatedAt) {
                        appRepository.getProfilePicturesForGroupIds(listOf(socketMessage.group.id))
                    }
                }
            }

            //Friend request was sent
            is SocketConnectionMessage.FriendRequest -> {
                appRepository.dataSync()
                if (socketMessage.accepted) {
                    NotificationManager.showNotification(
                        titletext = getString(Res.string.new_friend_accepted_noti),
                        bodytext = getString(
                            Res.string.new_friend_accepted_noti_body,
                            socketMessage.requestingUserName
                        ),
                        notiId = NotiId.Integ(NotiIdType.FRIEND_REQUEST.baseId)
                    )
                } else {
                    NotificationManager.showNotification(
                        titletext = getString(Res.string.new_friend_request_noti, socketMessage.requestingUserName),
                        bodytext = getString(Res.string.new_friend_request_noti_body, socketMessage.requestingUserName),
                        notiId = NotiId.Integ(NotiIdType.FRIEND_REQUEST.baseId)
                    )
                }

            }


        }
    } catch (e: Exception) {
        println("Failed to deserialize socket message: ${e.message}")
    }
}

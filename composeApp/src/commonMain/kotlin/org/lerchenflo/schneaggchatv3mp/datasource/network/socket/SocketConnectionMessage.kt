package org.lerchenflo.schneaggchatv3mp.datasource.network.socket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.AppJson
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.MapEntryResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toDomainMessage
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toMapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.MapRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
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

    @Serializable
    @SerialName("mapchange")
    data class MapChange(
        val mapEntry: MapEntryResponse,
        val newEntry: Boolean,
        val deleted: Boolean,
    ) : SocketConnectionMessage

    /**
     * OUTBOUND (client -> server): our own current location. Only lat/long are mandatory.
     * [altitude]/[batteryLevel] are sent whenever location sharing is on at all; [speed]/
     * [heading] are more revealing live driving telemetry, only sent when "Advanced location
     * sharing" is enabled.
     */
    @Serializable
    @SerialName("locationupdate")
    data class LocationUpdate(
        val lat: Double,
        val long: Double,
        val speed: Double? = null,
        val heading: Double? = null,
        val altitude: Double? = null,
        val batteryLevel: Int? = null,
    ) : SocketConnectionMessage

    /** INBOUND: a single friend's live location, pushed when that friend moves. */
    @Serializable
    @SerialName("friendlocationchange")
    data class FriendLocationChange(val friend: FriendLocationPayload) : SocketConnectionMessage

    /** INBOUND: all friends' current locations, pushed once when we connect (initial load). */
    @Serializable
    @SerialName("friendlocationssnapshot")
    data class FriendLocationsSnapshot(val friends: List<FriendLocationPayload>) : SocketConnectionMessage
}

/** Wire shape of a friend's live location, pushed over the WebSocket. */
@Serializable
data class FriendLocationPayload(
    val userId: String,
    val coordinates: LatLong,
    val locationTime: Long,
    val speed: Double? = null,
    val heading: Double? = null,
    val altitude: Double? = null,
    val batteryLevel: Int? = null,
    val distanceTraveled24h: Double? = null,
    val snailTrail: List<SnailTrailPointPayload> = emptyList(),
)

@Serializable
data class SnailTrailPointPayload(
    val coordinates: LatLong,
    val locationTime: Long,
    val speed: Double? = null,
    val heading: Double? = null,
)


suspend fun handleSocketConnectionMessage(ownId: String, message: String) {

    val appRepository = KoinPlatform.getKoin().get<AppRepository>()
    val userRepository = KoinPlatform.getKoin().get<UserRepository>()
    val messageRepository = KoinPlatform.getKoin().get<MessageRepository>()
    val groupRepository = KoinPlatform.getKoin().get<GroupRepository>()
    val globalViewModel = KoinPlatform.getKoin().get<GlobalViewModel>()
    val mapRepository = KoinPlatform.getKoin().get<MapRepository>()

    try {
        val socketMessage = AppJson.instance.decodeFromString<SocketConnectionMessage>(message)

        when (socketMessage) {

            //A message got updated
            is SocketConnectionMessage.MessageChange -> {
                val existing = messageRepository.getMessageById(socketMessage.message.messageId)
                val message = socketMessage.message.toDomainMessage(
                    ownId = ownId,
                    existingLocalPK = existing?.localPK ?: 0L,
                    existingPictureUrl = existing?.pictureUrl,
                    existingAudioPath = existing?.audioPath
                )

                if (socketMessage.deleted) {
                    messageRepository.deleteMessage(socketMessage.message.messageId)
                } else {
                    messageRepository.upsertMessage(message)
                }

                //THis is a new message, show a notification
                if (socketMessage.newMessage) {
                    // resolve username / groupname
                    message.senderAsString = if(message.groupMessage) {
                        groupRepository.getGroupById(message.receiverId)?.name ?: ""
                    } else {
                        userRepository.getUserById(message.senderId)?.name ?: ""
                    }

                    if (globalViewModel.selectedChat.value.id == message.senderId && globalViewModel.selectedChat.value.isGroup == message.groupMessage){
                        if (!AppLifecycleManager.isAppInForeground) {
                            println("Noti in current chat, but app is minimized, showing noti")
                            NotificationManager.showNotification(message)
                        }
                    } else {
                        NotificationManager.showNotification(message)
                    }

                    //Only get image if the message is new
                    if (socketMessage.message.msgType == MessageType.IMAGE) {
                        appRepository.getPicturesForMessageIds(listOf(socketMessage.message.messageId))
                    }

                    if (socketMessage.message.msgType == MessageType.AUDIO) {
                        appRepository.getAudiosForMessageIds(listOf(socketMessage.message.messageId))
                        println("fething audio in Socketconecction")
                    }
                }
            }

            //A user got updated
            is SocketConnectionMessage.UserChange -> {
                println("Recieved socket connection update: Userchange")

                if (socketMessage.deleted) {
                    userRepository.deleteUser(socketMessage.user.id)
                } else {
                    when (val newUser = socketMessage.user) {
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

                        is NetworkUtils.UserResponse.FriendUserResponse -> {
                            val existing = userRepository.getUserById(newUser.id)
                            userRepository.upsertUser(UserDto(
                                id = newUser.id,
                                updatedAt = newUser.updatedAt,
                                name = newUser.username,
                                nickName = newUser.nickName,
                                description = newUser.userDescription,
                                status = newUser.userStatus,
                                birthDate = newUser.birthDate,
                                frienshipStatus = NetworkUtils.FriendshipStatus.ACCEPTED,
                                requesterId = newUser.requesterId,
                                profilePicUpdatedAt = newUser.profilePicUpdatedAt,

                                // Preserve existing values:
                                locationLat = existing?.location?.lat,
                                locationLong = existing?.location?.long,
                                locationDate = existing?.location?.date,
                                locationSpeed = existing?.location?.speed,
                                locationHeading = existing?.location?.heading,
                                locationAltitude = existing?.location?.altitude,
                                locationBattery = existing?.location?.batteryLevel,
                                locationDistance24h = existing?.location?.distanceTraveled24h,
                                locationShared = newUser.shareLocation,
                                shareSpeedHeading = newUser.shareSpeedHeading,
                                snailTrailHours = newUser.snailTrailHours,
                                wakeupEnabled = existing?.wakeupEnabled ?: false,
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
                                locationLat = existing?.location?.lat,
                                locationLong = existing?.location?.long,
                                locationDate = existing?.location?.date,
                                locationSpeed = existing?.location?.speed,
                                locationHeading = existing?.location?.heading,
                                locationAltitude = existing?.location?.altitude,
                                locationBattery = existing?.location?.batteryLevel,
                                locationDistance24h = existing?.location?.distanceTraveled24h,
                                locationShared = newUser.locationShared,
                                wakeupEnabled = existing?.wakeupEnabled ?: false,
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

            is SocketConnectionMessage.MapChange -> {
                if (socketMessage.deleted) {
                    mapRepository.deleteMapEntry(socketMessage.mapEntry.id)
                } else {
                    mapRepository.upsertMapEntry(socketMessage.mapEntry.toMapEntry())
                }
            }

            //A friend's location changed (live push)
            is SocketConnectionMessage.FriendLocationChange -> {
                userRepository.updateFriendLocation(socketMessage.friend)
            }

            //Initial snapshot of all friends' locations, pushed once on connect
            is SocketConnectionMessage.FriendLocationsSnapshot -> {
                userRepository.updateFriendLocations(socketMessage.friends)
            }

            //Outbound-only - we send this ourselves, the server never echoes it back
            is SocketConnectionMessage.LocationUpdate -> Unit

        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

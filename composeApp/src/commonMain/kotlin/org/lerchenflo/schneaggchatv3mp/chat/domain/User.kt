package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

data class User(
    override val id: String,
    val lastChanged: Long = 0L,
    override val name: String = "",
    val nickName: String? = null,
    override val description: String?,
    override val status: String?,
    val location: UserLocation? = null,
    val locationShared: Boolean = false,
    // Per-friend advanced-location settings (what we share TOWARDS this friend)
    val shareSpeedHeading: Boolean = false,
    val snailTrail: Boolean = false,
    val wakeupEnabled: Boolean = false,
    override val profilePictureUrl: String = "",
    override val friendshipStatus: NetworkUtils.FriendshipStatus?,
    override val requesterId: String? = null,
    val notisMuted: Boolean = false,
    // Epoch millis this friend was last seen online, null if unknown/never. Not to be confused
    // with "online right now", which lives in UserRepository.onlineFriendIdsFlow and is never
    // persisted.
    val lastSeen: Long? = null,
    val birthDate: String? = null, //YYYY-MM-dd

    val email: String? = null,
    val emailVerifiedAt: Long?,

    val createdAt: Long?,

    val profilePicUpdatedAt: Long,

    //Not in db
    override val unreadMessageCount: Int = 0,
    override val unsentMessageCount: Int = 0,
    override val lastmessage: Message? = null,
    override val pinned: Long = 0L,
) : SelectedChat {
    override val isGroup: Boolean
        get() = false

    fun isEmailVerified() : Boolean {
        return emailVerifiedAt != null
    }

    fun isLocationValid() : Boolean {
        if (location == null) return false
        val dateI = Instant.fromEpochMilliseconds(location.date)
        return Clock.System.now() < (dateI + 24.hours)

    }

    override val displayName: String get() = nickName?.takeIf { it.isNotBlank() } ?: name

    override fun toString(): String {
        return """
        User(
            id='$id',
            lastChanged=$lastChanged,
            name='$name',
            nickName='$nickName',
            description=$description,
            status=$status,
            location=$location,
            locationShared=$locationShared,
            shareSpeedHeading=$shareSpeedHeading,
            snailTrail=$snailTrail,
            wakeupEnabled=$wakeupEnabled,
            profilePictureUrl='$profilePictureUrl',
            friendshipStatus=$friendshipStatus,
            requesterId=$requesterId,
            notisMuted=$notisMuted,
            lastSeen=$lastSeen,
            birthDate=$birthDate,
            email=$email,
            emailVerifiedAt=$emailVerifiedAt,
            createdAt=$createdAt,
            unreadMessageCount=$unreadMessageCount,
            unsentMessageCount=$unsentMessageCount,
            lastmessage=$lastmessage,
            isGroup=$isGroup,
            pinned=$pinned,
        )
    """.trimIndent()
    }
}

fun UserDto.toUser(): User = User(
    id = this.id,
    lastChanged = this.updatedAt,
    name = this.name,
    nickName = this.nickName,
    description = this.description,
    status = this.status,
    location = run {
        val lat = this.locationLat
        val long = this.locationLong
        val date = this.locationDate
        if (lat != null && long != null && date != null) UserLocation(
            lat = lat,
            long = long,
            date = date,
            speed = this.locationSpeed,
            heading = this.locationHeading,
            altitude = this.locationAltitude,
            batteryLevel = this.locationBattery,
            distanceTraveled24h = this.locationDistance24h,
        ) else null
    },
    locationShared = this.locationShared,
    shareSpeedHeading = this.shareSpeedHeading,
    snailTrail = this.snailTrail,
    wakeupEnabled = this.wakeupEnabled,
    requesterId = this.requesterId,
    notisMuted = this.notisMuted,
    lastSeen = this.lastSeen,
    birthDate = this.birthDate,
    email = this.email,
    emailVerifiedAt = this.emailVerifiedAt,
    createdAt = this.createdAt,
    friendshipStatus = this.frienshipStatus,
    profilePictureUrl = this.profilePictureUrl,
    profilePicUpdatedAt = this.profilePicUpdatedAt,
)

/** Convert domain User back to UserDto (for persistence/transport) */
fun User.toDto(): UserDto = UserDto(
    id = this.id,
    updatedAt = this.lastChanged,
    name = this.name,
    nickName = this.nickName,
    description = this.description,
    status = this.status,
    locationLat = this.location?.lat,
    locationLong = this.location?.long,
    locationDate = this.location?.date,
    locationSpeed = this.location?.speed,
    locationHeading = this.location?.heading,
    locationAltitude = this.location?.altitude,
    locationBattery = this.location?.batteryLevel,
    locationDistance24h = this.location?.distanceTraveled24h,
    locationShared = this.locationShared,
    shareSpeedHeading = this.shareSpeedHeading,
    snailTrail = this.snailTrail,
    wakeupEnabled = this.wakeupEnabled,
    requesterId = this.requesterId,
    notisMuted = this.notisMuted,
    lastSeen = this.lastSeen,
    birthDate = this.birthDate,
    email = this.email,
    emailVerifiedAt = this.emailVerifiedAt,
    frienshipStatus = this.friendshipStatus,
    createdAt = this.createdAt,
    profilePictureUrl = this.profilePictureUrl,
    profilePicUpdatedAt = this.profilePicUpdatedAt
)
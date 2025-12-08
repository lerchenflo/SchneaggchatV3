package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.relations.MessageWithReadersDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils

data class User(
    override val id: String,
    val lastChanged: Long = 0L,
    override val name: String = "",
    override val description: String?,
    override val status: String?,
    val locationLat: Double? = null,
    val locationLong: Double? = null,
    val locationDate: Long? = null,
    val locationShared: Boolean = false,
    val wakeupEnabled: Boolean = false,
    override val profilePictureUrl: String = "",
    val lastOnline: Long? = null,
    override val friendshipStatus: NetworkUtils.FriendshipStatus?,
    override val requesterId: String? = null,
    val notisMuted: Boolean = false,
    val birthDate: String? = null,

    val email: String? = null,
    val emailVerifiedAt: Long?,

    val createdAt: Long?,

    //Not in db
    override val unreadMessageCount: Int = 0,
    override val unsentMessageCount: Int = 0,
    override val lastmessage: Message? = null,
) : SelectedChat {
    override val isGroup: Boolean
        get() = false

    fun isEmailVerified() : Boolean {
        return emailVerifiedAt != null
    }

    override fun toString(): String {
        return """
        User(
            id='$id',
            lastChanged=$lastChanged,
            name='$name',
            description=$description,
            status=$status,
            locationLat=$locationLat,
            locationLong=$locationLong,
            locationDate=$locationDate,
            locationShared=$locationShared,
            wakeupEnabled=$wakeupEnabled,
            profilePictureUrl='$profilePictureUrl',
            lastOnline=$lastOnline,
            friendshipStatus=$friendshipStatus,
            requesterId=$requesterId,
            notisMuted=$notisMuted,
            birthDate=$birthDate,
            email=$email,
            emailVerifiedAt=$emailVerifiedAt,
            createdAt=$createdAt,
            unreadMessageCount=$unreadMessageCount,
            unsentMessageCount=$unsentMessageCount,
            lastmessage=$lastmessage,
            isGroup=$isGroup
        )
    """.trimIndent()
    }
}

fun UserDto.toUser(): User = User(
    id = this.id,
    lastChanged = this.changedate,
    name = this.name,
    description = this.description,
    status = this.status,
    locationLat = this.locationLat,
    locationLong = this.locationLong,
    locationDate = this.locationDate,
    locationShared = this.locationShared,
    wakeupEnabled = this.wakeupEnabled,
    lastOnline = this.lastOnline,
    requesterId = this.requesterId,
    notisMuted = this.notisMuted,
    birthDate = this.birthDate,
    email = this.email,
    emailVerifiedAt = this.emailVerifiedAt,
    createdAt = this.createdAt,
    friendshipStatus = this.frienshipStatus,
    profilePictureUrl = this.profilePictureUrl,
)

/** Convert domain User back to UserDto (for persistence/transport) */
fun User.toDto(): UserDto = UserDto(
    id = this.id,
    changedate = this.lastChanged,
    name = this.name,
    description = this.description,
    status = this.status,
    locationLat = this.locationLat,
    locationLong = this.locationLong,
    locationDate = this.locationDate,
    locationShared = this.locationShared,
    wakeupEnabled = this.wakeupEnabled,
    lastOnline = this.lastOnline,
    requesterId = this.requesterId,
    notisMuted = this.notisMuted,
    birthDate = this.birthDate,
    email = this.email,
    emailVerifiedAt = this.emailVerifiedAt,
    frienshipStatus = this.friendshipStatus,
    createdAt = this.createdAt,
    profilePictureUrl = this.profilePictureUrl,
)
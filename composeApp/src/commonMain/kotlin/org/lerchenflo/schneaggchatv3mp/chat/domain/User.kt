package org.lerchenflo.schneaggchatv3mp.chat.domain

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
    override val profilePicture: String = "",
    val lastOnline: Long? = null,
    val friendshipStatus: NetworkUtils.FriendshipStatus? = null,
    val requesterId: String? = null,
    val notisMuted: Boolean = false,
    val birthDate: String? = null,
    val email: String? = null,
    val createdAt: Long?,
) : SelectedChat() {
    override val isGroup: Boolean
        get() = false
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
    createdAt = this.createdAt,
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
    frienshipStatus = this.friendshipStatus,
    createdAt = this.createdAt,
)
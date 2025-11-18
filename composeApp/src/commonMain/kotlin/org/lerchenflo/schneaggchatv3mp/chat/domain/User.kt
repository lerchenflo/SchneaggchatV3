package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto

data class User(
    override val id: String,
    val lastChanged: Long = 0L,
    override val name: String = "",
    override val description: String = "",
    override val status: String = "",
    val locationLat: Double? = null,
    val locationLong: Double? = null,
    val locationDate: Long? = null,
    val locationShared: Boolean = false,
    val wakeupEnabled: Boolean = false,
    override val profilePicture: String = "",
    val lastOnline: Long? = null,
    val accepted: Boolean = false,
    val requested: Boolean = false,
    val notisMuted: Boolean = false,
    val birthDate: String = "",
    val gender: String = "",
    val settings: String? = null
) : SelectedChat() {
    override val isGroup: Boolean
        get() = false


}



fun UserDto.toUser(): User = User(
    id = this.id,
    lastChanged = this.lastChanged,
    name = this.name,
    description = this.description,
    status = this.status,
    locationLat = this.locationLat,
    locationLong = this.locationLong,
    locationDate = this.locationDate,
    locationShared = this.locationShared,
    wakeupEnabled = this.wakeupEnabled,
    profilePicture = this.profilePicture,
    lastOnline = this.lastOnline,
    accepted = this.accepted,
    requested = this.requested,
    notisMuted = this.notisMuted,
    birthDate = this.birthDate,
    gender = this.gender,
    settings = this.settings
)

/** Convert domain User back to UserDto (for persistence/transport) */
fun User.toDto(): UserDto = UserDto(
    id = this.id,
    lastChanged = this.lastChanged,
    name = this.name,
    description = this.description,
    status = this.status,
    locationLat = this.locationLat,
    locationLong = this.locationLong,
    locationDate = this.locationDate,
    locationShared = this.locationShared,
    wakeupEnabled = this.wakeupEnabled,
    profilePicture = this.profilePicture,
    lastOnline = this.lastOnline,
    accepted = this.accepted,
    requested = this.requested,
    notisMuted = this.notisMuted,
    birthDate = this.birthDate,
    gender = this.gender,
    settings = this.settings
)
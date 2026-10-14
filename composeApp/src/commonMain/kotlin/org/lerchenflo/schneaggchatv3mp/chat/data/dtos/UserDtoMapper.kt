package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.User
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.UserLocation
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toFriendshipStatus
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toNetworkFriendshipStatus

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
    phoneNumber = this.phoneNumber,
    email = this.email,
    emailVerifiedAt = this.emailVerifiedAt,
    createdAt = this.createdAt,
    friendshipStatus = this.frienshipStatus?.toFriendshipStatus(),
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
    phoneNumber = this.phoneNumber,
    email = this.email,
    emailVerifiedAt = this.emailVerifiedAt,
    frienshipStatus = this.friendshipStatus?.toNetworkFriendshipStatus(),
    createdAt = this.createdAt,
    profilePictureUrl = this.profilePictureUrl,
    profilePicUpdatedAt = this.profilePicUpdatedAt
)
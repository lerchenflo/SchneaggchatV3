package org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto

import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.UserEntity
import org.lerchenflo.schneaggchatv3mp.chat.domain.AvailableUser
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.chat.domain.WakeOutcome
import org.lerchenflo.schneaggchatv3mp.chat.domain.WakeResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.NetworkFriendshipStatus
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.UserResponse

// ─── Friendship status ───────────────────────────────────────────────────────

fun NetworkFriendshipStatus.toFriendshipStatus(): FriendshipStatus = when (this) {
    NetworkFriendshipStatus.PENDING -> FriendshipStatus.PENDING
    NetworkFriendshipStatus.ACCEPTED -> FriendshipStatus.ACCEPTED
    NetworkFriendshipStatus.DECLINED -> FriendshipStatus.DECLINED
    NetworkFriendshipStatus.BLOCKED -> FriendshipStatus.BLOCKED
}

fun FriendshipStatus.toNetworkFriendshipStatus(): NetworkFriendshipStatus = when (this) {
    FriendshipStatus.PENDING -> NetworkFriendshipStatus.PENDING
    FriendshipStatus.ACCEPTED -> NetworkFriendshipStatus.ACCEPTED
    FriendshipStatus.DECLINED -> NetworkFriendshipStatus.DECLINED
    FriendshipStatus.BLOCKED -> NetworkFriendshipStatus.BLOCKED
}

// ─── Available users ─────────────────────────────────────────────────────────

fun NewFriendsUserResponse.toAvailableUser(): AvailableUser = AvailableUser(
    id = id,
    username = username,
    commonFriendCount = commonFriendCount,
    profilePicUpdatedAt = profilePicUpdatedAt,
)

// ─── Wake ────────────────────────────────────────────────────────────────────

fun NetworkWakeOutcome.toWakeOutcome(): WakeOutcome = when (this) {
    NetworkWakeOutcome.WOKEN -> WakeOutcome.WOKEN
    NetworkWakeOutcome.NOT_ALLOWED -> WakeOutcome.NOT_ALLOWED
    NetworkWakeOutcome.NO_DEVICES -> WakeOutcome.NO_DEVICES
}

fun WakeResponse.toWakeResult(): WakeResult = WakeResult(
    outcome = outcome.toWakeOutcome(),
    userCount = userCount,
    deviceCount = deviceCount,
    skippedCount = skippedCount,
)

// ─── User rows ───────────────────────────────────────────────────────────────

/**
 * The stored row for this server user. [existing] supplies the fields the server never sends
 * (live location, muted notifications); [profilePictureUrl] is the local picture file the caller
 * decided to keep.
 */
fun UserResponse.toUserEntity(existing: UserEntity?, profilePictureUrl: String): UserEntity = when (this) {
    is UserResponse.FriendUserResponse -> UserEntity(
        id = id,
        updatedAt = updatedAt,
        profilePicUpdatedAt = profilePicUpdatedAt,
        profilePictureUrl = profilePictureUrl,
        name = username,
        nickName = nickName,
        description = userDescription,
        status = userStatus,
        birthDate = birthDate,
        phoneNumber = phoneNumber,
        frienshipStatus = NetworkFriendshipStatus.ACCEPTED,
        requesterId = requesterId,
        locationLat = existing?.locationLat,
        locationLong = existing?.locationLong,
        locationDate = existing?.locationDate,
        locationSpeed = existing?.locationSpeed,
        locationHeading = existing?.locationHeading,
        locationAltitude = existing?.locationAltitude,
        locationBattery = existing?.locationBattery,
        locationDistance24h = existing?.locationDistance24h,
        locationShared = shareLocation,
        shareSpeedHeading = shareSpeedHeading,
        snailTrail = shareSnailTrail,
        // On a friend's row this means "this friend may wake me"
        wakeupEnabled = allowWake,
        notisMuted = existing?.notisMuted ?: false,
        lastSeen = lastSeen,
        email = null,
        emailVerifiedAt = null,
        createdAt = null,
    )
    is UserResponse.SelfUserResponse -> UserEntity(
        id = id,
        updatedAt = updatedAt,
        profilePicUpdatedAt = profilePicUpdatedAt,
        profilePictureUrl = profilePictureUrl,
        name = username,
        description = userDescription,
        status = userStatus,
        birthDate = birthDate,
        phoneNumber = phoneNumber,
        frienshipStatus = null,
        requesterId = null,
        locationLat = existing?.locationLat,
        locationLong = existing?.locationLong,
        locationDate = existing?.locationDate,
        locationSpeed = existing?.locationSpeed,
        locationHeading = existing?.locationHeading,
        locationAltitude = existing?.locationAltitude,
        locationBattery = existing?.locationBattery,
        locationDistance24h = existing?.locationDistance24h,
        locationShared = locationShared,
        // On the own row this is the master wake switch
        wakeupEnabled = allowWakeGlobal,
        notisMuted = false,
        email = email,
        emailVerifiedAt = emailVerifiedAt,
        createdAt = createdAt,
    )
    // Not a friend: no profile details and no location
    is UserResponse.SimpleUserResponse -> UserEntity(
        id = id,
        updatedAt = updatedAt,
        profilePicUpdatedAt = profilePicUpdatedAt,
        profilePictureUrl = profilePictureUrl,
        name = username,
        frienshipStatus = friendShipStatus,
        requesterId = requesterId,
        locationShared = false,
        wakeupEnabled = false,
        notisMuted = false,
        emailVerifiedAt = null,
        createdAt = null,
    )
}

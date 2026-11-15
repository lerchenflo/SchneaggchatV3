package org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto

import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.LookedUpUserEntity
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

// ─── User lookup ─────────────────────────────────────────────────────────────

/**
 * The looked-up rows of this reply, one per requested id. A row keeps the picture file of its
 * [existing] row only while the picture version is the same; a changed or hidden picture is loaded
 * again on the next show.
 */
fun UserLookupResponse.toLookedUpUserEntities(
    existing: Map<String, LookedUpUserEntity>,
    fetchedAt: Long,
): List<LookedUpUserEntity> =
    users.map { it.toLookedUpUserEntity(existing[it.id], fetchedAt) } +
        deletedUserIds.map { deletedLookedUpUserEntity(it, fetchedAt) }

private fun LookedUpUserResponse.toLookedUpUserEntity(existing: LookedUpUserEntity?, fetchedAt: Long): LookedUpUserEntity {
    val samePicture = profilePicUpdatedAt != null && existing?.profilePicUpdatedAt == profilePicUpdatedAt
    return LookedUpUserEntity(
        id = id,
        name = username,
        profilePicUpdatedAt = profilePicUpdatedAt,
        deleted = false,
        fetchedAt = fetchedAt,
        profilePictureUrl = if (samePicture) existing.profilePictureUrl else "",
    )
}

private fun deletedLookedUpUserEntity(id: String, fetchedAt: Long): LookedUpUserEntity = LookedUpUserEntity(
    id = id,
    name = "",
    profilePicUpdatedAt = null,
    deleted = true,
    fetchedAt = fetchedAt,
    profilePictureUrl = "",
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

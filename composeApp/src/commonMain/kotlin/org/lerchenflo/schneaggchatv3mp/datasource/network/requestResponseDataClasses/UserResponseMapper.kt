package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import org.lerchenflo.schneaggchatv3mp.chat.domain.AvailableUser
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.chat.domain.WakeOutcome
import org.lerchenflo.schneaggchatv3mp.chat.domain.WakeResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils

// ─── Friendship status ───────────────────────────────────────────────────────

fun NetworkUtils.FriendshipStatus.toFriendshipStatus(): FriendshipStatus = when (this) {
    NetworkUtils.FriendshipStatus.PENDING -> FriendshipStatus.PENDING
    NetworkUtils.FriendshipStatus.ACCEPTED -> FriendshipStatus.ACCEPTED
    NetworkUtils.FriendshipStatus.DECLINED -> FriendshipStatus.DECLINED
    NetworkUtils.FriendshipStatus.BLOCKED -> FriendshipStatus.BLOCKED
}

fun FriendshipStatus.toNetworkFriendshipStatus(): NetworkUtils.FriendshipStatus = when (this) {
    FriendshipStatus.PENDING -> NetworkUtils.FriendshipStatus.PENDING
    FriendshipStatus.ACCEPTED -> NetworkUtils.FriendshipStatus.ACCEPTED
    FriendshipStatus.DECLINED -> NetworkUtils.FriendshipStatus.DECLINED
    FriendshipStatus.BLOCKED -> NetworkUtils.FriendshipStatus.BLOCKED
}

// ─── Available users ─────────────────────────────────────────────────────────

fun NetworkUtils.NewFriendsUserResponse.toAvailableUser(): AvailableUser = AvailableUser(
    id = id,
    username = username,
    commonFriendCount = commonFriendCount,
)

// ─── Wake ────────────────────────────────────────────────────────────────────

fun NetworkUtils.WakeOutcome.toWakeOutcome(): WakeOutcome = when (this) {
    NetworkUtils.WakeOutcome.WOKEN -> WakeOutcome.WOKEN
    NetworkUtils.WakeOutcome.NOT_ALLOWED -> WakeOutcome.NOT_ALLOWED
    NetworkUtils.WakeOutcome.NO_DEVICES -> WakeOutcome.NO_DEVICES
}

fun NetworkUtils.WakeResponse.toWakeResult(): WakeResult = WakeResult(
    outcome = outcome.toWakeOutcome(),
    userCount = userCount,
    deviceCount = deviceCount,
    skippedCount = skippedCount,
)

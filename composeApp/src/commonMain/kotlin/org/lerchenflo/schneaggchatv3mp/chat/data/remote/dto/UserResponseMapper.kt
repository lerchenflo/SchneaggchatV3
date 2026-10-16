package org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto

import org.lerchenflo.schneaggchatv3mp.chat.domain.AvailableUser
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.chat.domain.WakeOutcome
import org.lerchenflo.schneaggchatv3mp.chat.domain.WakeResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.NetworkFriendshipStatus

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

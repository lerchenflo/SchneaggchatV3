package org.lerchenflo.schneaggchatv3mp.datasource.network.socket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils

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

}
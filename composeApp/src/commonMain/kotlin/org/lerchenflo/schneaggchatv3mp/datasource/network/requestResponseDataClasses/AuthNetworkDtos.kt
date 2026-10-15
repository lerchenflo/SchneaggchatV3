package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.utilities.DEVICETYPE

@Serializable
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val deviceName: String,
    val deviceType: DEVICETYPE
)

@Serializable
data class LogoutRequest(
    val refreshToken: String? = null,
    val notificationToken: String? = null,
    val isAndroid: Boolean? = null,
    val allDevices: Boolean = false,
)

/**
 * One logged-in device of the own account.
 * [deviceName] is the raw name the device sent at login (incl. its id suffix).
 */
@Serializable
data class SessionResponse(
    val id: String,
    val deviceName: String? = null,
    val deviceType: DEVICETYPE? = null,
    val createdAt: Long,
    val lastUsedAt: Long,
)

@Serializable
data class NotificationTokenRequest(
    val token: String,
    val isAndroid: Boolean
)

@Serializable
data class PasswordChangeRequest(
    val oldPassword: String,
    val newPassword: String
)

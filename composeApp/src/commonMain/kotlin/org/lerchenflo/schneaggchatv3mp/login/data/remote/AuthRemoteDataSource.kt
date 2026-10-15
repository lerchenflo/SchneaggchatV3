package org.lerchenflo.schneaggchatv3mp.login.data.remote

import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.SessionResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.TokenPair
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

/**
 * Server calls for the account session: login, registration, logout, device sessions, push token,
 * email verification and password change.
 */
interface AuthRemoteDataSource {

    suspend fun login(username: String, password: String): NetworkResult<TokenPair, NetworkingError>

    suspend fun register(
        username: String,
        password: String,
        email: String,
        birthDate: String,
        profilePicBytes: ByteArray,
        phoneNumber: String? = null,
        language: String? = null,
        fileName: String = "profile.jpg"
    ): NetworkResult<Unit, NetworkingError>

    suspend fun sendEmailVerify(): NetworkResult<Unit, NetworkingError>

    /**
     * Ends this device's session on the server. Uses the client without the bearer token: the
     * refresh token in the body is what identifies the session, so an expired access token must not
     * send this through the Auth plugin and trigger a token refresh for a session we are about to
     * kill anyway.
     *
     * [refreshToken] is null when the session is already dead (e.g. ended from another device's
     * device list) - the server then only removes the [notificationToken].
     */
    suspend fun logout(
        refreshToken: String?,
        notificationToken: String?,
        isAndroid: Boolean,
    ): NetworkResult<Unit, NetworkingError>

    /**
     * Ends every session of the own account on the server, this device's included, and removes
     * all of the account's push tokens. Unlike [logout] this goes through the bearer client: the
     * server identifies the account by the refresh token or, failing that, the access token - so
     * it must not depend on the refresh token alone.
     */
    suspend fun logoutAllDevices(refreshToken: String?): NetworkResult<Unit, NetworkingError>

    suspend fun getSessions(): NetworkResult<List<SessionResponse>, NetworkingError>

    /**
     * Logs one of the own devices out remotely.
     */
    suspend fun endSession(sessionId: String): NetworkResult<Unit, NetworkingError>

    suspend fun setNotificationToken(token: String, isAndroid: Boolean) : NetworkResult<Any, NetworkingError>

    suspend fun changePassword(oldPassword: String, newPassword: String) : NetworkResult<Any, NetworkingError>
}

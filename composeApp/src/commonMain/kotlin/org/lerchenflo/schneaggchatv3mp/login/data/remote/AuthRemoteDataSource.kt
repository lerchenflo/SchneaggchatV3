package org.lerchenflo.schneaggchatv3mp.login.data.remote

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.login.data.remote.dto.SessionResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.TokenPair

/**
 * Server calls for the account session: login, registration, logout, device sessions, push token,
 * email verification and password change.
 */
interface AuthRemoteDataSource {

    suspend fun login(username: String, password: String): DataResult<TokenPair, DataError>

    suspend fun register(
        username: String,
        password: String,
        email: String,
        birthDate: String,
        profilePicBytes: ByteArray,
        phoneNumber: String? = null,
        language: String? = null,
        fileName: String = "profile.jpg"
    ): DataResult<Unit, DataError>

    suspend fun sendEmailVerify(): DataResult<Unit, DataError>

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
    ): DataResult<Unit, DataError>

    /**
     * Ends every session of the own account on the server, this device's included, and removes
     * all of the account's push tokens. Unlike [logout] this goes through the bearer client: the
     * server identifies the account by the refresh token or, failing that, the access token - so
     * it must not depend on the refresh token alone.
     */
    suspend fun logoutAllDevices(refreshToken: String?): DataResult<Unit, DataError>

    suspend fun getSessions(): DataResult<List<SessionResponse>, DataError>

    /**
     * Logs one of the own devices out remotely.
     */
    suspend fun endSession(sessionId: String): DataResult<Unit, DataError>

    suspend fun setNotificationToken(token: String, isAndroid: Boolean) : DataResult<Any, DataError>

    suspend fun changePassword(oldPassword: String, newPassword: String) : DataResult<Any, DataError>
}

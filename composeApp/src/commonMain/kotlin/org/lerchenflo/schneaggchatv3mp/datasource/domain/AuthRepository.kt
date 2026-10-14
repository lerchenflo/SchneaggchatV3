package org.lerchenflo.schneaggchatv3mp.datasource.domain

import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DeviceSession
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.SavedCredsResult

/**
 * Login, registration and the server side of the own account's sessions (password, devices,
 * push token). Ending the session locally (logout, wiping the app data) is [SessionTerminator]'s job.
 */
interface AuthRepository {

    /**
     * Password login. On success the session is persisted before [onResult] gets true; on failure
     * an error is sent to the [ErrorChannel] and [onResult] gets false.
     */
    suspend fun login(
        username: String,
        password: String,
        onResult: (Boolean) -> Unit
    )

    /** Registers a new account. [onResult] gets false (error already sent) if the server refused it. */
    suspend fun createAccount(
        username: String,
        email: String,
        password: String,
        birthdate: String,
        profilePic: ByteArray,
        phoneNumber: String? = null,
        language: String? = null,
        onResult: (Boolean) -> Unit
    )

    /**
     * Function to load all initial data on app start: restores a stored session (no network call)
     * and reports whether one exists and whether the own email is verified.
     */
    suspend fun loadSavedLoginConfig(): SavedCredsResult

    /** Sends the verification email again. @return false if it failed (error already sent) */
    suspend fun sendEmailVerify(): Boolean

    /** @return false if the password was not changed (error already sent) */
    suspend fun changePassword(oldPassword: String, newPassword: String): Boolean

    /**
     * Logged-in devices of the own account, or null when loading failed (error already sent).
     */
    suspend fun getSessions(): List<DeviceSession>?

    /**
     * Logs one of the own devices out remotely.
     */
    suspend fun endSession(sessionId: String): Boolean

    /** Registers this device's push token with the server. Empty tokens are ignored. */
    suspend fun setNotificationToken(token: String)

    /**
     * Ends the session on the server before the client forgets the refresh token, so the token is
     * actually dead instead of staying valid until it expires. Best effort: a logout must succeed
     * offline too, the session then only dies with its expiry.
     *
     * The device's push token goes along, otherwise the server keeps sending notifications for the
     * account this device just left. That also holds when the session is already dead (ended from
     * another device's device list, refresh rejected): then only the push token is sent.
     *
     * Bounded by a 5 s timeout - the HTTP client would otherwise let the user wait up to its 30 s
     * request timeout on a bad network before the local logout even starts. The server side call
     * is idempotent, so a cancelled attempt that still lands is harmless.
     */
    suspend fun endServerSession(refreshToken: String?)

    /**
     * The server half of logging the account out everywhere: ends every session (this device's
     * too) and drops all push tokens. Not best effort: on failure the error is sent and the other
     * devices are still logged in. The local logout afterwards is
     * [SessionTerminator.logoutAllDevices]'s job.
     * @return true if the server logged the account out everywhere
     */
    suspend fun logoutAllDevicesOnServer(): Boolean
}

package org.lerchenflo.schneaggchatv3mp.datasource.domain

/**
 * Ends the session on this device: forgets the credentials and wipes the local app data
 * (database, preferences, push token, socket). The server side goes through [AuthRepository].
 */
interface SessionTerminator {

    /**
     * Logs this device out. Credentials are cleared first, everything else after.
     * @param endSessionOnServer false when the server session is already gone (e.g. after
     * [logoutAllDevices] or an invalidated session), so no second server call is made
     */
    suspend fun logout(endSessionOnServer: Boolean = true)

    /**
     * Delete all app data (for example on appdatadelete or logout)
     */
    suspend fun deleteAllAppData()

    /**
     * Logs the account out everywhere. Mirrors the server's allDevices logout: it ends every
     * session (this device's too) and drops all push tokens, so this device then runs the normal
     * local logout - without a second server call, the session is already gone.
     *
     * Unlike [logout] this is not best effort: if the server call fails, the other devices are
     * still logged in, so the user stays logged in here too and sees the error.
     * @return true if the account was logged out everywhere
     */
    suspend fun logoutAllDevices(): Boolean
}

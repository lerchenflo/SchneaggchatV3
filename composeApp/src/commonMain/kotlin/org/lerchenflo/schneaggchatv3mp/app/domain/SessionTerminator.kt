package org.lerchenflo.schneaggchatv3mp.app.domain

import org.lerchenflo.schneaggchatv3mp.login.domain.AuthRepository

/**
 * Ends the session on this device: forgets the credentials and wipes the local app data
 * (database, downloaded media, preferences, push token, socket). The server side goes through [AuthRepository].
 */
interface SessionTerminator {

    /**
     * Logs this device out. Credentials are cleared first, everything else after.
     * @param endSessionOnServer false when the server session is already gone (e.g. after
     * [logoutAllDevices]), so no second server call is made. An invalidated session keeps true:
     * the refresh token is gone, but the call still sends the push token, so the server stops
     * pushing to this device
     * @param showSnackbar false when the caller already told the user (forced logout popup),
     * so the "Successfully logged out" snackbar is skipped
     */
    suspend fun logout(endSessionOnServer: Boolean = true, showSnackbar: Boolean = true)

    /**
     * Resets this device's local copy of the account but stays logged in: clears the database,
     * the notifications and the push token and drops the cached session, so the next start
     * restores it from the stored credentials and syncs everything again ("App broken" in the
     * misc settings). Keeps the credentials, the preferences and the downloaded media.
     * [logout] runs it as part of its wipe.
     *
     * A repair tool from version 1 of the app, where it also deleted all local files and
     * preferences. Not needed for a known bug any more, kept on purpose.
     */
    suspend fun resetLocalData()

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

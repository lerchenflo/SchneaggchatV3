package org.lerchenflo.schneaggchatv3mp.app.data

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.login.domain.AuthRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.app.domain.SessionTerminator
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthSessionManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SocketConnectionManager
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.Notifier
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.log_out_successfully

/**
 * [SessionTerminator] wiping this device: session ([AuthSessionManager]), database, preferences,
 * push token and socket. The server side of a logout goes through [AuthRepository].
 * Downloaded media is not deleted (see plans/Todo.md).
 */
class LocalSessionTerminator(
    private val database: AppDatabase,
    private val preferencemanager: Preferencemanager,
    private val authSessionManager: AuthSessionManager,
    private val authRepository: AuthRepository,
    private val notifier: Notifier,
    private val socketConnectionManager: SocketConnectionManager,
    private val loggingRepository: Logger,
) : SessionTerminator {

    /**
     * Delete all app data (for example on appdatadelete or logout)
     */
    override suspend fun deleteAllAppData(){

        preferencemanager.saveLastStartedVersion("") //Show Changelog on next startup

        database.allDatabaseDao().clearAll() //Drop all database tables
        notifier.removeToken() //Remove notificationTOken
        SessionCache.logout() //Remove saved credentials from cache, but not from storage
    }

    /**
     * Logout order (AUTH_SESSION_REBUILD_PLAN R11): credentials first, everything else after.
     * The refresh token is read into a local before it is cleared so the server-side session can
     * still be ended best-effort; a throw anywhere after step 2 can no longer leave a session on
     * disk that the next start would silently pick up again.
     */
    override suspend fun logout(endSessionOnServer: Boolean){
        val refreshToken = authSessionManager.currentTokens()?.refreshToken

        try {
            authSessionManager.clearSession() // mirror + durable credentials + generation, before anything can fail
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            loggingRepository.logError("Logout: clearing the session threw (${e.message}); continuing with the wipe")
        } finally {
            SessionCache.logout() //Remove cached credentials (Userid)
        }

        if (endSessionOnServer) authRepository.endServerSession(refreshToken) // best effort, bounded; the token is already gone locally

        deleteAllAppData() // delete all app data when logging out

        preferencemanager.clearAll() //Delete all app data to force login again (retries the token delete too)

        notifier.removeToken() //Remove notification token

        socketConnectionManager.close() //Close socket connection

        SnackbarManager.showMessage(getString(Res.string.log_out_successfully))
    }

    /**
     * Logs the account out everywhere. Mirrors the server's allDevices logout: it ends every
     * session (this device's too) and drops all push tokens, so this device then runs the normal
     * local logout - without a second server call, the session is already gone.
     *
     * Unlike [logout] this is not best effort: if the server call fails, the other devices are
     * still logged in, so the user stays logged in here too and sees the error.
     * @return true if the account was logged out everywhere
     */
    override suspend fun logoutAllDevices(): Boolean {
        if (!authRepository.logoutAllDevicesOnServer()) return false

        logout(endSessionOnServer = false)
        return true
    }
}

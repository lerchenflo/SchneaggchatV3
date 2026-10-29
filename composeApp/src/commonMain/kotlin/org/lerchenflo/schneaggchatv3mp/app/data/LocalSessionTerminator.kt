package org.lerchenflo.schneaggchatv3mp.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import org.lerchenflo.schneaggchatv3mp.utilities.AudioManager
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager

/**
 * [SessionTerminator] wiping this device: session ([AuthSessionManager]), database, downloaded
 * media, preferences, push token and socket. The server side of a logout goes through [AuthRepository].
 *
 * Every wipe runs in [externalScope] (plan R2), started before the first suspension point: a caller
 * that is cancelled meanwhile (a settings screen being left) only stops waiting, the wipe still
 * finishes. A half-done logout would leave the old account's data on the device.
 */
class LocalSessionTerminator(
    private val database: AppDatabase,
    private val preferencemanager: Preferencemanager,
    private val authSessionManager: AuthSessionManager,
    private val authRepository: AuthRepository,
    private val notifier: Notifier,
    private val socketConnectionManager: SocketConnectionManager,
    private val loggingRepository: Logger,
    private val pictureManager: PictureManager,
    private val audioManager: AudioManager,
    private val externalScope: CoroutineScope,
) : SessionTerminator {

    override suspend fun resetLocalData(){
        externalScope.async {
            preferencemanager.saveLastStartedVersion("") //Show Changelog on next startup

            database.allDatabaseDao().clearAll() //Drop all database tables
            notifier.cancelAllNotifications() //Notifications and conversation shortcuts still show the old account's chats
            notifier.removeToken() //Remove notificationTOken
            SessionCache.logout() //Remove saved credentials from cache, but not from storage
        }.await()
    }

    // Single flight: a second logout while one runs (logout tapped twice on a slow network) joins
    // it. Run twice, the second wipe could land after the next login and erase the new session.
    private val logoutLock = Mutex()
    private var logoutInFlight: Deferred<Unit>? = null

    /**
     * Logout order (AUTH_SESSION_REBUILD_PLAN R11): credentials first, everything else after.
     * The refresh token is read into a local before it is cleared so the server-side session can
     * still be ended best-effort; a throw anywhere after step 2 can no longer leave a session on
     * disk that the next start would silently pick up again.
     */
    override suspend fun logout(endSessionOnServer: Boolean, showSnackbar: Boolean){
        val logout = logoutLock.withLock {
            logoutInFlight?.takeIf { it.isActive } ?: startLogout(endSessionOnServer, showSnackbar).also { logoutInFlight = it }
        }
        logout.await()
    }

    private fun startLogout(endSessionOnServer: Boolean, showSnackbar: Boolean): Deferred<Unit> =
        externalScope.async {
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

            resetLocalData() // database, notifications, push token - the preferences follow below

            deleteStoredMedia()

            preferencemanager.clearAll() //Delete all app data to force login again (retries the token delete too)

            notifier.removeToken() //Remove notification token

            socketConnectionManager.close() //Close socket connection

            if (showSnackbar) SnackbarManager.showMessage(getString(Res.string.log_out_successfully))
        }

    /**
     * Pictures and voice messages of this account must not stay on the device for the next one.
     * Only on logout - "App broken" ([resetLocalData]) keeps them. Never throws.
     */
    private suspend fun deleteStoredMedia() {
        try {
            val pictures = pictureManager.deleteAllStoredPictures()
            val voiceMessages = audioManager.deleteAllStoredAudio()
            loggingRepository.logInfo("Logout: deleted $pictures pictures and $voiceMessages voice messages")
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            loggingRepository.logError("Logout: deleting the stored media failed (${e.message})")
        }
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
        return externalScope.async {
            if (!authRepository.logoutAllDevicesOnServer()) return@async false

            logout(endSessionOnServer = false)
            true
        }.await()
    }
}

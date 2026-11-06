package org.lerchenflo.schneaggchatv3mp.login.data

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL_TEST
import org.lerchenflo.schneaggchatv3mp.TEST_ACCOUNT_USERNAME
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.asEmptyDataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.map
import org.lerchenflo.schneaggchatv3mp.app.domain.onError
import org.lerchenflo.schneaggchatv3mp.app.domain.onSuccess
import org.lerchenflo.schneaggchatv3mp.login.domain.AuthRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.login.domain.DeviceSession
import org.lerchenflo.schneaggchatv3mp.login.domain.SavedCredsResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthSessionManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.SessionCheck
import org.lerchenflo.schneaggchatv3mp.login.data.remote.dto.toDeviceSession
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.JwtUtils
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.Notifier
import kotlin.time.Duration.Companion.seconds
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.TokenPair
import org.lerchenflo.schneaggchatv3mp.login.data.remote.AuthRemoteDataSource

//How long a logout waits for the server to drop the session before continuing to log out locally.
private val LOGOUT_SERVER_CALL_TIMEOUT = 5.seconds

/**
 * [AuthRepository] talking to the server through [AuthRemoteDataSource]. The session itself (tokens,
 * refresh, persistence) is owned by [AuthSessionManager]; this class only seeds and reads it.
 */
class RemoteAuthRepository(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val preferencemanager: Preferencemanager,
    private val authSessionManager: AuthSessionManager,
    private val userRepository: UserRepository,
    private val appVersion: AppVersion,
    private val notifier: Notifier,
    private val loggingRepository: Logger,
) : AuthRepository {

    override suspend fun setNotificationToken(token: String) {
        if (token.isEmpty()) return

        val android = appVersion.isAndroid()
        //println("Sending notification token to server... Android: $android")
        //Without a registered token the server can't push anything, so a failure must show up in the logs.
        //The upload is retried on the next app start (chat list) and whenever Firebase issues a new token.
        val result = authRemoteDataSource.setNotificationToken(token, android).trackConnectivity()
        if (result is DataResult.Error<*>) {
            loggingRepository.logError("Uploading the notification token failed: ${result.error}")
        }
    }

    override suspend fun sendEmailVerify(): EmptyDataResult<DataError> =
        authRemoteDataSource.sendEmailVerify().trackConnectivity()
            .asEmptyDataResult()

    /**
     * Ends the session on the server before the client forgets the refresh token, so the token is
     * actually dead instead of staying valid until it expires. Best effort: a logout must succeed
     * offline too, the session then only dies with its expiry.
     *
     * The device's push token goes along, otherwise the server keeps sending notifications for the
     * account this device just left. That also holds when the session is already dead (ended from
     * another device's device list, refresh rejected): then only the push token is sent.
     *
     * Bounded by [LOGOUT_SERVER_CALL_TIMEOUT] - the HTTP client would otherwise let the user wait
     * up to its 30 s request timeout on a bad network before the local logout even starts. The
     * server side call is idempotent, so a cancelled attempt that still lands is harmless.
     */
    override suspend fun endServerSession(refreshToken: String?) {
        val usableRefreshToken = refreshToken?.takeIf { it.isNotBlank() && JwtUtils.isTokenDateValid(it) }

        val finished = withTimeoutOrNull(LOGOUT_SERVER_CALL_TIMEOUT) {
            // Inside the timeout: fetching the FCM token can hang offline
            val notificationToken = notifier.getToken()
            if (usableRefreshToken == null && notificationToken.isNullOrBlank()) return@withTimeoutOrNull Unit

            authRemoteDataSource.logout(
                refreshToken = usableRefreshToken,
                notificationToken = notificationToken,
                isAndroid = appVersion.isAndroid(),
            ).trackConnectivity()
                .onError { loggingRepository.logWarning("Logout: server session not ended (${it.errorCode}): ${it.debugInfo}") }
        }

        if (finished == null) loggingRepository.logWarning("Logout: server session not ended (timeout)")
    }

    /**
     * Seeds the session after a password login or registration. Persists the pair (throws if
     * that fails, so the login reports failure) and updates every derived view through the
     * session manager - the only writer of token storage.
     */
    private suspend fun onNewTokenPair(tokenPair: TokenPair){
        try {
            authSessionManager.onLoggedIn(tokenPair)
            SessionCache.updateOnline(true)
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            loggingRepository.logError("Token save failed: ${e.message}")
            throw e // Re-throw to maintain existing error handling behavior
        }
    }

    /**
     * Function to load all initial data on app start
     */
    override suspend fun loadSavedLoginConfig(): SavedCredsResult {
        val tokens = preferencemanager.getTokens()

        val tokensNotEmpty = tokens.accessToken.isNotEmpty() && tokens.refreshToken.isNotEmpty()

        // Classifies the stored pair without a network call: a valid refresh token hydrates
        // SessionCache (and schedules a refresh if the access token is stale), an expired or
        // unreadable one is cleared so nothing can retry against it.
        val tokenDateValid = authSessionManager.ensureSession() is SessionCheck.Active

        val credsSaved = tokenDateValid && tokensNotEmpty

        if (credsSaved){
            println("Tokens are saved in local storage, autologin permitted")

            // ensureSession() already hydrated SessionCache through the session manager's sink;
            // only the developer flag lives outside the token pair.
            SessionCache.updateDeveloper(preferencemanager.getDevSettings())
        }

        var emailVerified = false
        if (credsSaved) {
            val userId = JwtUtils.getUserIdFromToken(tokens.refreshToken)
            emailVerified = userRepository.getUserById(userId)?.emailVerifiedAt != null
        }

        return SavedCredsResult(
            credsSaved = credsSaved,
            emailVerified = emailVerified,
            sessionExpired = !tokenDateValid && tokensNotEmpty,
        )
    }


    override suspend fun login(username: String, password: String): EmptyDataResult<DataError> {
        //Overwrite the server url for the test account to always connect to it
        if (username.lowercase().trim() == TEST_ACCOUNT_USERNAME) {
            preferencemanager.saveServerUrl(BASE_SERVER_URL_TEST)
        }

        return authRemoteDataSource.login(username.trim(), password).trackConnectivity()
            .onSuccess { tokenPair ->
                withContext(NonCancellable) {
                    onNewTokenPair(tokenPair)
                }

                // onLoggedIn() hydrated SessionCache through the session manager's sink; only the
                // developer flag lives outside the token pair.
                SessionCache.updateDeveloper(preferencemanager.getDevSettings())
            }
            .asEmptyDataResult()
    }


    override suspend fun createAccount(
        username: String,
        email: String,
        password: String,
        birthdate: String,
        profilePic: ByteArray,
        phoneNumber: String?,
        language: String?,
    ): EmptyDataResult<DataError> =
        authRemoteDataSource.register(username, password, email, birthdate, profilePic, phoneNumber, language)
            .trackConnectivity()
            .asEmptyDataResult()


    override suspend fun changePassword(oldPassword: String, newPassword: String): EmptyDataResult<DataError> =
        authRemoteDataSource.changePassword(oldPassword, newPassword).trackConnectivity()
            .asEmptyDataResult()


    /** Logged-in devices of the own account. */
    override suspend fun getSessions(): DataResult<List<DeviceSession>, DataError> =
        authRemoteDataSource.getSessions().trackConnectivity()
            .map { sessions -> sessions.map { it.toDeviceSession() } }

    /**
     * The server half of the account-wide logout (the local half is
     * [org.lerchenflo.schneaggchatv3mp.app.domain.SessionTerminator.logoutAllDevices]).
     * Not best effort: if the server call fails, the other devices are still logged in.
     */
    override suspend fun logoutAllDevicesOnServer(): EmptyDataResult<DataError> {
        val refreshToken = authSessionManager.currentTokens()?.refreshToken
            ?.takeIf { it.isNotBlank() && JwtUtils.isTokenDateValid(it) }

        return authRemoteDataSource.logoutAllDevices(refreshToken).trackConnectivity()
            .onError { loggingRepository.logWarning("Logout all devices failed (${it.errorCode}): ${it.debugInfo}") }
            .asEmptyDataResult()
    }

    /**
     * Logs one of the own devices out remotely.
     */
    override suspend fun endSession(sessionId: String): EmptyDataResult<DataError> =
        authRemoteDataSource.endSession(sessionId).trackConnectivity()
            .asEmptyDataResult()
}

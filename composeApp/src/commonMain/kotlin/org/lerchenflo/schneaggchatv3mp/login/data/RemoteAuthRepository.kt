package org.lerchenflo.schneaggchatv3mp.login.data

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL_TEST
import org.lerchenflo.schneaggchatv3mp.TEST_ACCOUNT_USERNAME
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.login.domain.AuthRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorEvent
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.login.domain.DeviceSession
import org.lerchenflo.schneaggchatv3mp.login.domain.SavedCredsResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthSessionManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.SessionCheck
import org.lerchenflo.schneaggchatv3mp.login.data.remote.dto.toDeviceSession
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.onError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.JwtUtils
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.Notifier
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_access_expired
import schneaggchatv3mp.composeapp.generated.resources.error_invalid_credentials
import schneaggchatv3mp.composeapp.generated.resources.error_login_server_error
import schneaggchatv3mp.composeapp.generated.resources.error_login_too_many_requests
import schneaggchatv3mp.composeapp.generated.resources.offline
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
    private val errorChannel: ErrorChannel,
) : AuthRepository {

    override suspend fun setNotificationToken(token: String) {
        if (token.isEmpty()) return

        val android = appVersion.isAndroid()
        //println("Sending notification token to server... Android: $android")
        authRemoteDataSource.setNotificationToken(token, android).trackConnectivity()
    }

    override suspend fun sendEmailVerify() : Boolean{
        return when (val result = authRemoteDataSource.sendEmailVerify().trackConnectivity()) {
            is NetworkResult.Error<NetworkingError> -> {
                println("Send email verify: Error occured: ${result.error}")
                errorChannel.sendErrorSuspend(ErrorEvent(error = result.error))
                false
            }
            is NetworkResult.Success<*> -> {
                true
            }
        }
    }

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
                .onError { loggingRepository.logWarning("Logout: server session not ended (${it.errorCode}): ${it.message}") }
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

        //Token is expired, send errormessage
        if (!tokenDateValid && tokensNotEmpty){
            errorChannel.trySendError(
                event = ErrorEvent(
                    401,
                    errorMessageUiText = UiText.StringResourceText(Res.string.error_access_expired),
                    duration = 5000L,
                )
            )
        }

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
            emailVerified = emailVerified
        )
    }


    override suspend fun login(
        username: String,
        password: String,
        onResult: (Boolean) -> Unit
    ) {
        //Overwrite the server url for the test account to always connect to it
        if (username.lowercase().trim() == TEST_ACCOUNT_USERNAME) {
            preferencemanager.saveServerUrl(BASE_SERVER_URL_TEST)
        }

        when(val result = authRemoteDataSource.login(username.trim(), password).trackConnectivity()){
            is NetworkResult.Error<*> -> {
                println("Error: ${result.error}")

                // Only a genuine 401 means wrong credentials - rate limiting, server errors and
                // being offline are not the user's fault and shouldn't be reported as such.
                val messageRes = when (result.error) {
                    is NetworkingError.TooManyRequests -> Res.string.error_login_too_many_requests
                    is NetworkingError.NoInternetConnection,
                    is NetworkingError.NetworkTimeout -> Res.string.offline
                    is NetworkingError.ServerError -> Res.string.error_login_server_error
                    else -> Res.string.error_invalid_credentials
                }

                errorChannel.sendErrorSuspend(
                    ErrorEvent(
                        errorMessageUiText = UiText.StringResourceText(messageRes),
                        duration = 5000L
                    )
                )

                onResult(false)
            }
            is NetworkResult.Success<TokenPair> -> {
                withContext(NonCancellable) {
                    onNewTokenPair(result.data)
                }

                // onLoggedIn() hydrated SessionCache through the session manager's sink; only the
                // developer flag lives outside the token pair.
                SessionCache.updateDeveloper(preferencemanager.getDevSettings())

                onResult(true)
            }
        }
    }


    override suspend fun createAccount(
        username: String,
        email: String,
        password: String,
        birthdate: String,
        profilePic: ByteArray,
        phoneNumber: String?,
        language: String?,
        onResult: (Boolean) -> Unit
    ) {
        when(val response = authRemoteDataSource.register(username, password, email, birthdate, profilePic, phoneNumber, language).trackConnectivity()){
            is NetworkResult.Error -> {
                println("Error: ${response.error}")

                errorChannel.sendErrorSuspend(
                    ErrorEvent(
                        error = response.error,
                        duration = 5000L
                    )
                )

                onResult(false)
            }
            is NetworkResult.Success<*> -> {
                onResult(true)
            }
        }
    }


    override suspend fun changePassword(oldPassword: String, newPassword: String) : Boolean {
        when (val success = authRemoteDataSource.changePassword(oldPassword, newPassword).trackConnectivity()){
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = success.error))
                return false
            }
            is NetworkResult.Success<*> -> {
                return true
            }
        }
    }


    /**
     * Logged-in devices of the own account, or null when loading failed (error already sent).
     */
    override suspend fun getSessions(): List<DeviceSession>? {
        return when (val result = authRemoteDataSource.getSessions().trackConnectivity()) {
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = result.error))
                null
            }
            is NetworkResult.Success -> result.data.map { it.toDeviceSession() }
        }
    }

    /**
     * The server half of the account-wide logout (the local half is
     * [org.lerchenflo.schneaggchatv3mp.app.domain.SessionTerminator.logoutAllDevices]).
     * Not best effort: if the server call fails, the other devices are still logged in.
     * @return true if the server logged the account out everywhere
     */
    override suspend fun logoutAllDevicesOnServer(): Boolean {
        val refreshToken = authSessionManager.currentTokens()?.refreshToken
            ?.takeIf { it.isNotBlank() && JwtUtils.isTokenDateValid(it) }

        return when (val result = authRemoteDataSource.logoutAllDevices(refreshToken).trackConnectivity()) {
            is NetworkResult.Error<*> -> {
                loggingRepository.logWarning("Logout all devices failed (${result.error.errorCode}): ${result.error.message}")
                errorChannel.sendErrorSuspend(ErrorEvent(error = result.error))
                false
            }
            is NetworkResult.Success<*> -> true
        }
    }

    /**
     * Logs one of the own devices out remotely.
     */
    override suspend fun endSession(sessionId: String): Boolean {
        return when (val result = authRemoteDataSource.endSession(sessionId).trackConnectivity()) {
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = result.error))
                false
            }
            is NetworkResult.Success<*> -> true
        }
    }
}

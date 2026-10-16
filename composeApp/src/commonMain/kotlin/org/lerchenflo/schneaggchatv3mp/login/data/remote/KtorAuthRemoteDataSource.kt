package org.lerchenflo.schneaggchatv3mp.login.data.remote

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.appendProfilePic
import org.lerchenflo.schneaggchatv3mp.login.data.remote.dto.LoginRequest
import org.lerchenflo.schneaggchatv3mp.login.data.remote.dto.LogoutRequest
import org.lerchenflo.schneaggchatv3mp.login.data.remote.dto.NotificationTokenRequest
import org.lerchenflo.schneaggchatv3mp.login.data.remote.dto.PasswordChangeRequest
import org.lerchenflo.schneaggchatv3mp.login.data.remote.dto.SessionResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.TokenPair
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion

/** [AuthRemoteDataSource] over Ktor, against the server URL from [Preferencemanager]. */
class KtorAuthRemoteDataSource(
    httpClient: HttpClient,
    authHttpClient: HttpClient, //For auth without the bearer
    preferenceManager: Preferencemanager,
    loggingRepository: Logger,
    private val appVersion: AppVersion,
) : AuthRemoteDataSource {

    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    // Without the bearer token, see authHttpClient
    private val authServer = ServerClient(authHttpClient, preferenceManager, loggingRepository)

    override suspend fun login(username: String, password: String): NetworkResult<TokenPair, NetworkingError> {
        return authServer.safePost<LoginRequest, TokenPair>(
            endpoint = "/auth/login",
            body = LoginRequest(username = username, password = password, deviceName = appVersion.getDeviceName(), deviceType = appVersion.getDeviceType())
        )
    }

    override suspend fun register(
        username: String,
        password: String,
        email: String,
        birthDate: String,
        profilePicBytes: ByteArray,
        phoneNumber: String?,
        language: String?,
        fileName: String
    ): NetworkResult<Unit, NetworkingError> {
        return authServer.safePostMultipart(
            endpoint = "/auth/register",
            parts = {
                append("username", username)
                append("password", password)
                append("email", email)
                append("birthDate", birthDate)
                if (!phoneNumber.isNullOrBlank()) {
                    append("phoneNumber", phoneNumber)
                }
                if (!language.isNullOrBlank()) {
                    append("language", language)
                }
                appendProfilePic(profilePicBytes, fileName)
            }
        )
    }

    override suspend fun sendEmailVerify(): NetworkResult<Unit, NetworkingError> {
        return server.safePost<String, Unit>(
            endpoint = "/users/verificationemail",
            body = ""
        )
    }

    override suspend fun logout(
        refreshToken: String?,
        notificationToken: String?,
        isAndroid: Boolean,
    ): NetworkResult<Unit, NetworkingError> {
        return authServer.safePost<LogoutRequest, Unit>(
            endpoint = "/auth/logout",
            body = LogoutRequest(
                refreshToken = refreshToken,
                notificationToken = notificationToken?.ifBlank { null },
                isAndroid = if (notificationToken.isNullOrBlank()) null else isAndroid,
            )
        )
    }

    override suspend fun logoutAllDevices(refreshToken: String?): NetworkResult<Unit, NetworkingError> {
        return server.safePost<LogoutRequest, Unit>(
            endpoint = "/auth/logout",
            body = LogoutRequest(
                refreshToken = refreshToken,
                allDevices = true,
            )
        )
    }

    override suspend fun getSessions(): NetworkResult<List<SessionResponse>, NetworkingError> {
        return server.safeGet(endpoint = "/sessions")
    }

    override suspend fun endSession(sessionId: String): NetworkResult<Unit, NetworkingError> {
        return server.safeDelete(endpoint = "/sessions/$sessionId")
    }

    override suspend fun setNotificationToken(token: String, isAndroid: Boolean) : NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/users/setnotificationtoken",
            body = NotificationTokenRequest(
                token = token,
                isAndroid = isAndroid
            )
        )
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String) : NetworkResult<Any, NetworkingError> {
        return server.safePost(
            endpoint = "/users/changepassword",
            body = PasswordChangeRequest(
                oldPassword = oldPassword,
                newPassword = newPassword
            )
        )
    }
}

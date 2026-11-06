package org.lerchenflo.schneaggchatv3mp.fakes

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult
import org.lerchenflo.schneaggchatv3mp.login.domain.AuthRepository
import org.lerchenflo.schneaggchatv3mp.login.domain.DeviceSession
import org.lerchenflo.schneaggchatv3mp.login.domain.SavedCredsResult

/**
 * [AuthRepository] that records the calls and answers with [result] / [sessions] /
 * [savedCredsResult]. It never touches `SessionCache`; tests log in with `loginTestUser`.
 */
class FakeAuthRepository(
    var result: Boolean = true,
    var sessions: List<DeviceSession>? = emptyList(),
    var savedCredsResult: SavedCredsResult? = null,
) : AuthRepository {

    val loginCalls = CallLog<Pair<String, String>>()
    val notificationTokens = CallLog<String>()
    val endedSessions = CallLog<String>()
    val endedServerSessions = CallLog<String?>()
    val logoutAllDevicesCalls = CallLog<Unit>()

    override suspend fun login(username: String, password: String): EmptyDataResult<DataError> {
        loginCalls.record(username to password)
        return result.toFakeResult()
    }

    override suspend fun createAccount(
        username: String,
        email: String,
        password: String,
        birthdate: String,
        profilePic: ByteArray,
        phoneNumber: String?,
        language: String?,
    ): EmptyDataResult<DataError> = result.toFakeResult()

    override suspend fun loadSavedLoginConfig(): SavedCredsResult =
        checkNotNull(savedCredsResult) { "FakeAuthRepository.savedCredsResult is not set" }

    override suspend fun sendEmailVerify(): EmptyDataResult<DataError> = result.toFakeResult()

    override suspend fun changePassword(oldPassword: String, newPassword: String): EmptyDataResult<DataError> = result.toFakeResult()

    override suspend fun getSessions(): DataResult<List<DeviceSession>, DataError> = sessions.toFakeDataResult()

    override suspend fun endSession(sessionId: String): EmptyDataResult<DataError> {
        endedSessions.record(sessionId)
        return result.toFakeResult()
    }

    override suspend fun setNotificationToken(token: String) = notificationTokens.record(token)

    override suspend fun endServerSession(refreshToken: String?) = endedServerSessions.record(refreshToken)

    override suspend fun logoutAllDevicesOnServer(): EmptyDataResult<DataError> {
        logoutAllDevicesCalls.record(Unit)
        return result.toFakeResult()
    }
}

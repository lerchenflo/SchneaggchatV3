package org.lerchenflo.schneaggchatv3mp.fakes

import org.lerchenflo.schneaggchatv3mp.datasource.domain.AuthRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.DeviceSession
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.SavedCredsResult

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

    override suspend fun login(username: String, password: String, onResult: (Boolean) -> Unit) {
        loginCalls.record(username to password)
        onResult(result)
    }

    override suspend fun createAccount(
        username: String,
        email: String,
        password: String,
        birthdate: String,
        profilePic: ByteArray,
        phoneNumber: String?,
        language: String?,
        onResult: (Boolean) -> Unit,
    ) = onResult(result)

    override suspend fun loadSavedLoginConfig(): SavedCredsResult =
        checkNotNull(savedCredsResult) { "FakeAuthRepository.savedCredsResult is not set" }

    override suspend fun sendEmailVerify(): Boolean = result

    override suspend fun changePassword(oldPassword: String, newPassword: String): Boolean = result

    override suspend fun getSessions(): List<DeviceSession>? = sessions

    override suspend fun endSession(sessionId: String): Boolean {
        endedSessions.record(sessionId)
        return result
    }

    override suspend fun setNotificationToken(token: String) = notificationTokens.record(token)

    override suspend fun endServerSession(refreshToken: String?) = endedServerSessions.record(refreshToken)

    override suspend fun logoutAllDevicesOnServer(): Boolean {
        logoutAllDevicesCalls.record(Unit)
        return result
    }
}

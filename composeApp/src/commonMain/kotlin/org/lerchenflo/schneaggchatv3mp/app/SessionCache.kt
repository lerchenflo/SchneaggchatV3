package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton session cache that combines Compose-friendly mutable state and
 * kotlinx.coroutines StateFlows for synchronous reads (value) and reactive observers.
 *
 * - Read synchronously with the `*Value()` helpers (non-suspending) or via the `*Flow`/properties.
 * - Observe reactively with e.g. `SessionCache.ownIdFlow`.
 * - Update with the provided `update...` methods which keep both StateFlow and
 *   Compose state in sync.
 *
 * NOTE: helper methods are named `get*Value()` / `is*Value()` to avoid JVM signature
 * collisions with Kotlin-generated property getters (e.g. `getOwnId()`).
 */
object SessionCache {

    // --------------------- sessionId ---------------------
    private val _sessionIdFlow = MutableStateFlow<String?>(null)
    val sessionIdFlow: StateFlow<String?> = _sessionIdFlow.asStateFlow()

    var sessionId: String? by mutableStateOf(null)
        private set

    fun updateSessionId(newValue: String?) {
        sessionId = newValue
        _sessionIdFlow.value = newValue
    }

    // synchronous, non-colliding helper
    fun getSessionIdValue(): String? = _sessionIdFlow.value

    // --------------------- ownId ---------------------
    // Keep -1L as initial value to preserve previous behaviour (was -1)
    private val _ownIdFlow = MutableStateFlow<Long?>(-1L)
    val ownIdFlow: StateFlow<Long?> = _ownIdFlow.asStateFlow()

    var ownId: Long? by mutableStateOf(-1L)
        private set

    fun updateOwnId(newValue: Long?) {
        ownId = newValue
        _ownIdFlow.value = newValue
        // keep developer flag in sync
        developer = (newValue == 1L || newValue == 2L)
    }

    // synchronous, non-colliding helper
    fun getOwnIdValue(): Long? = _ownIdFlow.value

    // --------------------- developer flag ---------------------
    var developer: Boolean by mutableStateOf((getOwnIdValue() == 1L || getOwnIdValue() == 2L))
        private set

    // if you need to set developer manually (rare), provide a setter
    fun setDeveloperValue(value: Boolean) {
        developer = value
    }

    // --------------------- username ---------------------
    private val _usernameFlow = MutableStateFlow<String>("")
    val usernameFlow: StateFlow<String> = _usernameFlow.asStateFlow()

    var username: String by mutableStateOf("")
        private set

    fun updateUsername(newValue: String) {
        username = newValue
        _usernameFlow.value = newValue
    }

    // synchronous, non-colliding helper
    fun getUsernameValue(): String = _usernameFlow.value

    // --------------------- password (do not print) ---------------------
    var passwordDonotprint: String by mutableStateOf("")
        private set

    fun updatePassword(newValue: String) {
        passwordDonotprint = newValue
    }

    // --------------------- loggedIn ---------------------
    private val _loggedInFlow = MutableStateFlow(false)
    val loggedInFlow: StateFlow<Boolean> = _loggedInFlow.asStateFlow()

    var loggedIn: Boolean by mutableStateOf(false)
        private set

    fun updateLoggedIn(newValue: Boolean) {
        loggedIn = newValue
        _loggedInFlow.value = newValue
    }

    // synchronous, non-colliding helper
    fun isLoggedInValue(): Boolean = _loggedInFlow.value

    // --------------------- online ---------------------
    private val _onlineFlow = MutableStateFlow(true)
    val onlineFlow: StateFlow<Boolean> = _onlineFlow.asStateFlow()

    var online: Boolean by mutableStateOf(true)
        private set

    fun updateOnline(newValue: Boolean) {
        online = newValue
        _onlineFlow.value = newValue
    }

    // synchronous, non-colliding helper
    fun isOnlineValue(): Boolean = _onlineFlow.value

    // --------------------- clear / helpers ---------------------
    fun clear() {
        updateSessionId(null)
        updateOwnId(null)
        updateUsername("")
        updatePassword("")
        updateLoggedIn(false)
        updateOnline(true)
        setDeveloperValue(false)
    }

    override fun toString(): String {
        val sid = sessionId?.let {
            if (it.length <= 8) it else it.substring(0, 6) + "..."
        } ?: "null"

        val own = getOwnIdValue()?.toString() ?: "null"
        val pwd = if (passwordDonotprint.isEmpty()) "<empty>" else "<redacted>"

        return "SessionCache(sessionId=$sid, ownId=$own, username=\"$username\", password=$pwd, loggedIn=$loggedIn, online=$online)"
    }
}

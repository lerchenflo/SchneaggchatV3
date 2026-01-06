package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils

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

    var tokens: NetworkUtils.TokenPair? by mutableStateOf(null)
        private set

    fun updateTokenPair(newValue: NetworkUtils.TokenPair?) {
        tokens = newValue
    }

    // synchronous, non-colliding helper
    fun getTokenPair(): NetworkUtils.TokenPair? = tokens

    // --------------------- ownId ---------------------

    private val _ownId = MutableStateFlow<String?>(null)
    val ownId = _ownId.asStateFlow()

    fun updateOwnId(newValue: String?) {
        _ownId.value = newValue
    }

    // synchronous, non-colliding helper
    fun getOwnIdValue(): String? = ownId.value

    // --------------------- developer flag ---------------------
    var developer: Boolean by mutableStateOf(false)
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
        _onlineFlow.value = newValue
        online = newValue
    }

    // synchronous, non-colliding helper
    fun isOnlineValue(): Boolean = _onlineFlow.value

    // --------------------- clear / helpers ---------------------
    fun clear() {
        updateTokenPair(null)
        updateOwnId(null)
        updateUsername("")
        updateLoggedIn(false)
        updateOnline(false)
        setDeveloperValue(false)
    }





    // Add this to the SessionCache object

    override fun toString(): String {
        return buildString {
            appendLine("SessionCache {")
            appendLine("  tokens: ${if (tokens != null) "[PRESENT]" else "null"}")
            appendLine("  ownId: ${ownId.value ?: "null"}")
            appendLine("  developer: $developer")
            appendLine("  username: ${username.ifEmpty { "[empty]" }}")
            appendLine("  loggedIn: $loggedIn")
            appendLine("  online: ${onlineFlow.value}")
            append("}")
        }
    }

}

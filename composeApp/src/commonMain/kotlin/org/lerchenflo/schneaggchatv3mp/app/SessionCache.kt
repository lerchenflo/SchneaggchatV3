package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.utilities.JwtUtils

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


    sealed class AuthState {
        data object LoggedOut : AuthState()

        data class LoggedIn(
            val userId: String,
            val tokens: NetworkUtils.TokenPair,
            val developer: Boolean = false,
        ) : AuthState()
    }




    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Convenience for Compose
    var authStateValue: AuthState by mutableStateOf(AuthState.LoggedOut)
        private set

    fun login(tokens: NetworkUtils.TokenPair, developer: Boolean = false) {
        val state = AuthState.LoggedIn(
            userId = userIdOf(tokens),
            tokens = tokens,
            developer = developer
        )
        _authState.value = state
        authStateValue = state
    }

    /**
     * Same as [login], but refuses tokens that are blank or already expired. Background/push
     * entry points (FCM, mark-as-read, iOS push bridge) rehydrate a session straight from storage
     * without going through the login/refresh network path - this keeps them from fabricating a
     * `LoggedIn(userId = "")` state out of empty or stale tokens. Returns whether login happened.
     */
    fun loginIfValid(tokens: NetworkUtils.TokenPair, developer: Boolean = false): Boolean {
        if (!JwtUtils.isTokenDateValid(tokens.refreshToken)) return false
        login(tokens, developer)
        return true
    }

    fun logout() {
        _authState.value = AuthState.LoggedOut
        authStateValue = AuthState.LoggedOut
    }

    fun updateTokens(newTokens: NetworkUtils.TokenPair) {
        val userId = userIdOf(newTokens)
        _authState.update { currentState ->
            when (currentState) {
                is AuthState.LoggedIn -> currentState.copy(
                    tokens = newTokens,
                    userId = userId
                )
                is AuthState.LoggedOut -> AuthState.LoggedIn(
                    userId = userId,
                    tokens = newTokens
                )
            }
        }
        authStateValue = _authState.value
    }

    fun updateDeveloper(developer: Boolean) {
        _authState.update { currentState ->
            when (currentState) {
                is AuthState.LoggedIn -> currentState.copy(developer = developer)
                is AuthState.LoggedOut -> currentState
            }
        }
        authStateValue = _authState.value
    }

    // --------------------- online ---------------------
    private val _onlineFlow = MutableStateFlow(true)
    val onlineFlow: StateFlow<Boolean> = _onlineFlow.asStateFlow()

    fun updateOnline(newValue: Boolean) {
        _onlineFlow.value = newValue
    }

    fun isOnline(): Boolean = _onlineFlow.value


    // --------------------- helpers ---------------------
    /**
     * Pure read. Re-establishing a session from storage is `AuthSessionManager.ensureSession()`'s
     * job - a read must never drive the session state machine (it used to raise a Login action
     * here, which together with the 5 s connectivity loop produced a refresh storm).
     */
    fun requireLoggedIn(): AuthState.LoggedIn? = _authState.value as? AuthState.LoggedIn

    fun isLoggedIn(): Boolean = _authState.value is AuthState.LoggedIn

    // Both tokens carry the same subject; the refresh token is the fallback for a truncated
    // access token so a session never ends up as LoggedIn(userId = "").
    private fun userIdOf(tokens: NetworkUtils.TokenPair): String =
        JwtUtils.getUserIdFromToken(tokens.accessToken).ifBlank { JwtUtils.getUserIdFromToken(tokens.refreshToken) }



    override fun toString(): String = buildString {
        appendLine("╔══════════ SessionCache ══════════")

        when (val state = _authState.value) {
            is AuthState.LoggedOut -> {
                appendLine("║  auth     : ✗ LoggedOut")
            }
            is AuthState.LoggedIn -> {
                appendLine("║  auth     : ✓ LoggedIn")
                appendLine("║  userId   : ${state.userId}")
                appendLine("║  developer: ${if (state.developer) "✓ yes" else "✗ no"}")
                appendLine("║  tokens   : ${if (state.tokens.accessToken.isNotBlank()) "✓ present" else "✗ missing"}")
            }
        }

        appendLine("║  online   : ${if (_onlineFlow.value) "✓ yes" else "✗ no"}")
        appendLine("╚══════════════════════════════════")
    }
}

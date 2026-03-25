package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
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
            userId = JwtUtils.getUserIdFromToken(tokens.accessToken),
            tokens = tokens,
            developer = developer
        )
        _authState.value = state
        authStateValue = state
    }

    fun logout() {
        _authState.value = AuthState.LoggedOut
        authStateValue = AuthState.LoggedOut
    }

    fun updateTokens(newTokens: NetworkUtils.TokenPair) {
        val userId = JwtUtils.getUserIdFromToken(newTokens.accessToken)
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

    // --------------------- online ---------------------
    private val _onlineFlow = MutableStateFlow(true)
    val onlineFlow: StateFlow<Boolean> = _onlineFlow.asStateFlow()

    fun updateOnline(newValue: Boolean) {
        _onlineFlow.value = newValue
    }

    fun isOnline(): Boolean = _onlineFlow.value


    // --------------------- helpers ---------------------
    fun requireLoggedIn(): AuthState.LoggedIn? {
        val loggedIn = _authState.value as? AuthState.LoggedIn
        if (loggedIn == null) {
            println("user not logged in, sending login request")
            AppRepository.ActionChannel.trySendAction(AppRepository.ActionChannel.ActionEvent.Login)
        }
        return loggedIn
    }

    fun isLoggedIn(): Boolean = _authState.value is AuthState.LoggedIn



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

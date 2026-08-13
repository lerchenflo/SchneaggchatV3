package org.lerchenflo.schneaggchatv3mp.login.presentation.autologincredchecker

data class AutoLoginCredCheckerState(
    val isLoading: Boolean = true
)

sealed interface AutoLoginCredCheckerAction {
    data object CheckCredentials : AutoLoginCredCheckerAction
}

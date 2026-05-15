package org.lerchenflo.schneaggchatv3mp.login.presentation.emailverifiedcheck

import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.utilities.UiText

data class EmailVerifiedCheckState(
    val userData: User? = null,
    val isLoading: Boolean = false,

    val currentEmail: String = "",
    val showChangeEmailPopup: Boolean = false
)

sealed interface EmailVerifiedCheckAction {

    data object OnChangeEmailStart: EmailVerifiedCheckAction
    data class OnChangeEmailText(val text: String) : EmailVerifiedCheckAction
    data object OnChangeEmailDismiss: EmailVerifiedCheckAction

    data object OnRequestSupportClick: EmailVerifiedCheckAction

    data object OnResendEmailClick : EmailVerifiedCheckAction
    data object OnLogoutClick : EmailVerifiedCheckAction
    data object OnCheckVerificationClick : EmailVerifiedCheckAction
}

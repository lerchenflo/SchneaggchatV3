package org.lerchenflo.schneaggchatv3mp.login.presentation.signup

import androidx.compose.ui.focus.FocusRequester

data class SignupFocusRequesters(
    val profilePic: FocusRequester,
    val username: FocusRequester,
    val email: FocusRequester,
    val date: FocusRequester,
    val password: FocusRequester,
    val password2: FocusRequester,
    val terms: FocusRequester,
    val signup: FocusRequester,
)


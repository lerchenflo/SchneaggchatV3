package org.lerchenflo.schneaggchatv3mp.login.presentation.signup

data class SignupState(
    val createButtonDisabled: Boolean = true,
    val isLoading: Boolean = false,

    val usernameState: InputfieldState = InputfieldState(),
    val passwordState: InputfieldState = InputfieldState(),
    val passwordRetypeState: InputfieldState = InputfieldState(),
    val emailState: InputfieldState = InputfieldState(),
    val agbsAccepted: Boolean = false

    )

data class InputfieldState(
    val text: String = "",
    val errorMessage: String? = null,
    val enabled: Boolean = true
){
    fun isCorrect() : Boolean{
        return !text.isEmpty() && errorMessage == null
    }
}

sealed interface SignupAction {
    data class OnUsernameTextChange(val newText: String) : SignupAction
    data class OnPasswordTextChange(val newText: String, val retrypasswordfield: Boolean) : SignupAction
    data class OnEmailTextChange(val newText: String) : SignupAction
    data object OnSignUpButtonPress : SignupAction

    data class OnAgbChecked(val checked: Boolean) : SignupAction
}

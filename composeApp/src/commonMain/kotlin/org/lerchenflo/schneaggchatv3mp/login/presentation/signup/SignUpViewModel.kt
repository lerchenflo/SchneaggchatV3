package org.lerchenflo.schneaggchatv3mp.login.presentation.signup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.SessionCache.username
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.network.util.ResponseReason
import org.lerchenflo.schneaggchatv3mp.network.util.toEnumOrNull
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cannot_be_empty
import schneaggchatv3mp.composeapp.generated.resources.email_exists
import schneaggchatv3mp.composeapp.generated.resources.invalid_email
import schneaggchatv3mp.composeapp.generated.resources.password_needs_to_be_the_same
import schneaggchatv3mp.composeapp.generated.resources.requirement_digit
import schneaggchatv3mp.composeapp.generated.resources.requirement_forbidden
import schneaggchatv3mp.composeapp.generated.resources.requirement_length
import schneaggchatv3mp.composeapp.generated.resources.requirement_special
import schneaggchatv3mp.composeapp.generated.resources.unknown_error
import schneaggchatv3mp.composeapp.generated.resources.username_exists
import schneaggchatv3mp.composeapp.generated.resources.username_too_long


class SignUpViewModel(
    private val appRepository: AppRepository
): ViewModel() {

    var state by mutableStateOf(SignupState())
        private set


    fun onAction(action: SignupAction){
        viewModelScope.launch {
            val x = /* Variable for exhaustive when */when(action){
                is SignupAction.OnUsernameTextChange -> {
                    state = state.copy(
                        usernameState = state.usernameState.copy(
                            text = action.newText,
                            errorMessage = if (action.newText.isEmpty()) getString(Res.string.cannot_be_empty) else null
                        )
                    )
                }

                is SignupAction.OnEmailTextChange -> {
                    state = state.copy(
                        emailState = state.emailState.copy(
                            text = action.newText,
                            errorMessage = if (isEmailValid(action.newText)) null else getString(Res.string.invalid_email)
                        )
                    )
                }
                is SignupAction.OnPasswordTextChange -> {
                    state = if (!action.retrypasswordfield) {

                        val missing = getMissingPasswordRequirements(action.newText)

                        val errormessage = if (action.newText.isEmpty()) {
                            getString(Res.string.cannot_be_empty)
                        }else if (!missing.isEmpty()){
                            missing.joinToString("\n")
                        }else null


                        state.copy(
                            passwordState = state.passwordState.copy(
                                text = action.newText,
                                errorMessage = errormessage
                            )
                        )
                    }else {
                        state.copy(
                            passwordRetypeState = state.passwordRetypeState.copy(
                                text = action.newText,
                                errorMessage = if (action.newText == state.passwordState.text) null else getString(Res.string.password_needs_to_be_the_same)
                            )
                        )
                    }
                }
                SignupAction.OnSignUpButtonPress -> {
                    //TODO: Navigate with global navigator
                }

                is SignupAction.OnAgbChecked -> {
                    state = state.copy(
                        agbsAccepted = action.checked
                    )
                }

                is SignupAction.OnGebiDateChange -> {
                    state = state.copy(
                        gebiDate = action.newDate
                    )
                }
            }
        }

        if (isWithoutErrors()) {
            state = state.copy(
                createButtonDisabled = false
            )
        }
    }


    fun isWithoutErrors() : Boolean{
        return state.usernameState.isCorrect()
                && state.passwordState.isCorrect()
                && state.passwordRetypeState.isCorrect()
                && state.emailState.isCorrect()
                && state.gebiDate != null
                && state.agbsAccepted
    }


    /* TODO
    fun signup(onCreateSuccess: () -> Unit) {
        viewModelScope.launch {
            if (validateInput()) {
                try {
                    state = state.copy(
                        isLoading = true
                    )

                    // Use the sharedViewModel's login function with a callback
                    appRepository.createAccount(username, email, password, gender, gebidate.toString()) { success, message ->
                        if (success) {
                            println("Account erstellen erfolgreich")

                            appRepository.login(username, password) { success, message ->
                                if (success){
                                    onCreateSuccess()
                                }
                            }


                        } else {

                            viewModelScope.launch {
                                val responsereason = message.toEnumOrNull<ResponseReason>(true)

                                if (responsereason == ResponseReason.exists){
                                    usernameerrorMessage = getString(Res.string.username_exists)
                                }else if (responsereason == ResponseReason.email_exists){
                                    emailerrorMessage = getString(Res.string.email_exists)
                                }else if (responsereason == ResponseReason.too_big){
                                    usernameerrorMessage = getString(Res.string.username_too_long)
                                }else if (responsereason == ResponseReason.unknown_error){
                                    password2errorMessage = getString(Res.string.unknown_error)
                                }

                            }

                        }
                    }

                } catch (e: Exception) {
                    password2errorMessage = e.message
                } finally {
                    isLoading = false
                }
            }
        }

    }

     */


    private fun isEmailValid(email: String): Boolean {
        // More comprehensive RFC 5322 compliant regex
        val emailRegex = "^[a-zA-Z0-9.!#\$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\$"

        println("Validating email: $email")

        // Additional checks beyond regex
        if (email.isEmpty()) {
            println("Email validation failed: Email is empty")
            return false
        }

        if (email.length > 254) {
            println("Email validation failed: Email exceeds maximum length of 254 characters")
            return false
        }

        // Check for exactly one @ symbol
        val atCount = email.count { it == '@' }
        if (atCount != 1) {
            println("Email validation failed: Email must contain exactly one @ symbol (found $atCount)")
            return false
        }

        // Split into local and domain parts
        val parts = email.split('@')
        val localPart = parts[0]
        val domainPart = parts[1]

        // Check local part length (max 64 chars per RFC)
        if (localPart.isEmpty()) {
            println("Email validation failed: Local part (before @) is empty")
            return false
        }

        if (localPart.length > 64) {
            println("Email validation failed: Local part exceeds maximum length of 64 characters")
            return false
        }

        // Check domain part
        if (domainPart.isEmpty()) {
            println("Email validation failed: Domain part (after @) is empty")
            return false
        }

        // Check domain part has at least one dot
        if (!domainPart.contains('.')) {
            println("Email validation failed: Domain part must contain at least one dot")
            return false
        }

        // Check domain part segments
        val domainSegments = domainPart.split('.')
        for (segment in domainSegments) {
            if (segment.isEmpty()) {
                println("Email validation failed: Domain contains empty segment")
                return false
            }
            if (segment.length > 63) {
                println("Email validation failed: Domain segment '$segment' exceeds maximum length of 63 characters")
                return false
            }
        }

        // Final regex check
        if (!email.matches(emailRegex.toRegex())) {
            println("Email validation failed: Email format doesn't match RFC 5322 standard")
            return false
        }

        println("Email validation passed: $email")
        return true
    }


    private suspend fun getMissingPasswordRequirements(password: String): List<String> {
        val missing = mutableListOf<String>()

        // Length check
        if (password.length < 8) {
            missing.add(getString(Res.string.requirement_length))
        }

        // Digit check
        if (!password.any { it.isDigit() }) {
            missing.add(getString(Res.string.requirement_digit))
        }

        // Special character check
        if (!password.any { !it.isLetterOrDigit() }) {
            missing.add(getString(Res.string.requirement_special))
        }

        // Common forbidden patterns
        val forbiddenPatterns = listOf(
            "123", "234", "345", "456", "567", "678", "789", "890",
            "987", "876", "765", "654", "543", "432", "321", "210",
            "geheim", "test", "1234", "password", "qwerty", "asdfgh",
            "zxcvbn", "letmein", "admin", "welcome", "111111", "abc123",
            "passwort", "fortnite"
        )

        if (forbiddenPatterns.any { password.contains(it, ignoreCase = true) }) {
            missing.add(getString(Res.string.requirement_forbidden))
        }

        return missing
    }
}
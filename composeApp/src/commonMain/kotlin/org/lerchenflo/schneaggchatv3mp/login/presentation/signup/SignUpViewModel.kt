package org.lerchenflo.schneaggchatv3mp.login.presentation.signup

import androidx.compose.foundation.Image
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.Image
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Scale
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.SessionCache.updateUsername
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cannot_be_empty
import schneaggchatv3mp.composeapp.generated.resources.invalid_email
import schneaggchatv3mp.composeapp.generated.resources.password_needs_to_be_the_same
import schneaggchatv3mp.composeapp.generated.resources.requirement_digit
import schneaggchatv3mp.composeapp.generated.resources.requirement_forbidden
import schneaggchatv3mp.composeapp.generated.resources.requirement_length
import schneaggchatv3mp.composeapp.generated.resources.requirement_lowercase
import schneaggchatv3mp.composeapp.generated.resources.requirement_special
import schneaggchatv3mp.composeapp.generated.resources.requirement_uppercase


class SignUpViewModel(
    private val appRepository: AppRepository,
    private val navigator: Navigator
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
                    signup(
                        {
                            viewModelScope.launch {
                                navigator.navigate(Route.ChatSelector){
                                    popUpTo(Route.ChatGraph) { inclusive = true }
                                }
                            }
                        }
                    )
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

                SignupAction.OnBackClicked -> {
                    viewModelScope.launch {
                        navigator.navigate(Route.Login)
                    }
                }

                is SignupAction.OnProfilepicSelected -> {
                    CoroutineScope(Dispatchers.IO).launch{
                        val bytearray = action.profilePicResult
                            .loadBytes()
                        val imageBitmap = bytearray.decodeToImageBitmap()

                        println("Selected image size: ${bytearray.size}")

                        state = state.copy(
                            profilePic = bytearray
                        )
                    }

                }
            }
        }

        val enablebutton = isInputComplete()
        if (enablebutton) {
            state = state.copy(
                createButtonDisabled = !enablebutton
            )
        }
    }


    fun isInputComplete() : Boolean{
        return state.usernameState.isCorrect()
                && state.passwordState.isCorrect()
                && state.passwordRetypeState.isCorrect()
                && state.emailState.isCorrect()
                && state.gebiDate != null
                && state.profilePic != null
                && state.agbsAccepted
                && state.passwordState.text == state.passwordRetypeState.text
    }



    fun signup(onCreateSuccess: () -> Unit) {
        viewModelScope.launch {
            if (isInputComplete()) {
                try {
                    state = state.copy(
                        isLoading = true
                    )

                    appRepository.createAccount(state.usernameState.text, state.emailState.text, state.passwordState.text, state.gebiDate.toString(), state.profilePic!!) { success ->
                        if (success) {
                            println("Account erstellen erfolgreich")

                            appRepository.login(state.usernameState.text, state.passwordState.text) { success ->
                                if (success){
                                    //Set username
                                    updateUsername(state.usernameState.text)
                                    onCreateSuccess()
                                }
                            }

                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    state = state.copy(
                        isLoading = false
                    )
                }
            }
        }

    }



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

        // Uppercase letter check
        if (!password.any { it.isUpperCase() }) {
            missing.add(getString(Res.string.requirement_uppercase))
        }

        // Lowercase letter check
        if (!password.any { it.isLowerCase() }) {
            missing.add(getString(Res.string.requirement_lowercase))
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
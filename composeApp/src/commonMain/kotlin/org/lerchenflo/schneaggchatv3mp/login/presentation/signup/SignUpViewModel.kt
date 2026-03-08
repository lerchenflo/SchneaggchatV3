package org.lerchenflo.schneaggchatv3mp.login.presentation.signup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.getMissingPasswordRequirements
import org.lerchenflo.schneaggchatv3mp.utilities.isEmailValid
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cannot_be_empty
import schneaggchatv3mp.composeapp.generated.resources.invalid_email
import schneaggchatv3mp.composeapp.generated.resources.must_be_accepted
import schneaggchatv3mp.composeapp.generated.resources.password_needs_to_be_the_same
import schneaggchatv3mp.composeapp.generated.resources.username_too_long
import schneaggchatv3mp.composeapp.generated.resources.username_too_short
import schneaggchatv3mp.composeapp.generated.resources.you_need_to_select_a_profilepicture


class SignUpViewModel(
    private val appRepository: AppRepository,
    private val navigator: Navigator,
    private val pictureManager: PictureManager,
    private val loggingRepository: LoggingRepository
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
                            errorMessage = when {
                                action.newText.length <= 3 -> getString(Res.string.username_too_short)
                                action.newText.length >= 25 -> getString(Res.string.username_too_long)
                                else -> null
                            }
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

                is SignupAction.OnAgbChecked -> {
                    state = state.copy(
                        agbsAccepted = action.checked,
                        agbsErrorText = if (action.checked) null else getString(Res.string.must_be_accepted)
                    )

                }

                is SignupAction.OnGebiDateChange -> {
                    println("On gebi date change")
                    state = state.copy(
                        gebiDate = action.newDate,
                        gebiErrorText = null
                    )
                }

                is SignupAction.OnProfilepicSelected -> {
                    CoroutineScope(Dispatchers.Default).launch{ //Use one core
                        val bytearrayfullsize = action.profilePicResult
                            .loadBytes()

                        val bytearray = pictureManager.downscaleImage(bytearrayfullsize)

                        state = state.copy(
                            profilePic = bytearray,
                            profilePicErrorText = null
                        )

                    }
                }


                SignupAction.OnSignUpButtonPress -> {
                    signup()
                }

                SignupAction.OnBackClicked -> {
                    viewModelScope.launch {
                        navigator.navigate(Route.Login)
                    }
                }
            }
        }

    }

    private suspend fun checkenableCreateButton(): Boolean {
        if (state.isInputComplete()) return true

        // Load error strings once
        val cannotBeEmptyError = getString(Res.string.cannot_be_empty)
        val mustBeAcceptedError = getString(Res.string.must_be_accepted)
        val mustSetProfilepicError = getString(Res.string.you_need_to_select_a_profilepicture)

        println("Gebidate null: ${state.gebiDate == null}\ngebidate error: ${state.gebiErrorText}")

        // Update all errors in a single state copy - REMOVED runBlocking
        state = state.copy(
            usernameState = if (!state.usernameState.isCorrect()) {
                state.usernameState.copy(errorMessage = cannotBeEmptyError)
            } else {
                state.usernameState
            },
            passwordState = if (!state.passwordState.isCorrect()) {
                state.passwordState.copy(errorMessage = cannotBeEmptyError)
            } else {
                state.passwordState
            },
            passwordRetypeState = if (!state.passwordRetypeState.isCorrect()) {
                state.passwordRetypeState.copy(errorMessage = cannotBeEmptyError)
            } else {
                state.passwordRetypeState
            },
            emailState = if (!state.emailState.isCorrect()) {
                state.emailState.copy(errorMessage = cannotBeEmptyError)
            } else {
                state.emailState
            },
            // Fix: Always set error if gebiDate is null, clear if not
            gebiErrorText = if (state.gebiDate == null) cannotBeEmptyError else null,
            // Fix: Always set error if profilePic is null, clear if not
            profilePicErrorText = if (state.profilePic == null) mustSetProfilepicError else null,
            // Fix: Always set error if agbs not accepted, clear if accepted
            agbsErrorText = if (!state.agbsAccepted) mustBeAcceptedError else null
        )

        println("Setting errors finished: $state")

        return false
    }






    fun signup() {

        viewModelScope.launch {
            if (!checkenableCreateButton()) return@launch

            if (state.isInputComplete() && !state.isLoading) {
                try {
                    state = state.copy(
                        isLoading = true
                    )

                    var accCreationSuccessful = false
                    appRepository.createAccount(state.usernameState.text, state.emailState.text, state.passwordState.text, state.gebiDate.toString(), state.profilePic!!) { success ->
                        if (success) {
                            accCreationSuccessful = true
                            println("Account erstellen erfolgreich")
                        }
                    }

                    if (accCreationSuccessful){
                        appRepository.login(state.usernameState.text, state.passwordState.text) { success ->
                            if (success){
                                //Set username
                                viewModelScope.launch {
                                    navigator.navigate(Route.ChatSelector, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
                                }
                            }
                        }
                    }


                } catch (e: Exception) {
                    loggingRepository.logWarning("Signup failed: ${e.message}")
                } finally {
                    state = state.copy(
                        isLoading = false
                    )
                }
            }
        }

    }

}
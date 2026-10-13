package org.lerchenflo.schneaggchatv3mp.login.presentation.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.logging.LogEntry
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.datasource.domain.ErrorEvent
import org.lerchenflo.schneaggchatv3mp.datasource.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.server_not_reachable


class LoginViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val navigator: Navigator,
    private val applicationScope: ApplicationScope,
    private val loggingRepository: Logger,
    private val errorChannel: ErrorChannel,
): ViewModel() {

    var logs by mutableStateOf<List<LogEntry>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            loggingRepository.getLogs().collect { loglist ->
                logs = loglist
            }
        }
    }

    fun onClearLogs() {
        viewModelScope.launch {
            loggingRepository.clearLogs()
        }
    }


    var username by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var loginButtonDisabled by mutableStateOf(true)
        private set

    var isLoading by mutableStateOf(false)
        private set



    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Update username state
    fun updateUsername(newValue: String) {
        username = newValue
        clearError() // Clear error when user types
    }

    // Update password state
    fun updatePassword(newValue: String) {
        password = newValue
        clearError()
    }


    var serverUrl by mutableStateOf("")
        private set

    fun updateServerUrl(newValue: String) {
        serverUrl = newValue
        viewModelScope.launch {
            settingsRepository.saveServerUrl(newValue)
        }
    }





    init {

        viewModelScope.launch {
            val online =  appRepository.testServer(settingsRepository.getServerUrl())
            if (!online){
                errorChannel.sendErrorSuspend(
                    event = ErrorEvent(
                        errorCode = 408,
                        errorMessage = "ServerUrl: " + settingsRepository.getServerUrl(),
                        errorMessageUiText = UiText.StringResourceText(Res.string.server_not_reachable),
                        duration = 6000L
                    )
                )
            }
        }

        viewModelScope.launch {
            serverUrl = settingsRepository.getServerUrl()
        }
    }




    // Handle login logic
    fun login() {
        if (!isLoading) {
            isLoading = true
            viewModelScope.launch {
                try {

                    // Use the sharedViewModel's login function with a callback
                    appRepository.login(username, password) { success ->
                        if (success) {
                            println("Login erfolgreich, triggering data sync")
                            applicationScope.launch {
                                appRepository.dataSync(reason = "loginSuccess")
                            }
                            viewModelScope.launch {
                                navigator.navigate(Route.EmailVerifiedCheck, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
                            }
                        }
                    }

                } catch (e: Exception) {
                    ensureActive()
                    errorMessage = "Connection error: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    fun navigateSignUp(){
        viewModelScope.launch {
            navigator.navigate(Route.SignUp)
        }
    }

    private fun clearError() {
        errorMessage = null

        loginButtonDisabled = !(username.isNotBlank() && password.isNotBlank())
    }
}
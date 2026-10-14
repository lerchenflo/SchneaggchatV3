package org.lerchenflo.schneaggchatv3mp.login.presentation.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.app.logging.LogEntry
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.app.domain.AppInfoRepository
import org.lerchenflo.schneaggchatv3mp.login.domain.AuthRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorEvent
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.settings.domain.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.CheckStatus
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.server_not_reachable


class LoginViewModel(
    private val dataSyncCoordinator: DataSyncCoordinator,
    private val authRepository: AuthRepository,
    private val appInfoRepository: AppInfoRepository,
    private val settingsRepository: SettingsRepository,
    private val navigator: Navigator,
    private val loggingRepository: Logger,
    private val errorChannel: ErrorChannel,
    private val languageService: LanguageService,
    appVersion: AppVersion,
): ViewModel() {

    // Shown in the version text under the login form
    val versionName: String = appVersion.getVersionName()
    val versionCode: String = appVersion.getVersionCode()

    /** The server URL as saved right now; the website and password-reset links open this one. */
    val savedServerUrl: StateFlow<String> = settingsRepository.getServerUrlFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, BASE_SERVER_URL)

    // ─── Language: German system language -> offer Vorarlbergerisch ────────────
    var showVoriLanguageDialog by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val language = languageService.getCurrentLanguage()

            if (language == LanguageSetting.SYSTEM) {
                val systemlanguage = languageService.getSystemLanguage()
                println("system language: $systemlanguage")

                if (systemlanguage.trim().startsWith("de") || systemlanguage.trim().startsWith("DE")) {
                    showVoriLanguageDialog = true
                }
            }
        }
    }

    fun onLanguageSelected(language: LanguageSetting) {
        viewModelScope.launch {
            languageService.applyLanguage(language)
            showVoriLanguageDialog = false
        }
    }

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

    // ─── Server test of the UrlChangeDialog ────────────────────────────────────
    var serverCheckStatus by mutableStateOf<CheckStatus>(CheckStatus.Idle)
        private set

    // Child scope of viewModelScope for the running server tests, so closing the dialog cancels all of them
    private val serverCheckScope = CoroutineScope(
        viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job])
    )

    fun testServer(serverUrl: String) {
        if (serverCheckStatus == CheckStatus.Loading) return
        serverCheckStatus = CheckStatus.Loading
        serverCheckScope.launch {
            val success = appInfoRepository.testServer(serverUrl)
            ensureActive() // reset in the meantime: drop the result

            serverCheckStatus = if (success){
                CheckStatus.Success
            } else CheckStatus.Error
        }
    }

    fun onServerUrlEdited() {
        serverCheckStatus = CheckStatus.Idle
    }

    /** The dialog opens or closes: drops running tests and their result, like the dialog's own state used to. */
    fun resetServerCheck() {
        serverCheckScope.coroutineContext.cancelChildren()
        serverCheckStatus = CheckStatus.Idle
    }





    init {

        viewModelScope.launch {
            val online =  appInfoRepository.testServer(settingsRepository.getServerUrl())
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
                    authRepository.login(username, password) { success ->
                        if (success) {
                            println("Login erfolgreich, triggering data sync")
                            // UNDISPATCHED: the sync is started before the navigation below can clear
                            // this ViewModel; it then runs in the app-wide scope (plan R2).
                            viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                dataSyncCoordinator.dataSync(reason = "loginSuccess")
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
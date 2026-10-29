package org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.app.domain.AppInfoRepository
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.CheckStatus
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.dev_settings_tour_skipped

class DevSettingsViewModel(
    private val loggingRepository: Logger,
    private val navigator: Navigator,
    private val settingsRepository: SettingsRepository,
    private val appInfoRepository: AppInfoRepository,
): ViewModel() {

    val demoMode = settingsRepository.getDemoModeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // ─── Server test of the UrlChangeDialog ────────────────────────────────────
    private val _serverCheckStatus = MutableStateFlow<CheckStatus>(CheckStatus.Idle)
    val serverCheckStatus: StateFlow<CheckStatus> = _serverCheckStatus.asStateFlow()

    // Child scope of viewModelScope for the running server tests, so closing the dialog cancels all of them
    private val serverCheckScope = CoroutineScope(
        viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job])
    )

    fun testServer(serverUrl: String) {
        if (_serverCheckStatus.value == CheckStatus.Loading) return
        _serverCheckStatus.value = CheckStatus.Loading
        serverCheckScope.launch {
            val success = appInfoRepository.testServer(serverUrl)
            ensureActive() // reset in the meantime: drop the result

            _serverCheckStatus.value = if (success){
                CheckStatus.Success
            } else CheckStatus.Error
        }
    }

    fun onServerUrlEdited() {
        _serverCheckStatus.value = CheckStatus.Idle
    }

    /** The dialog opens or closes: drops running tests and their result, like the dialog's own state used to. */
    fun resetServerCheck() {
        serverCheckScope.coroutineContext.cancelChildren()
        _serverCheckStatus.value = CheckStatus.Idle
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveDemoMode(enabled)
        }
    }

    fun skipOnboardingTour() {
        viewModelScope.launch {
            settingsRepository.setOnboardingSeen(true)
            SnackbarManager.showMessage(getString(Res.string.dev_settings_tour_skipped))
        }
    }


    fun navigateGames() {
        viewModelScope.launch {
            navigator.navigate(Route.GamesSelector)
        }
    }

    fun navigateRecap() {
        viewModelScope.launch {
            navigator.navigate(
                destination = Route.Recap,
                //navigationOptions =
            )
        }
    }

    fun navigateEvents() {
        viewModelScope.launch {
            navigator.navigate(Route.Events())
        }
    }

}
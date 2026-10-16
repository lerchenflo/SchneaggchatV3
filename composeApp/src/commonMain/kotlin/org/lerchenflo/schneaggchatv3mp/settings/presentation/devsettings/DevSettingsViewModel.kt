package org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.dev_settings_tour_skipped

class DevSettingsViewModel(
    private val loggingRepository: LoggingRepository,
    private val navigator: Navigator,
    private val preferenceManager: Preferencemanager
): ViewModel() {

    val demoMode = preferenceManager.getDemoModeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.saveDemoMode(enabled)
        }
    }

    fun skipOnboardingTour() {
        viewModelScope.launch {
            preferenceManager.setOnboardingSeen(true)
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
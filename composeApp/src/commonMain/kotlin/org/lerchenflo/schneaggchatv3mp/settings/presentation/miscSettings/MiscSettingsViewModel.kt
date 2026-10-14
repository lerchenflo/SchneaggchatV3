package org.lerchenflo.schneaggchatv3mp.settings.presentation.miscSettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.logging.LogEntry
import org.lerchenflo.schneaggchatv3mp.datasource.domain.Logger
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.SessionTerminator
import org.lerchenflo.schneaggchatv3mp.datasource.domain.SettingsRepository

class MiscSettingsViewModel(
    private val appRepository: AppRepository,
    private val sessionTerminator: SessionTerminator,
    private val navigator: Navigator,
    private val loggingRepository: Logger,
    private val applicationScope: ApplicationScope,
    private val settingsRepository: SettingsRepository
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

    fun deleteAllAppData(){
        viewModelScope.launch {
            sessionTerminator.deleteAllAppData()
            applicationScope.launch {
                appRepository.dataSync(reason = "appDataDeleted") //Trigger datasync that user does not get stuck in the email verify screen
            }
            navigator.navigate(Route.AutoLoginCredChecker, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))

        }
    }

    fun resetOnboardingTour() {
        viewModelScope.launch {
            settingsRepository.setOnboardingSeen(false)
        }
    }



}
package org.lerchenflo.schneaggchatv3mp.settings.presentation.miscSettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.logging.LogEntry
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.app.domain.SessionTerminator
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.launchOnMain

class MiscSettingsViewModel(
    private val dataSyncCoordinator: DataSyncCoordinator,
    private val sessionTerminator: SessionTerminator,
    private val navigator: Navigator,
    private val loggingRepository: Logger,
    private val settingsRepository: SettingsRepository,
    private val applicationScope: ApplicationScope,
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

    fun resetLocalData(){
        applicationScope.launchOnMain {
            sessionTerminator.resetLocalData()
            // Not awaited: the navigation below runs right away. Main.immediate starts the sync
            // before that, and it then runs in the app-wide scope (plan R2).
            launch {
                dataSyncCoordinator.dataSync(reason = "appDataDeleted") //Trigger datasync that user does not get stuck in the email verify screen
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
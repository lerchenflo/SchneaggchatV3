package org.lerchenflo.schneaggchatv3mp.settings.presentation.miscSettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.logging.LogEntry
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils

class MiscSettingsViewModel(
    private val appRepository: AppRepository,
    private val navigator: Navigator,
    private val loggingRepository: LoggingRepository,
    private val shareUtils: ShareUtils
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
            appRepository.deleteAllAppData()
            navigator.navigate(Route.AutoLoginCredChecker, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
        }
    }

    fun onSendBugReportEmail(emailContent: String) {
        shareUtils.openMailClient(
            recipient = "schneaggchat@gmail.com",
            subject = "Bug Report / Feature Request",
            body = emailContent
        )
    }

}
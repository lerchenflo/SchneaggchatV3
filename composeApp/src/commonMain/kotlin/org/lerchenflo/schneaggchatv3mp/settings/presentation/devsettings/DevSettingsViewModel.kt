package org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.logging.LogEntry
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import kotlin.collections.emptyList
import kotlin.math.log

class DevSettingsViewModel(
    private val loggingRepository: LoggingRepository,
    private val navigator: Navigator
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


    fun navigateGames() {
        viewModelScope.launch {
            navigator.navigate(Route.Games)
        }
    }

}
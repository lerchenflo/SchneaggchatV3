package org.lerchenflo.schneaggchatv3mp.settings.presentation.miscSettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import org.lerchenflo.schneaggchatv3mp.app.logging.LogEntry
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString

@OptIn(FormatStringsInDatetimeFormats::class)
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

    fun formatAllLogs(logs: List<LogEntry>): String {
        if (logs.isEmpty()) return "No logs available"
        
        val timestamp =
            kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            .format(LocalDateTime.Format { byUnicodePattern("dd-MM-yyyy HH:mm:ss") })
        
        val header = """
            ========================================
            Application Logs Export
            Generated: $timestamp
            Total Logs: ${logs.size}
            ========================================
            
        """.trimIndent()
        
        val logEntries = logs.joinToString("\n\n") { log ->
            """
            [${log.type}] ${log.id}
            Message: ${log.message}
            
            timestamp: ${millisToString(log.timeStamp, format = "dd.MM HH:mm:ss.SSS")}
            ---
            """.trimIndent()
        }
        
        val footer = """
            
            ========================================
            End of Logs
            ========================================
        """.trimIndent()
        
        return header + logEntries + footer
    }

}
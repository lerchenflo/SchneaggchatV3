package org.lerchenflo.schneaggchatv3mp.settings.presentation.appearancesettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.ThemeSetting
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService

class AppearanceSettingsViewModel(
    private val preferenceManager: Preferencemanager,
    private val loggingRepository: LoggingRepository,
    private val languageService: LanguageService,
    private val appRepository: AppRepository,
): ViewModel() {

    var markdownEnabeled by mutableStateOf(false)
        private set

    var highlightTodaysMessageTimestamp by mutableStateOf(true)
        private set

    var selectedTheme by mutableStateOf(ThemeSetting.SYSTEM)
        private set

    var selectedLanguage by mutableStateOf(LanguageSetting.SYSTEM)
        private set

    init {
        viewModelScope.launch { // Markdown
            preferenceManager.getUseMdFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting MD preference: ${exception.message}")
                }
                .collect { value ->
                    markdownEnabeled = value
                }

        }
        viewModelScope.launch { // Highlight Today's Message Timestamp
            preferenceManager.getHighlightTodaysMessageTimestampFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting Highlight Today's Message Timestamp preference: ${exception.message}")
                }
                .collect { value ->
                    highlightTodaysMessageTimestamp = value
                }
        }
        viewModelScope.launch { // Theme
            preferenceManager.getThemeFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting Theme setting: ${exception.message}")
                }
                .collectLatest { value ->
                    selectedTheme = value
                }
        }
        viewModelScope.launch { // Language
            languageService.getCurrentLanguageFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting Language setting: ${exception.message}")
                }
                .collectLatest { value ->
                    selectedLanguage = value
                }
        }
    }



    fun updateMarkdownSwitch(newValue: Boolean){
        viewModelScope.launch {
            appRepository.setUseMd(newValue)
        }
    }

    fun updateHighlightTodaysMessageTimestamp(newValue: Boolean){
        viewModelScope.launch {
            appRepository.setHighlightTodaysMessageTimestamp(newValue)
        }
    }


    fun saveThemeSetting(theme: ThemeSetting){
        viewModelScope.launch {
            appRepository.setThemeSetting(theme)
        }
    }


    fun saveLanguageSetting(language: LanguageSetting){
        viewModelScope.launch {
            appRepository.setLanguageSetting(language)
        }
        println("System language: ${languageService.getSystemLanguage()}")
    }

}
package org.lerchenflo.schneaggchatv3mp.settings.presentation.appearancesettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.utilities.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.preferences.ThemeSetting
import org.lerchenflo.schneaggchatv3mp.utilities.preferences.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService

class AppearanceSettingsViewModel(
    private val preferenceManager: Preferencemanager,
    private val loggingRepository: LoggingRepository,
    private val languageService: LanguageService
): ViewModel() {

    var markdownEnabeled by mutableStateOf(false)
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
        CoroutineScope(Dispatchers.IO).launch {
            preferenceManager.saveUseMd(newValue)
        }
    }


    fun saveThemeSetting(theme: ThemeSetting){
        CoroutineScope(Dispatchers.IO).launch {
            preferenceManager.saveThemeSetting(theme = theme)
        }
    }


    fun saveLanguageSetting(language: LanguageSetting){
        CoroutineScope(Dispatchers.IO).launch {
            languageService.applyLanguage(language = language)
        }
        println("System language: ${languageService.getSystemLanguage()}")
    }

}
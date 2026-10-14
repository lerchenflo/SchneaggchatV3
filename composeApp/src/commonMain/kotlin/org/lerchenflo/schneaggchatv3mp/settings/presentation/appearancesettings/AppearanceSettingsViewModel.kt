package org.lerchenflo.schneaggchatv3mp.settings.presentation.appearancesettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.DEFAULT_QUICK_REACTIONS
import org.lerchenflo.schneaggchatv3mp.chat.domain.MAX_REACTION_LENGTH
import org.lerchenflo.schneaggchatv3mp.settings.domain.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.settings.domain.ThemeSetting
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService

class AppearanceSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val loggingRepository: Logger,
    private val languageService: LanguageService,
): ViewModel() {

    var markdownEnabeled by mutableStateOf(false)
        private set

    var highlightTodaysMessageTimestamp by mutableStateOf(true)
        private set

    var selectedTheme by mutableStateOf(ThemeSetting.SYSTEM)
        private set

    var selectedLanguage by mutableStateOf(LanguageSetting.SYSTEM)
        private set

    /** The user's quick reactions, or DEFAULT_QUICK_REACTIONS while the list was never customised. */
    var quickReactions by mutableStateOf(DEFAULT_QUICK_REACTIONS)
        private set

    init {
        viewModelScope.launch { // Markdown
            settingsRepository.getUseMdFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting MD preference: ${exception.message}")
                }
                .collect { value ->
                    markdownEnabeled = value
                }

        }
        viewModelScope.launch { // Highlight Today's Message Timestamp
            settingsRepository.getHighlightTodaysMessageTimestampFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting Highlight Today's Message Timestamp preference: ${exception.message}")
                }
                .collect { value ->
                    highlightTodaysMessageTimestamp = value
                }
        }
        viewModelScope.launch { // Theme
            settingsRepository.getThemeFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting Theme setting: ${exception.message}")
                }
                .collectLatest { value ->
                    selectedTheme = value
                }
        }
        viewModelScope.launch { // Quick reactions
            settingsRepository.getQuickReactionsFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting quick reactions: ${exception.message}")
                }
                .collect { value ->
                    quickReactions = value
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
            settingsRepository.setUseMd(newValue)
        }
    }

    fun updateHighlightTodaysMessageTimestamp(newValue: Boolean){
        viewModelScope.launch {
            settingsRepository.setHighlightTodaysMessageTimestamp(newValue)
        }
    }


    /**
     * Stores the edited list. Blanks, over-long entries and duplicates are dropped here as a last
     * guard - the server rejects all three, and the request is fire-and-forget.
     */
    fun saveQuickReactions(reactions: List<String>){
        val sanitized = reactions
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length <= MAX_REACTION_LENGTH }
            .distinct()

        viewModelScope.launch {
            settingsRepository.setQuickReactions(sanitized)
        }
    }

    /** Back to DEFAULT_QUICK_REACTIONS, and keeps following that set if it changes in a later version. */
    fun restoreDefaultQuickReactions(){
        viewModelScope.launch {
            settingsRepository.resetQuickReactions()
        }
    }


    fun saveThemeSetting(theme: ThemeSetting){
        viewModelScope.launch {
            settingsRepository.setThemeSetting(theme)
        }
    }


    fun saveLanguageSetting(language: LanguageSetting){
        viewModelScope.launch {
            settingsRepository.setLanguageSetting(language)
        }
        println("System language: ${languageService.getSystemLanguage()}")
    }

}
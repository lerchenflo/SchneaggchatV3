package org.lerchenflo.schneaggchatv3mp.settings.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.datasource.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.User
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.datasource.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.UserRepository

class SharedSettingsViewmodel(
    private val appVersion: AppVersion,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val loggingRepository: Logger
) : ViewModel() {

    var serverUrl by mutableStateOf("")
        private set

    var devSettingsEnabled by mutableStateOf(false)
        private set

    var ownUser by mutableStateOf<User?>(null)
        private set

    var isAndroid by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch { // Developer Settings
            settingsRepository.getDevSettingsFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting Developer Settings preference: ${exception.message}")
                }
                .collect { value ->
                    devSettingsEnabled = value
                }
        }

        viewModelScope.launch { // Server URL
            settingsRepository.getServerUrlFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting Server URL preference: ${exception.message}")
                }
                .collect { value ->
                    serverUrl = value
                }
        }

        viewModelScope.launch { // Own user
            val ownId = SessionCache.requireLoggedIn()?.userId ?: return@launch

            userRepository.getUserByIdFlow(ownId).collect { value ->
                ownUser = value
            }
        }

        isAndroid = appVersion.isAndroid()
    }

    // Developer Settings

    fun updateDevSettings(newValue: Boolean){
        viewModelScope.launch {
            settingsRepository.setDevSettings(newValue)
            SessionCache.updateDeveloper(newValue)
        }
    }


    fun updateServerUrl(newValue: String) {
        serverUrl = newValue
        viewModelScope.launch {
            settingsRepository.saveServerUrl(newValue)
        }
    }


}
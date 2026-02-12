package org.lerchenflo.schneaggchatv3mp.settings.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.preferences.Preferencemanager

class SharedSettingsViewmodel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager,
    private val loggingRepository: LoggingRepository
) : ViewModel() {

    var serverUrl by mutableStateOf("")
        private set

    var devSettingsEnabled by mutableStateOf(false)
        private set

    var ownUser by mutableStateOf<User?>(null)
        private set

    init {
        viewModelScope.launch { // Developer Settings
            preferenceManager.getDevSettingsFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting Developer Settings preference: ${exception.message}")
                }
                .collect { value ->
                    devSettingsEnabled = value
                }
        }

        viewModelScope.launch { // Server URL
            preferenceManager.getServerUrlFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting Server URL preference: ${exception.message}")
                }
                .collect { value ->
                    serverUrl = value
                }
        }

        viewModelScope.launch { // Own user
            appRepository.getOwnUserFlow().collect { value ->
                ownUser = value
            }
        }
    }

    // Developer Settings

    fun updateDevSettings(newValue: Boolean){
        CoroutineScope(Dispatchers.IO).launch {
            preferenceManager.saveDevSettings(newValue)
        }
    }


    fun updateServerUrl(newValue: String) {
        serverUrl = newValue
        viewModelScope.launch {
            preferenceManager.saveServerUrl(newValue)
        }
    }


}
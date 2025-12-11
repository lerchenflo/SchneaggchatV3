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
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager

class SharedSettingsViewmodel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager,
) : ViewModel() {

    init {
        viewModelScope.launch { // Developer Settings
            preferenceManager.getDevSettingsFlow()
                .catch { exception ->
                    println("Problem getting Developer Settings preference: ${exception.printStackTrace()}")
                }
                .collect { value ->
                    devSettingsEnabled = value
                }
        }

        viewModelScope.launch { // Server URL
            preferenceManager.getServerUrlFlow()
                .catch { exception ->
                    println("Problem getting Server URL preference: ${exception.printStackTrace()}")
                }
                .collect { value ->
                    serverUrl = value
                }
        }

        viewModelScope.launch { // Server URL
            appRepository.getOwnUserFlow().collect { value ->
                ownUser = value
            }
        }
    }

    // Developer Settings
    var devSettingsEnabled by mutableStateOf(false)
        private set

    fun updateDevSettings(newValue: Boolean){
        CoroutineScope(Dispatchers.IO).launch {
            preferenceManager.saveDevSettings(newValue)
        }
    }



    var serverUrl by mutableStateOf("")
        private set

    fun updateServerUrl(newValue: String) {
        serverUrl = newValue
        viewModelScope.launch {
            preferenceManager.saveServerUrl(newValue)
        }
    }


    var ownUser by mutableStateOf<User?>(null)
        private set

}
package org.lerchenflo.schneaggchatv3mp.settings.presentation.settings_dev

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.ThemeSetting
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.log_out_successfully

class DevSettingsViewModel(
    private val preferenceManager: Preferencemanager,
    private val navigator: Navigator
): ViewModel() {


    init {


        viewModelScope.launch { // Server URL
            preferenceManager.getServerUrlFlow()
                .catch { exception ->
                    println("Problem getting Server URL preference: ${exception.printStackTrace()}")
                }
                .collect { value ->
                    serverUrl = value
                }
        }
        viewModelScope.launch { // Developer Settings
            preferenceManager.getDevSettingsFlow()
                .catch { exception ->
                    println("Problem getting Developer Settings preference: ${exception.printStackTrace()}")
                }
                .collect { value ->
                    devSettingsEnabeled = value
                }

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


    // Developer Settings
    var devSettingsEnabeled by mutableStateOf(false)
        private set

    fun updateDevSettings(newValue: Boolean){
        CoroutineScope(Dispatchers.IO).launch {
            preferenceManager.saveDevSettings(newValue)
        }
    }



    fun onBackClick(){
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

}
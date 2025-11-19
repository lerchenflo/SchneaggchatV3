package org.lerchenflo.schneaggchatv3mp.settings.presentation

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
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.UserDto
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.ThemeSetting
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.log_out_successfully

class SettingsViewModel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager
): ViewModel() {



    fun init(){
        viewModelScope.launch { // Markdown
            preferenceManager.getUseMdFlow()
                .catch { exception ->
                    println("Problem getting MD preference: ${exception.printStackTrace()}")
                }
                .collect { value ->
                    markdownEnabeled = value
                }

        }
        viewModelScope.launch { // Theme
            preferenceManager.getThemeFlow()
                .catch { exception ->
                    println("Problem getting Theme setting: ${exception.printStackTrace()}")
                }
                .collect { value ->
                    selectedTheme = value
                }
        }
        viewModelScope.launch { // Server URL
            preferenceManager.getServerUrlFlow()
                .catch { exception ->
                    println("Problem getting Server URL preference: ${exception.printStackTrace()}")
                }
                .collect { value ->
                    serverURL = value
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


    fun getOwnuser() : Flow<UserDto?>{
        return appRepository.getownUser()
    }


    var markdownEnabeled by mutableStateOf(false)
        private set

    fun updateMarkdownSwitch(newValue: Boolean){
        CoroutineScope(Dispatchers.IO).launch {
            preferenceManager.saveUseMd(newValue)
        }
    }

    fun changeUsername(){
        // todo
    }

    fun logout(){
        viewModelScope.launch {
            appRepository.deleteAllAppData() // delete all app data when logging out
            preferenceManager.saveTokens(tokenPair = NetworkUtils.TokenPair(accessToken = "", refreshToken = "")) // override credentials with empty string
            SessionCache.clear() //Alle variabla löscja
            SnackbarManager.showMessage(getString(Res.string.log_out_successfully))
            // todo navigate to login screen
            // Des passiert jo scho über da callback. Willsch es umbaua oder wia? -Fabi
        }
    }



    fun deleteAllAppData(){
        viewModelScope.launch {
            appRepository.deleteAllAppData()
            SnackbarManager.showMessage("App data deleted ✅")
        }
    }

    var selectedTheme by mutableStateOf(ThemeSetting.SYSTEM)
        private set

    fun saveThemeSetting(theme: ThemeSetting){
        CoroutineScope(Dispatchers.IO).launch {
            preferenceManager.saveThemeSetting(theme = theme)
        }
    }

    var serverURL by mutableStateOf("")
        private set

    fun updateServerUrl(newValue: String){ //updates only the variable in the viewmodel
        serverURL = newValue
    }

    fun saveServerUrl(){
        CoroutineScope(Dispatchers.IO).launch { // updates the preferences
            preferenceManager.saveServerUrl(serverURL)
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

}
package org.lerchenflo.schneaggchatv3mp.settings.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.log_out_successfully

class SettingsViewModel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager
): ViewModel() {

    fun init(){
        viewModelScope.launch {
            preferenceManager.getUseMdFlow()
                .catch { exception ->
                    println("Problem getting MD preference: ${exception.printStackTrace()}")
                }
                .collect { value ->
                markdownEnabeled = value
                }

        }
        CoroutineScope(Dispatchers.IO).launch {
            _ownUser.value = appRepository.getownUser()
        }
    }


    private val _ownUser = MutableStateFlow<User?>(null)
    val ownUser: StateFlow<User?> = _ownUser

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
            preferenceManager.saveAutologinCreds("", "") // override credentials with empty string
            SnackbarManager.showMessage(getString(Res.string.log_out_successfully))
            // todo navigate to login screen
        }
    }



    fun deleteAllAppData(){
        viewModelScope.launch {
            appRepository.deleteAllAppData()
            SnackbarManager.showMessage("App data deleted ✅")
        }
    }

}
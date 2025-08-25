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
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager

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
        SnackbarManager.showMessage("Not impemented")
        // todo
    }



    fun deleteAllAppData(){
        viewModelScope.launch {
            appRepository.deleteAllAppData()
            SnackbarManager.showMessage("App data deleted ✅")
        }
    }

}
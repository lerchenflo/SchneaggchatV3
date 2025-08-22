package org.lerchenflo.schneaggchatv3mp.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager

class SettingsViewModel(
    private val appRepository: AppRepository
): ViewModel() {

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
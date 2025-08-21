package org.lerchenflo.schneaggchatv3mp.settings.Presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import kotlin.reflect.KClass

class SettingsViewModel: ViewModel() {
    //Custom Factory für desktop fix
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                return SettingsViewModel() as T
            }
        }
    }

    fun changeUsername(){
        // todo
    }

    fun logout(){
        SnackbarManager.showMessage("Not impemented")
        // todo
    }
}
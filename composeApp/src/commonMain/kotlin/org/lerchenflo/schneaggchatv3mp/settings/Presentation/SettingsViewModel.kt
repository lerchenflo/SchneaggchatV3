package org.lerchenflo.schneaggchatv3mp.settings.Presentation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import org.koin.compose.koinInject
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


    /* TODO flo
    fun deleteAllAppData(deleteAppDataUseCase: DeleteAppDataUseCase){
        viewModelScope.launch {
            deleteAppDataUseCase()
            SnackbarManager.showMessage("App data deleted ✅")
        }
    }

     */
}
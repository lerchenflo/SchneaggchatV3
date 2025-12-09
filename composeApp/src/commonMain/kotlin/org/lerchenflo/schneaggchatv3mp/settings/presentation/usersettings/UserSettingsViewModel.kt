package org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.domain.models.GalleryPhotoResult
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.log_out_successfully

class UserSettingsViewModel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager,
    private val navigator: Navigator,
): ViewModel() {


    fun updateUsername(newValue: String) {
        //Todo: networktask
    }

    fun changeProfilePicture(newImage: GalleryPhotoResult){
        val bytearray = newImage
            .loadBytes()

        //TODO: Update profile pic local and server
    }

    fun sendEmailVerify(){
        viewModelScope.launch {
            appRepository.sendEmailVerify()
        }

        //TODO: Stringressource
        SnackbarManager.showMessage("Verification email sent")
    }

    fun changeEmail(newEmail: String){
        //TODO: Change email
    }


    fun logout(){
        viewModelScope.launch {
            appRepository.logout()
            viewModelScope.launch {
                navigator.navigate(Route.Login, exitAllPreviousScreens = true)
            }
        }
    }

}
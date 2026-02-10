@file:OptIn(ExperimentalTime::class)

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
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_username_must_not_be_empty
import schneaggchatv3mp.composeapp.generated.resources.please_restart_app
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class UserSettingsViewModel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager,
    private val pictureManager: PictureManager,
    private val navigator: Navigator,
): ViewModel() {

    var lastEmailVerificationTime: Instant = Instant.DISTANT_PAST



    fun changeProfilePicture(newImage: GalleryPhotoResult){

        val bytearrayfullsize = newImage
            .loadBytes()

        viewModelScope.launch {

            val bytearray = pictureManager.downscaleImage(bytearrayfullsize)

            val success = appRepository.changeProfilePic(bytearray)

            if (success) {
                SnackbarManager.showMessage(getString(Res.string.please_restart_app)) //Des image caching vo coil isch so guat dassas sich ned abtöta loht. ma künnt profilbild mit datum speichra aber denn wirds ned überschrieba und unendlich speicherverbrauch
            }
        }
    }

    fun sendEmailVerify(){
        if (lastEmailVerificationTime.plus(2.minutes) > Clock.System.now()) {
            println("Email sending is throttled")
            return //If last email was sent in the last 2 mins
        }

        lastEmailVerificationTime = Clock.System.now()
        viewModelScope.launch {
            appRepository.sendEmailVerify()
        }

        //TODO: Stringressource
        SnackbarManager.showMessage("Verification email sent")
    }

    fun updateUsernameOnServer(newUsername: String){

        if(newUsername.isEmpty()){ // check if username is not empty
            viewModelScope.launch {
                SnackbarManager.showMessage(getString(Res.string.error_username_must_not_be_empty))
            }
            return
        }

        viewModelScope.launch {
            appRepository.changeUsername(newUsername)
        }
    }

    fun changeEmail(newEmail: String){
        viewModelScope.launch {
            appRepository.changeUserDetails(newEmail = newEmail, userId = SessionCache.getOwnIdValue() ?: "")
            appRepository.dataSync()
        }
    }

    fun changeStatus(newStatus: String) {
        viewModelScope.launch {
            appRepository.changeUserDetails(newStatus = newStatus, userId = SessionCache.getOwnIdValue() ?: "")
            appRepository.dataSync()
        }
    }

    fun changeBirthDate(newBirthDate: String) {
        viewModelScope.launch {
            appRepository.changeUserDetails(newBirthDate = newBirthDate, userId = SessionCache.getOwnIdValue() ?: "")
            appRepository.dataSync()
        }
    }


    fun logout(){
        viewModelScope.launch {
            appRepository.logout()
            viewModelScope.launch {
                navigator.navigate(Route.Login, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
            }
        }
    }

}
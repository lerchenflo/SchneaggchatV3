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
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.log_out_successfully
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class UserSettingsViewModel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager,
    private val navigator: Navigator,
): ViewModel() {

    var lastEmailVerificationTime: Instant = Instant.DISTANT_PAST

    fun updateUsername(newValue: String) {
        //Todo: networktask
    }

    fun changeProfilePicture(newImage: GalleryPhotoResult){
        val bytearray = newImage
            .loadBytes()

        //TODO: Update profile pic local and server
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
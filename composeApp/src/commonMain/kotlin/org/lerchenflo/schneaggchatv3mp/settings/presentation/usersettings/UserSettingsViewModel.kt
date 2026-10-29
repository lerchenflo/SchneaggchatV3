@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ismoy.imagepickerkmp.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.picker.GalleryPhotoResult
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.login.domain.AuthRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.SessionTerminator
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_username_must_not_be_empty
import schneaggchatv3mp.composeapp.generated.resources.please_restart_app
import schneaggchatv3mp.composeapp.generated.resources.verification_email_sent
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.launchOnMain

class UserSettingsViewModel(
    private val dataSyncCoordinator: DataSyncCoordinator,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val sessionTerminator: SessionTerminator,
    private val pictureManager: PictureManager,
    private val navigator: Navigator,
    private val loggingRepository: Logger,
    private val permissionManager: PermissionManager,
    private val applicationScope: ApplicationScope,
): ViewModel() {

    var lastEmailVerificationTime: Instant = Instant.DISTANT_PAST


    fun changeProfilePicture(newImage: GalleryPhotoResult){

        val bytearrayfullsize = newImage
            .loadBytes()

        applicationScope.launchOnMain {

            val bytearray = pictureManager.downscaleImage(bytearrayfullsize)

            val success = userRepository.changeProfilePic(bytearray)

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
        applicationScope.launchOnMain {
            authRepository.sendEmailVerify()
        }

        viewModelScope.launch {
            SnackbarManager.showMessage(getString(Res.string.verification_email_sent))
        }
    }

    fun updateUsernameOnServer(newUsername: String){

        val userId = SessionCache.requireLoggedIn()?.userId ?: return


        if(newUsername.isEmpty()){ // check if username is not empty
            viewModelScope.launch {
                SnackbarManager.showMessage(getString(Res.string.error_username_must_not_be_empty))
            }
            return
        }

        applicationScope.launchOnMain {
            userRepository.changeUsername(newUsername)
        }
    }

    fun changeEmail(newEmail: String){
        val userId = SessionCache.requireLoggedIn()?.userId ?: return

        applicationScope.launchOnMain {
            userRepository.changeUserDetails(newEmail = newEmail, userId = userId)
            dataSyncCoordinator.dataSync(reason = "emailChanged")
        }
    }

    fun changeStatus(newStatus: String) {
        applicationScope.launchOnMain {
            val userId = SessionCache.requireLoggedIn()?.userId ?: return@launchOnMain

            userRepository.changeUserDetails(newStatus = newStatus, userId = userId)
            dataSyncCoordinator.dataSync(reason = "statusChanged")
        }
    }

    fun changeBirthDate(newBirthDate: String) {
        val userId = SessionCache.requireLoggedIn()?.userId ?: return

        applicationScope.launchOnMain {
            userRepository.changeUserDetails(newBirthDate = newBirthDate, userId = userId)
            dataSyncCoordinator.dataSync(reason = "birthDateChanged")
        }
    }

    fun changePhoneNumber(newPhoneNumber: String) {
        val userId = SessionCache.requireLoggedIn()?.userId ?: return

        applicationScope.launchOnMain {
            userRepository.changeUserDetails(newPhoneNumber = newPhoneNumber, userId = userId)
            dataSyncCoordinator.dataSync(reason = "phoneNumberChanged")
        }
    }


    fun logout(){
        applicationScope.launchOnMain {
            sessionTerminator.logout()
            navigator.navigate(Route.Login, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
        }
    }

}
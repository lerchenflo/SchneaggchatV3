@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.domain.models.GalleryPhotoResult
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
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

class UserSettingsViewModel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager,
    private val pictureManager: PictureManager,
    private val navigator: Navigator,
    private val loggingRepository: LoggingRepository,
    private val permissionManager: PermissionManager,
): ViewModel() {

    var lastEmailVerificationTime: Instant = Instant.DISTANT_PAST

    // Master wake switch. Stored as wakeupEnabled on our OWN user row - on a friend's row that
    // same column means "this friend may wake me" instead.
    var wakeEnabledGlobal by mutableStateOf(false)
        private set

    var friends by mutableStateOf<List<User>>(emptyList())
        private set

    init {
        viewModelScope.launch { // Master wake switch (own user)
            val ownId = SessionCache.requireLoggedIn()?.userId ?: return@launch

            appRepository.getUserByIdFlow(ownId)
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting own user for wake settings: ${exception.message}")
                }
                .collect { value ->
                    wakeEnabledGlobal = value?.wakeupEnabled ?: false
                }
        }

        viewModelScope.launch { // Per-friend "this friend may wake me" switches
            appRepository.getFriendsFlow("")
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting friends for wake settings: ${exception.message}")
                }
                .collect { value ->
                    friends = value
                }
        }
    }

    /**
     * Commit the wake settings dialog. Only changed values are sent - the dialog hands back every
     * friend regardless of whether the user touched them.
     */
    fun saveWakeSettings(newGlobal: Boolean, friendDrafts: List<WakePermissionDraft>) {
        val wasGlobal = wakeEnabledGlobal
        val currentByFriendId = friends.associate { it.id to it.wakeupEnabled }

        viewModelScope.launch {
            if (newGlobal != wasGlobal) {
                appRepository.setWakeGlobal(newGlobal)
            }

            friendDrafts.forEach { draft ->
                if (currentByFriendId[draft.friendId] != draft.allowWake) {
                    appRepository.setWakePermission(draft.friendId, draft.allowWake)
                }
            }

            // Turning the master switch on is the moment the full screen alarm actually becomes
            // relevant, so ask for the permission here rather than at some unrelated later point.
            if (newGlobal && !wasGlobal) {
                requestFullScreenIntentPermissionIfNeeded()
            }
        }
    }

    private suspend fun requestFullScreenIntentPermissionIfNeeded() {
        println("full screen permission: " + permissionManager.checkFullScreenIntentPermission())
        if (permissionManager.checkFullScreenIntentPermission() != PermissionState.GRANTED) {
            println("Requesting full screen intent permission")

            permissionManager.requestFullScreenIntentPermission()
        }
    }


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

        viewModelScope.launch {
            appRepository.changeUsername(newUsername)
        }
    }

    fun changeEmail(newEmail: String){
        val userId = SessionCache.requireLoggedIn()?.userId ?: return

        viewModelScope.launch {
            appRepository.changeUserDetails(newEmail = newEmail, userId = userId)
            appRepository.dataSync(reason = "emailChanged")
        }
    }

    fun changeStatus(newStatus: String) {
        viewModelScope.launch {
            val userId = SessionCache.requireLoggedIn()?.userId ?: return@launch

            appRepository.changeUserDetails(newStatus = newStatus, userId = userId)
            appRepository.dataSync(reason = "statusChanged")
        }
    }

    fun changeBirthDate(newBirthDate: String) {
        val userId = SessionCache.requireLoggedIn()?.userId ?: return

        viewModelScope.launch {
            appRepository.changeUserDetails(newBirthDate = newBirthDate, userId = userId)
            appRepository.dataSync(reason = "birthDateChanged")
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
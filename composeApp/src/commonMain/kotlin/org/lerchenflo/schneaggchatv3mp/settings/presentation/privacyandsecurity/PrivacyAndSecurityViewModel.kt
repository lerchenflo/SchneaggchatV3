@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.settings.presentation.privacyandsecurity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import org.lerchenflo.schneaggchatv3mp.settings.presentation.schneaggmapsettings.FriendShareDraft
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.password_change_failed
import schneaggchatv3mp.composeapp.generated.resources.password_changed_successfully
import schneaggchatv3mp.composeapp.generated.resources.verification_email_sent
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class PrivacyAndSecurityViewModel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager,
    private val navigator: Navigator,
    private val loggingRepository: LoggingRepository,
    private val permissionManager: PermissionManager,
) : ViewModel() {

    var lastEmailVerificationTime: Instant = Instant.DISTANT_PAST

    var shareLocationGlobal by mutableStateOf(false)
        private set

    var friends by mutableStateOf<List<User>>(emptyList())
        private set

    init {
        viewModelScope.launch { // Global location sharing status
            val ownId = SessionCache.requireLoggedIn()?.userId ?: return@launch

            appRepository.getUserByIdFlow(ownId)
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting own user for location sharing: ${exception.message}")
                }
                .collect { value ->
                    shareLocationGlobal = value?.locationShared ?: false
                }
        }

        viewModelScope.launch { // Friends for per-friend location sharing
            appRepository.getFriendsFlow("")
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting friends for privacy settings: ${exception.message}")
                }
                .collect { value ->
                    friends = value
                }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = appRepository.changePassword(oldPassword, newPassword)
            if (success) {
                SnackbarManager.showMessage(getString(Res.string.password_changed_successfully))
                onFinished(true)
            } else {
                SnackbarManager.showMessage(getString(Res.string.password_change_failed))
                onFinished(false)
            }
        }
    }

    fun sendEmailVerify() {
        if (lastEmailVerificationTime.plus(2.minutes) > Clock.System.now()) {
            return
        }

        lastEmailVerificationTime = Clock.System.now()
        viewModelScope.launch {
            appRepository.sendEmailVerify()
            SnackbarManager.showMessage(getString(Res.string.verification_email_sent))
        }
    }

    fun changeEmail(newEmail: String) {
        val userId = SessionCache.requireLoggedIn()?.userId ?: return

        viewModelScope.launch {
            appRepository.changeUserDetails(newEmail = newEmail, userId = userId)
            appRepository.dataSync(reason = "emailChanged")
        }
    }

    fun saveLocationSharing(newGlobalShare: Boolean, friendDrafts: List<FriendShareDraft>) {
        val wasGlobalShared = shareLocationGlobal

        viewModelScope.launch {
            when {
                newGlobalShare && !wasGlobalShared -> {
                    friendDrafts.forEach { draft ->
                        appRepository.setLocationSharing(draft.friendId, share = true, draft.shareSpeedHeading, draft.snailTrail)
                    }
                }
                !newGlobalShare && wasGlobalShared -> {
                    appRepository.disableLocationSharingForAllFriends()
                }
                else -> {
                    friendDrafts.forEach { draft ->
                        val friend = friends.find { it.id == draft.friendId } ?: return@forEach
                        val changed = friend.locationShared != draft.share ||
                                friend.shareSpeedHeading != draft.shareSpeedHeading ||
                                friend.snailTrail != draft.snailTrail
                        if (changed) {
                            appRepository.setLocationSharing(draft.friendId, draft.share, draft.shareSpeedHeading, draft.snailTrail)
                        }
                    }
                }
            }

            if (newGlobalShare) {
                permissionManager.requestLocationPermission()
            }
        }
    }

    suspend fun getServerUrl(): String {
        return preferenceManager.getServerUrl()
    }

    fun logout() {
        viewModelScope.launch {
            appRepository.logout()
            navigator.navigate(Route.Login, navigationOptions = Navigator.NavigationOptions(exitAllPreviousScreens = true))
        }
    }
}

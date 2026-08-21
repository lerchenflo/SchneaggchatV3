@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.settings.presentation.notificationsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings.WakePermissionDraft
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import kotlin.time.ExperimentalTime

class NotificationSettingsViewModel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager,
    private val navigator: Navigator,
    private val loggingRepository: LoggingRepository,
    private val permissionManager: PermissionManager,
) : ViewModel() {

    var notificationPermissionState by mutableStateOf(PermissionState.NOT_DETERMINED)
        private set

    var wakeEnabledGlobal by mutableStateOf(false)
        private set

    var friends by mutableStateOf<List<User>>(emptyList())
        private set

    init {
        checkNotificationPermission()

        viewModelScope.launch { // Global wake status
            val ownId = SessionCache.requireLoggedIn()?.userId ?: return@launch

            appRepository.getUserByIdFlow(ownId)
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting own user for wake settings: ${exception.message}")
                }
                .collect { value ->
                    wakeEnabledGlobal = value?.wakeupEnabled ?: false
                }
        }

        viewModelScope.launch { // Friends for per-friend wake sharing
            appRepository.getFriendsFlow("")
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting friends for notification settings: ${exception.message}")
                }
                .collect { value ->
                    friends = value
                }
        }
    }

    var showOpenSettingsDialog by mutableStateOf(false)
        private set

    fun checkNotificationPermission() {
        viewModelScope.launch {
            notificationPermissionState = permissionManager.checkNotificationPermission()
        }
    }

    fun requestNotificationPermission() {
        viewModelScope.launch {
            if (notificationPermissionState == PermissionState.GRANTED) {
                return@launch
            }

            val result = permissionManager.requestNotificationPermission(openSettings = false)
            notificationPermissionState = result
            if (result != PermissionState.GRANTED) {
                showOpenSettingsDialog = true
            }
        }
    }

    fun openAppSettings() {
        showOpenSettingsDialog = false
        viewModelScope.launch {
            permissionManager.requestNotificationPermission(openSettings = true)
        }
    }

    fun dismissOpenSettingsDialog() {
        showOpenSettingsDialog = false
    }

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

            if (newGlobal && !wasGlobal) {
                requestFullScreenIntentPermissionIfNeeded()
            }
        }
    }

    private suspend fun requestFullScreenIntentPermissionIfNeeded() {
        if (permissionManager.checkFullScreenIntentPermission() != PermissionState.GRANTED) {
            permissionManager.requestFullScreenIntentPermission()
        }
    }
}

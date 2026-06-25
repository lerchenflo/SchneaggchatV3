package org.lerchenflo.schneaggchatv3mp.settings.presentation.schneaggmapsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager

class SchneaggmapSettingsViewModel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager,
    private val loggingRepository: LoggingRepository
) : ViewModel() {

    var mergeMapLocations by mutableStateOf(true)
        private set

    var shareLocationGlobal by mutableStateOf(false)
        private set

    var advancedLocationSharing by mutableStateOf(false)
        private set

    var friends by mutableStateOf<List<User>>(emptyList())
        private set

    init {
        viewModelScope.launch { // Merge map locations when zooming
            preferenceManager.getMergeMapLocationsFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting merge map locations preference: ${exception.message}")
                }
                .collect { value ->
                    mergeMapLocations = value
                }
        }

        viewModelScope.launch { // Global "share my location at all" switch (own user)
            val ownId = SessionCache.requireLoggedIn()?.userId ?: return@launch

            appRepository.getUserByIdFlow(ownId)
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting own user for location sharing: ${exception.message}")
                }
                .collect { value ->
                    shareLocationGlobal = value?.locationShared ?: false
                }
        }

        viewModelScope.launch { // Per-friend "share my location with them" switches
            appRepository.getFriendsFlow("")
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting friends for location sharing: ${exception.message}")
                }
                .collect { value ->
                    friends = value
                }
        }

        viewModelScope.launch { // "Advanced location sharing" - sends our own telemetry + reveals per-friend advanced controls
            preferenceManager.getAdvancedLocationSharingFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting advanced location sharing preference: ${exception.message}")
                }
                .collect { value ->
                    advancedLocationSharing = value
                }
        }
    }

    fun updateMergeMapLocations(newValue: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            preferenceManager.saveMergeMapLocations(newValue)
        }
    }

    fun updateShareLocationGlobal(newValue: Boolean) {
        viewModelScope.launch {
            appRepository.setOwnLocationShared(newValue)
        }
    }

    fun updateFriendLocationSharing(friendId: String, share: Boolean) {
        viewModelScope.launch {
            val friend = friends.find { it.id == friendId }
            appRepository.setLocationSharing(
                friendId = friendId,
                share = share,
                shareSpeedHeading = friend?.shareSpeedHeading ?: false,
                snailTrailHours = friend?.snailTrailHours,
            )
        }
    }

    fun updateAdvancedLocationSharing(newValue: Boolean) {
        viewModelScope.launch {
            preferenceManager.saveAdvancedLocationSharing(newValue)
        }
    }

    fun updateFriendAdvancedSharing(friendId: String, shareSpeedHeading: Boolean, snailTrailHours: Int?) {
        viewModelScope.launch {
            val friend = friends.find { it.id == friendId }
            appRepository.setLocationSharing(
                friendId = friendId,
                share = friend?.locationShared ?: true,
                shareSpeedHeading = shareSpeedHeading,
                snailTrailHours = snailTrailHours,
            )
        }
    }
}

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
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager

/** Draft per-friend location sharing settings edited in the dialog, only sent to the server on Save. */
data class FriendShareDraft(
    val friendId: String,
    val share: Boolean,
    val shareSpeedHeading: Boolean,
    val snailTrail: Boolean,
)

class SchneaggmapSettingsViewModel(
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager,
    private val loggingRepository: LoggingRepository,
    private val permissionManager: PermissionManager,
) : ViewModel() {

    var mergeMapLocations by mutableStateOf(true)
        private set

    // Derived server-side from whether any friend has locationShared=true. Only used to seed
    // the dialog's local draft toggle - there is no standalone switch to write to.
    var shareLocationGlobal by mutableStateOf(false)
        private set

    var advancedLocationSharing by mutableStateOf(false)
        private set

    var mapStyle by mutableStateOf(MapStyleSetting.LIBERTY)
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

        viewModelScope.launch { // Global "share my location at all" switch (own user) - derived, read-only
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

        viewModelScope.launch { // Map style
            preferenceManager.getMapStyleSettingFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting map style preference: ${exception.message}")
                }
                .collect { value ->
                    mapStyle = value
                }
        }
    }

    fun updateMergeMapLocations(newValue: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            preferenceManager.saveMergeMapLocations(newValue)
        }
    }

    fun updateAdvancedLocationSharing(newValue: Boolean) {
        viewModelScope.launch {
            preferenceManager.saveAdvancedLocationSharing(newValue)
        }
    }

    fun saveMapStyleSetting(style: MapStyleSetting) {
        viewModelScope.launch {
            preferenceManager.saveMapStyleSetting(style)
        }
    }

    /**
     * Commits the location sharing dialog's draft to the server. If the master switch changed
     * from off to on, sharing is force-enabled for every friend (using each friend's drafted
     * advanced flags) and location permission is requested; if it changed from on to off,
     * sharing is force-disabled for every friend. Otherwise only friends whose draft actually
     * differs from their current state are updated.
     */
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
}

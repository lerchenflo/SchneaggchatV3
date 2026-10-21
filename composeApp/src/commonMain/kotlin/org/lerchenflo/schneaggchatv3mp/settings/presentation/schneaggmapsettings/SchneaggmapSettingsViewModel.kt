package org.lerchenflo.schneaggchatv3mp.settings.presentation.schneaggmapsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.settings.domain.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.launchOnMain

/** Draft per-friend location sharing settings edited in the dialog, only sent to the server on Save. */
data class FriendShareDraft(
    val friendId: String,
    val share: Boolean,
    val shareSpeedHeading: Boolean,
    val snailTrail: Boolean,
)

class SchneaggmapSettingsViewModel(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val loggingRepository: Logger,
    private val permissionManager: PermissionManager,
    private val applicationScope: ApplicationScope,
) : ViewModel() {

    var mergeMapLocations by mutableStateOf(true)
        private set

    var mergeMapUsers by mutableStateOf(true)
        private set

    var mapStyle by mutableStateOf(MapStyleSetting.LIBERTY)
        private set

    // Derived server-side from whether any friend has locationShared=true. Only used to seed
    // the dialog's local draft toggle - there is no standalone switch to write to.
    var shareLocationGlobal by mutableStateOf(false)
        private set

    var friends by mutableStateOf<List<User>>(emptyList())
        private set

    init {
        viewModelScope.launch { // Merge map locations when zooming
            settingsRepository.getMergeMapLocationsFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting merge map locations preference: ${exception.message}")
                }
                .collect { value ->
                    mergeMapLocations = value
                }
        }

        viewModelScope.launch { // Merge map users when zooming
            settingsRepository.getMergeMapUsersFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting merge map users preference: ${exception.message}")
                }
                .collect { value ->
                    mergeMapUsers = value
                }
        }

        viewModelScope.launch { // Base map style of the Schneaggmap
            settingsRepository.getMapStyleSettingFlow()
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting map style preference: ${exception.message}")
                }
                .collect { value ->
                    mapStyle = value
                }
        }

        viewModelScope.launch { // Global "share my location at all" switch (own user) - derived, read-only
            val ownId = SessionCache.requireLoggedIn()?.userId ?: return@launch

            userRepository.getUserByIdFlow(ownId)
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting own user for location sharing: ${exception.message}")
                }
                .collect { value ->
                    shareLocationGlobal = value?.locationShared ?: false
                }
        }

        viewModelScope.launch { // Per-friend "share my location with them" switches
            userRepository.getFriendsFlow("")
                .catch { exception ->
                    loggingRepository.logWarning("Problem getting friends for location sharing: ${exception.message}")
                }
                .collect { value ->
                    friends = value
                }
        }
    }

    fun updateMergeMapLocations(newValue: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMergeMapLocations(newValue)
        }
    }

    fun updateMergeMapUsers(newValue: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMergeMapUsers(newValue)
        }
    }

    fun updateMapStyle(newValue: MapStyleSetting) {
        viewModelScope.launch {
            settingsRepository.setMapStyleSetting(newValue)
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

        // The server writes outlive the screen; the permission prompt belongs to it and is
        // dropped when the screen is gone
        val writes = applicationScope.launchOnMain {
            when {
                newGlobalShare && !wasGlobalShared -> {
                    friendDrafts.forEach { draft ->
                        userRepository.setLocationSharing(draft.friendId, share = true, draft.shareSpeedHeading, draft.snailTrail)
                    }
                }
                !newGlobalShare && wasGlobalShared -> {
                    userRepository.disableLocationSharingForAllFriends()
                }
                else -> {
                    friendDrafts.forEach { draft ->
                        val friend = friends.find { it.id == draft.friendId } ?: return@forEach
                        val changed = friend.locationShared != draft.share ||
                                friend.shareSpeedHeading != draft.shareSpeedHeading ||
                                friend.snailTrail != draft.snailTrail
                        if (changed) {
                            userRepository.setLocationSharing(draft.friendId, draft.share, draft.shareSpeedHeading, draft.snailTrail)
                        }
                    }
                }
            }
        }

        if (newGlobalShare) {
            viewModelScope.launch {
                writes.join()
                permissionManager.requestLocationPermission()
            }
        }
    }
}

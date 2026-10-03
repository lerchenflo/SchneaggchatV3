package org.lerchenflo.schneaggchatv3mp.settings.presentation.loggedindevices

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.settings.data.DEVICETYPE
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.logged_in_devices_logged_out

/**
 * One logged-in device of the own account, ready for display.
 * [displayName] is null when the device never sent a name.
 */
data class LoggedInDevice(
    val id: String,
    val displayName: String?,
    val deviceType: DEVICETYPE?,
    val createdAt: Long,
    val lastUsedAt: Long,
    val isCurrentDevice: Boolean,
)

class LoggedInDevicesViewModel(
    private val appRepository: AppRepository,
    private val appVersion: AppVersion,
) : ViewModel() {

    var devices by mutableStateOf<List<LoggedInDevice>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var loadFailed by mutableStateOf(false)
        private set

    init {
        loadDevices()
    }

    fun loadDevices() {
        viewModelScope.launch {
            isLoading = true
            loadFailed = false

            val sessions = appRepository.getSessions()
            if (sessions == null) {
                loadFailed = true
            } else {
                val ownName = appVersion.getDeviceName()
                val ownType = appVersion.getDeviceType()

                devices = sessions
                    .map { it.toLoggedInDevice(isCurrentDevice = it.deviceName == ownName && it.deviceType == ownType) }
                    // Own device on top, the rest by last activity (server order)
                    .sortedByDescending { it.isCurrentDevice }
            }

            isLoading = false
        }
    }

    fun logoutDevice(device: LoggedInDevice) {
        if (device.isCurrentDevice) return // The own device logs out via the normal logout

        viewModelScope.launch {
            if (appRepository.endSession(device.id)) {
                devices = devices.filterNot { it.id == device.id }
                SnackbarManager.showMessage(getString(Res.string.logged_in_devices_logged_out))
            }
        }
    }
}

// Device names carry a device id suffix (see AppVersion.getDeviceName actuals), e.g.
// "Pixel 8 - androidId: 1a2b" or "iPhone 15 (iOS 17.5) - iPhone - vendorId: ABC" - only the
// human-readable part is shown.
private val deviceIdSuffix = Regex("""\s+-\s+(androidId|vendorId|installId|macAddress):.*$""")

private fun NetworkUtils.SessionResponse.toLoggedInDevice(isCurrentDevice: Boolean): LoggedInDevice {
    val withoutId = deviceName?.replace(deviceIdSuffix, "")?.trim()
    // iOS appends the generic UIDevice.name ("iPhone") after the marketing name - drop it
    val readableName = if (deviceType == DEVICETYPE.IOS) withoutId?.substringBefore(" - ") else withoutId

    return LoggedInDevice(
        id = id,
        displayName = readableName?.ifBlank { null },
        deviceType = deviceType,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        isCurrentDevice = isCurrentDevice,
    )
}

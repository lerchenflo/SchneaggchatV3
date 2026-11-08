package org.lerchenflo.schneaggchatv3mp.desktopupdate.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.desktopupdate.domain.AppRelease
import org.lerchenflo.schneaggchatv3mp.desktopupdate.domain.DesktopUpdateRepository
import org.lerchenflo.schneaggchatv3mp.desktopupdate.domain.findUpdateFor
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion

/** Checks GitHub once for a newer desktop release and holds the banner for it. */
class DesktopUpdateViewModel(
    private val desktopUpdateRepository: DesktopUpdateRepository,
    private val appVersion: AppVersion,
) : ViewModel() {

    /** A newer GitHub release, null if up to date. */
    private val _update = MutableStateFlow<AppRelease?>(null)
    val update: StateFlow<AppRelease?> = _update.asStateFlow()

    init {
        viewModelScope.launch {
            _update.value = findUpdate()
        }
    }

    fun onDismiss() {
        val update = _update.value
        _update.value = null
        if (update?.isBeta == true) {
            viewModelScope.launch {
                desktopUpdateRepository.saveDismissedBetaVersion(update.tagName)
            }
        }
    }

    private suspend fun findUpdate(): AppRelease? {
        val installedVersion = appVersion.getVersionName()
        println("app version: $installedVersion")

        val update = try {
            desktopUpdateRepository.getReleases().findUpdateFor(installedVersion)
        } catch (e: IllegalArgumentException) {
            // Handle the bad data safely without crashing the app
            println("Error checking version: ${e.message}")
            null
        }

        // A closed beta banner stays closed for that beta, only the next beta shows again
        val closedBeta = update?.isBeta == true && update.tagName == desktopUpdateRepository.getDismissedBetaVersion()
        return update.takeUnless { closedBeta }
    }
}

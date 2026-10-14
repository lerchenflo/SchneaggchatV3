package org.lerchenflo.schneaggchatv3mp.settings.presentation

import androidx.lifecycle.ViewModel
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion

class SettingsViewModel(
    appVersion: AppVersion,
): ViewModel() {

    // Shown in the version text at the bottom of the settings screen
    val versionName: String = appVersion.getVersionName()
    val versionCode: String = appVersion.getVersionCode()

}

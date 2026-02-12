package org.lerchenflo.schneaggchatv3mp.settings.data

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.utilities.preferences.Preferencemanager

class SettingsRepository(
    private val preferencemanager: Preferencemanager

) {
    fun getUsemd() : Flow<Boolean> {
        return preferencemanager.getUseMdFlow()
    }

}
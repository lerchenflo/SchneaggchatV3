package org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route

class DevSettingsViewModel(
    private val loggingRepository: LoggingRepository,
    private val navigator: Navigator
): ViewModel() {


    fun navigateGames() {
        viewModelScope.launch {
            navigator.navigate(Route.Games)
        }
    }

}
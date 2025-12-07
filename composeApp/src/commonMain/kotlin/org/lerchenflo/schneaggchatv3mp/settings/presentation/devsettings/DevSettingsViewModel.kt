package org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator

class DevSettingsViewModel(
    private val navigator: Navigator,
): ViewModel() {

    fun onBackClick(){
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

}
package org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator

class UserSettingsViewModel(
    private val navigator: Navigator,
): ViewModel() {

    fun onBackClick(){
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

    fun updateUsername(newValue: String) {
        //Todo: networktask
    }

}
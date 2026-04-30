package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator

class SchneaggmapViewModel(
    private val navigator: Navigator
) : ViewModel() {

    fun onBackClick() {
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }
}

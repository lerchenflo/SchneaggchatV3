package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository

class ChatDetailsViewmodel(
    private val appRepository : AppRepository,
    private val navigator: Navigator
) : ViewModel() {

    fun onBackClick(){
        viewModelScope.launch {
            navigator.navigateBack()
        }
    }

}
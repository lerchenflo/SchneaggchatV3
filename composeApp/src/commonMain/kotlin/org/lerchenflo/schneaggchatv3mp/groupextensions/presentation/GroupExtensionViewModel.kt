package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionRepository

/** Backs the screen of one extension: loads the container, the type specific screens hang below it. */
class GroupExtensionViewModel(
    private val groupExtensionRepository: GroupExtensionRepository,
    private val navigator: Navigator,

    extensionId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupExtensionState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            groupExtensionRepository.getExtensionFlow(extensionId).collect { extension ->
                _state.update { it.copy(extension = extension, isLoading = false) }
            }
        }
    }

    fun onAction(action: GroupExtensionAction) {
        when (action) {
            GroupExtensionAction.OnBackClick -> viewModelScope.launch {
                navigator.navigateBack()
            }
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.MapRepository


class SchneaggmapViewModel(
    private val navigator: Navigator,
    private val mapRepository: MapRepository,
    private val appRepository: AppRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SchneaggmapState())

    val state = combine(
        _state,
        mapRepository.getAllMapEntriesFlow(),
    ) { state, entries ->

        state.copy(
            entries = entries,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SchneaggmapState(),
    )


    fun onAction(action: SchneaggmapAction) {
        when (action) {
            SchneaggmapAction.OnBackClicked -> viewModelScope.launch { navigator.navigateBack() }
            SchneaggmapAction.ToggleFilterDropdown -> _state.update {
                it.copy(isFilterDropdownVisible = !it.isFilterDropdownVisible, selectedEntry = null)
            }
            is SchneaggmapAction.ToggleMainType -> {
                val enabled = state.value.enabledTypes
                val updated = if (action.key in enabled) enabled - action.key else enabled + action.key

                _state.update { it.copy(enabledTypes = updated) }
            }
            is SchneaggmapAction.SelectEntry -> _state.update {
                it.copy(selectedEntry = action.entry, isFilterDropdownVisible = false)
            }

            is SchneaggmapAction.OnMapClick -> {
                println("Onclick. Longclick: ${action.longClick}")

                //Dismiss filter dropdown on map click
                if (_state.value.isFilterDropdownVisible) {
                    _state.update {
                        it.copy(isFilterDropdownVisible = false)
                    }
                }

                //Dismiss
                if (_state.value.selectedEntry != null) {
                    _state.update {
                        it.copy(selectedEntry = null)
                    }
                }
            }

            is SchneaggmapAction.OnEntryClick -> {
                println("On entry clicked: ${action.entryId}")

                val selectedEntry = state.value.entries.firstOrNull { it.id == action.entryId}

                if (selectedEntry == null) {
                    println("selected entry is null")

                }

                _state.update { currentState ->
                    currentState.copy(
                        selectedEntry = selectedEntry,
                        isFilterDropdownVisible = false
                    )
                }
            }

            SchneaggmapAction.OnEntryPopupDismiss -> {
                _state.update {
                    it.copy(
                        selectedEntry = null
                    )
                }
            }

            is SchneaggmapAction.OnEntryPopupSave -> {
                viewModelScope.launch {
                    //appRepository.ed TODO Update/upsert on server
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            appRepository.dataSync()

        }
    }


}

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
        mapRepository.getAllSubtypesFlow(),
        mapRepository.getAllMainTypesFlow(),
    ) { s, entries, subtypes, mainTypes ->

        println("MAP: Loaded ${entries.size} map entrys")

        val subtypesByMainType = subtypes.groupBy { it.mainTypeKey }
        val enabledMainTypes = if (s.enabledMainTypes.isEmpty() && mainTypes.isNotEmpty()) {
            mainTypes.map { it.key }.toSet()
        } else {
            s.enabledMainTypes
        }
        s.copy(
            entries = entries,
            subtypesByMainType = subtypesByMainType,
            mainTypes = mainTypes,
            enabledMainTypes = enabledMainTypes,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SchneaggmapState(),
    )

    init {
        refresh()
    }

    fun onAction(action: SchneaggmapAction) {
        when (action) {
            SchneaggmapAction.OnBackClicked -> viewModelScope.launch { navigator.navigateBack() }
            SchneaggmapAction.ToggleFilterDropdown -> _state.update {
                it.copy(isFilterDropdownVisible = !it.isFilterDropdownVisible, selectedEntry = null)
            }
            SchneaggmapAction.Refresh -> refresh()
            is SchneaggmapAction.ToggleMainType -> {
                val enabled = _state.value.enabledMainTypes
                val updated = if (action.key in enabled) enabled - action.key else enabled + action.key
                _state.update { it.copy(enabledMainTypes = updated) }
            }
            is SchneaggmapAction.SelectEntry -> _state.update {
                it.copy(selectedEntry = action.entry, isFilterDropdownVisible = false)
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                appRepository.mapSync()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}

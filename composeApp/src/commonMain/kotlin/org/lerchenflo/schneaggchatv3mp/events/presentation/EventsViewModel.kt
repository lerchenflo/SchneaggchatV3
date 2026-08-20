package org.lerchenflo.schneaggchatv3mp.events.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator

class EventsViewModel(
    private val navigator: Navigator
) : ViewModel() {

    private val _state = MutableStateFlow(EventsState())
    val state = _state.asStateFlow()

    fun onAction(action: EventsAction) {
        when (action) {
            is EventsAction.OnEventClick -> {
                viewModelScope.launch {
                    _state.update { currentstate ->
                        currentstate.copy(
                            selectedEvent = currentstate.events.first { it.id == action.eventId }
                        )
                    }
                }
            }
        }
    }
}

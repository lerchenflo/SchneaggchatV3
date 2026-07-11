package org.lerchenflo.schneaggchatv3mp.games.presentation.fingerpicker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MIN_FINGERS = 2
private const val HOLD_DURATION_MILLIS = 2500L
private const val HOLD_STEPS = 30

enum class FingerPickerPhase { WAITING, HOLDING, RESULT }

data class FingerPickerState(
    val phase: FingerPickerPhase = FingerPickerPhase.WAITING,
    val touches: Map<PointerId, Offset> = emptyMap(),
    val winnerCount: Int = 1,
    val holdProgress: Float = 0f,
    val winners: Set<PointerId> = emptySet(),
)

sealed interface FingerPickerAction {
    data class OnTouchesChanged(val touches: Map<PointerId, Offset>) : FingerPickerAction
    data class OnWinnerCountChange(val count: Int) : FingerPickerAction
}

class FingerPickerViewModel : ViewModel() {

    private val _state = MutableStateFlow(FingerPickerState())
    val state: StateFlow<FingerPickerState> = _state.asStateFlow()

    private var holdJob: Job? = null

    fun onAction(action: FingerPickerAction) {
        when (action) {
            is FingerPickerAction.OnTouchesChanged -> onTouchesChanged(action.touches)
            is FingerPickerAction.OnWinnerCountChange -> _state.update {
                it.copy(winnerCount = action.count.coerceAtLeast(1))
            }
        }
    }

    private fun onTouchesChanged(touches: Map<PointerId, Offset>) {
        val current = _state.value

        if (current.phase == FingerPickerPhase.RESULT) {
            // Wait for every finger to lift before a new round can start
            if (touches.isEmpty()) {
                _state.value = FingerPickerState(winnerCount = current.winnerCount)
            }
            return
        }

        if (touches.size < MIN_FINGERS) {
            holdJob?.cancel()
            _state.update {
                it.copy(phase = FingerPickerPhase.WAITING, touches = touches, holdProgress = 0f)
            }
            return
        }

        // Any change in which fingers are pressed restarts the countdown - only holding the
        // same set of fingers steady for the full duration triggers a pick.
        if (touches.keys != current.touches.keys || current.phase == FingerPickerPhase.WAITING) {
            startHold(touches)
        } else {
            _state.update { it.copy(touches = touches) }
        }
    }

    private fun startHold(touches: Map<PointerId, Offset>) {
        holdJob?.cancel()
        _state.update {
            it.copy(phase = FingerPickerPhase.HOLDING, touches = touches, holdProgress = 0f)
        }
        holdJob = viewModelScope.launch {
            repeat(HOLD_STEPS) { step ->
                delay(HOLD_DURATION_MILLIS / HOLD_STEPS)
                _state.update { it.copy(holdProgress = (step + 1) / HOLD_STEPS.toFloat()) }
            }
            pickWinners()
        }
    }

    private fun pickWinners() {
        val current = _state.value
        val winnerCount = current.winnerCount.coerceIn(1, current.touches.size - 1)
        val winners = current.touches.keys.shuffled().take(winnerCount).toSet()
        _state.update { it.copy(phase = FingerPickerPhase.RESULT, winners = winners, holdProgress = 1f) }
    }
}

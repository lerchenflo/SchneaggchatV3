package org.lerchenflo.schneaggchatv3mp.games.presentation.morse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MorseState(
    val currentCode: String = "",
    val currentChar: Char? = null,
    val invalid: Boolean = false,
    val history: List<Char> = emptyList()
)

private const val HISTORY_LIMIT = 10
private const val AUTO_COMMIT_DELAY_MS = 1200L
private const val INVALID_CLEAR_DELAY_MS = 800L
private const val MAX_CODE_DEPTH = 5

class MorseViewModel : ViewModel() {

    private val _state = MutableStateFlow(MorseState())
    val state: StateFlow<MorseState> = _state.asStateFlow()

    private var autoCommitJob: Job? = null

    fun addDot() = addSymbol(".")
    fun addDash() = addSymbol("-")

    private fun addSymbol(symbol: String) {
        autoCommitJob?.cancel()
        val current = _state.value
        val newCode = current.currentCode + symbol

        if (newCode.length > MAX_CODE_DEPTH) {
            triggerInvalid()
            return
        }

        val resolved = charForCode(newCode)
        _state.update { it.copy(currentCode = newCode, currentChar = resolved, invalid = false) }

        if (resolved != null) {
            autoCommitJob = viewModelScope.launch {
                delay(AUTO_COMMIT_DELAY_MS)
                commit()
            }
        }
    }

    fun commit() {
        val char = _state.value.currentChar ?: return
        _state.update { state ->
            val newHistory = (state.history + char).takeLast(HISTORY_LIMIT)
            state.copy(currentCode = "", currentChar = null, history = newHistory, invalid = false)
        }
    }

    fun clear() {
        autoCommitJob?.cancel()
        _state.update { it.copy(currentCode = "", currentChar = null, invalid = false) }
    }

    private fun triggerInvalid() {
        _state.update { it.copy(currentCode = "", currentChar = null, invalid = true) }
        viewModelScope.launch {
            delay(INVALID_CLEAR_DELAY_MS)
            _state.update { it.copy(invalid = false) }
        }
    }
}

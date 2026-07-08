package org.lerchenflo.schneaggchatv3mp.games.presentation.coinflip

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CoinSide {
    HEADS,
    TAILS
}

data class CoinFlipState(
    val tailsLabel: String = "",
    val result: CoinSide = CoinSide.HEADS,
    // Incremented on every flip to signal the UI to run a new toss animation
    val flipTrigger: Int = 0,
    val isFlipping: Boolean = false
)

sealed class CoinFlipAction {
    data class OnTailsLabelChange(val label: String) : CoinFlipAction()
    object Flip : CoinFlipAction()
    object AnimationFinished : CoinFlipAction()
}

class CoinFlipViewModel : ViewModel() {

    private val _state = MutableStateFlow(CoinFlipState())
    val state: StateFlow<CoinFlipState> = _state.asStateFlow()

    fun onAction(action: CoinFlipAction) {
        when (action) {
            is CoinFlipAction.OnTailsLabelChange -> _state.update { it.copy(tailsLabel = action.label) }
            is CoinFlipAction.Flip -> flip()
            is CoinFlipAction.AnimationFinished -> _state.update { it.copy(isFlipping = false) }
        }
    }

    private fun flip() {
        if (_state.value.isFlipping) return

        _state.update {
            it.copy(
                result = CoinSide.entries.random(),
                flipTrigger = it.flipTrigger + 1,
                isFlipping = true
            )
        }
    }
}

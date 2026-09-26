package org.lerchenflo.schneaggchatv3mp.tools.presentation.fuelcalculator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.lerchenflo.schneaggchatv3mp.tools.domain.FuelMixBasis

private const val MAX_AMOUNT_INPUT_LENGTH = 10

/** The highest ratio is three digits, see MAX_FUEL_RATIO. */
private const val MAX_RATIO_INPUT_LENGTH = 3

class FuelCalculatorViewModel : ViewModel() {

    private val _state = MutableStateFlow(FuelCalculatorState())
    val state = _state.asStateFlow()

    fun onAction(action: FuelCalculatorAction) {
        when (action) {
            is FuelCalculatorAction.OnBasisSelect -> selectBasis(action.basis)
            // Length-capped rather than range-checked: a jerrycan never needs more digits, and it
            // keeps an absurd paste out of the fixed-decimal formatter
            is FuelCalculatorAction.OnAmountChange ->
                if (action.amount.length <= MAX_AMOUNT_INPUT_LENGTH) {
                    _state.update { it.copy(amountInput = action.amount) }
                }
            is FuelCalculatorAction.OnRatioChange ->
                if (action.ratio.length <= MAX_RATIO_INPUT_LENGTH) {
                    _state.update { it.copy(ratioInput = action.ratio) }
                }
            is FuelCalculatorAction.OnRatioPresetSelect ->
                _state.update { it.copy(ratioInput = action.ratio.toString()) }
            FuelCalculatorAction.OnClear -> _state.update { it.copy(amountInput = "") }
        }
    }

    /**
     * Petrol and mixture are both litres, so a switch between them keeps the number. Oil is entered
     * in millilitres, so crossing that boundary would silently reinterpret it a thousandfold.
     */
    private fun selectBasis(basis: FuelMixBasis) {
        _state.update { state ->
            if (basis == state.basis) return@update state
            val unitChanged = (basis == FuelMixBasis.OIL) != (state.basis == FuelMixBasis.OIL)
            state.copy(
                basis = basis,
                amountInput = if (unitChanged) "" else state.amountInput,
            )
        }
    }
}

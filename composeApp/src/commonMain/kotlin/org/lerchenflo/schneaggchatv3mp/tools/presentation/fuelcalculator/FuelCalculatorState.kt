package org.lerchenflo.schneaggchatv3mp.tools.presentation.fuelcalculator

import org.lerchenflo.schneaggchatv3mp.tools.domain.DEFAULT_FUEL_RATIO
import org.lerchenflo.schneaggchatv3mp.tools.domain.FuelMixBasis
import org.lerchenflo.schneaggchatv3mp.tools.domain.FuelMixResult
import org.lerchenflo.schneaggchatv3mp.tools.domain.MAX_FUEL_RATIO
import org.lerchenflo.schneaggchatv3mp.tools.domain.MILLILITRES_PER_LITRE
import org.lerchenflo.schneaggchatv3mp.tools.domain.MIN_FUEL_RATIO
import org.lerchenflo.schneaggchatv3mp.tools.domain.calculateFuelMix
import org.lerchenflo.schneaggchatv3mp.tools.domain.parseFuelAmount

/**
 * The calculator is pure arithmetic, so the state keeps only what was typed and derives the rest.
 * Both text fields stay raw strings: a half-typed "1," has to survive in the field.
 */
data class FuelCalculatorState(
    val basis: FuelMixBasis = FuelMixBasis.PETROL,
    val amountInput: String = "",
    val ratioInput: String = DEFAULT_FUEL_RATIO.toString(),
) {
    /** The petrol part of petrol:oil, null while the typed ratio is unusable. */
    val ratio: Int?
        get() = ratioInput.trim().toIntOrNull()?.takeIf { it in MIN_FUEL_RATIO..MAX_FUEL_RATIO }

    /** The typed amount in litres; the oil field is entered in millilitres and converted here. */
    private val amountLitres: Double?
        get() = parseFuelAmount(amountInput)
            ?.let { if (basis == FuelMixBasis.OIL) it / MILLILITRES_PER_LITRE else it }

    val result: FuelMixResult?
        get() {
            val ratio = ratio ?: return null
            val amount = amountLitres ?: return null
            return calculateFuelMix(basis, amount, ratio)
        }

    /** An empty field is not an error yet - only something typed that is not a positive number. */
    val amountError: Boolean get() = amountInput.isNotBlank() && amountLitres == null

    val ratioError: Boolean get() = ratio == null
}

sealed interface FuelCalculatorAction {
    data class OnBasisSelect(val basis: FuelMixBasis) : FuelCalculatorAction
    data class OnAmountChange(val amount: String) : FuelCalculatorAction
    data class OnRatioChange(val ratio: String) : FuelCalculatorAction
    data class OnRatioPresetSelect(val ratio: Int) : FuelCalculatorAction
    data object OnClear : FuelCalculatorAction
}

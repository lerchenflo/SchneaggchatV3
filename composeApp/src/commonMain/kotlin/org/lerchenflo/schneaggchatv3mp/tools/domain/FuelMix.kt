package org.lerchenflo.schneaggchatv3mp.tools.domain

import kotlin.math.abs
import kotlin.math.roundToLong

const val MILLILITRES_PER_LITRE = 1000.0

/** Mix ratios two-stroke engines are commonly run at, as the petrol part of petrol:oil. */
val FUEL_RATIO_PRESETS = listOf(25, 32, 40, 50, 60, 100)

const val DEFAULT_FUEL_RATIO = 50

const val MIN_FUEL_RATIO = 1
const val MAX_FUEL_RATIO = 200

/**
 * Which of the three quantities of a batch the user typed in. The other two follow from the ratio,
 * so mixing can start from whichever one is actually known.
 */
enum class FuelMixBasis {
    /** Litres of plain petrol in the can; the oil still has to go in on top. */
    PETROL,

    /** Litres of finished mix, petrol and oil together - a tank that must end up exactly full. */
    MIXTURE,

    /** The oil, for when the rest of the bottle is what decides how big the batch gets. */
    OIL,
}

/** A finished two-stroke batch. [petrolLitres] plus [oilLitres] is always [totalLitres]. */
data class FuelMixResult(
    val petrolLitres: Double,
    val oilLitres: Double,
    val totalLitres: Double,
)

/**
 * Splits a two-stroke batch mixed at petrol:oil = [ratio]:1. [amountLitres] is read as whatever
 * [basis] names, so one ratio answers all three questions a mixing can start from.
 * Returns null for anything that cannot describe a batch.
 */
fun calculateFuelMix(basis: FuelMixBasis, amountLitres: Double, ratio: Int): FuelMixResult? {
    if (ratio < MIN_FUEL_RATIO || !amountLitres.isFinite() || amountLitres <= 0.0) return null

    val petrol: Double
    val oil: Double
    when (basis) {
        FuelMixBasis.PETROL -> {
            petrol = amountLitres
            oil = amountLitres / ratio
        }
        // The finished mix divides into ratio + 1 parts, not ratio - the oil is part of the volume
        FuelMixBasis.MIXTURE -> {
            petrol = amountLitres * ratio / (ratio + 1)
            oil = amountLitres / (ratio + 1)
        }
        FuelMixBasis.OIL -> {
            oil = amountLitres
            petrol = amountLitres * ratio
        }
    }
    return FuelMixResult(petrolLitres = petrol, oilLitres = oil, totalLitres = petrol + oil)
}

/** Parses a typed amount, accepting the comma as a decimal separator like the keypad produces it. */
fun parseFuelAmount(input: String): Double? =
    input.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }

/**
 * Fixed-decimal formatting, because kotlin common has no printf and toString() would fall back to
 * scientific notation on the extremes.
 */
private fun Double.toFixed(decimals: Int): String {
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val scaled = (abs(this) * factor).roundToLong()
    val sign = if (this < 0) "-" else ""
    val whole = scaled / factor
    if (decimals == 0) return "$sign$whole"
    return "$sign$whole.${(scaled % factor).toString().padStart(decimals, '0')}"
}

/** Litres with two decimals - the resolution of a jerrycan scale. */
fun formatLitres(litres: Double): String = litres.toFixed(2)

/** Oil is measured out with a syringe, so it is always given in whole millilitres. */
fun formatMillilitres(litres: Double): String = (litres * MILLILITRES_PER_LITRE).toFixed(0)

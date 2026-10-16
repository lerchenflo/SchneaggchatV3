package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split

import kotlin.math.absoluteValue

/** Currency of a split container without settings (e.g. created by an older client). */
const val DEFAULT_SPLIT_CURRENCY = "EUR"

/** Highest amount one expense may have, mirrors the server limit. */
const val MAX_SPLIT_AMOUNT_CENTS = 100_000_000L

// Up to 9 digits, optionally followed by "." or "," and up to 2 decimals. Also allows the
// in-between states while typing ("12," / ",5").
private val AMOUNT_INPUT_REGEX = Regex("^\\d{0,9}([.,]\\d{0,2})?$")

fun currencySymbol(currency: String): String = when (currency) {
    "EUR" -> "€"
    "USD" -> "$"
    "GBP" -> "£"
    else -> currency
}

/**
 * "1234,50 €" for 123450 cents. [decimalSeparator] comes from the current language.
 * With [signed] a positive amount gets a leading "+", a negative one always gets "-".
 */
fun formatMoney(cents: Long, currency: String, decimalSeparator: String, signed: Boolean = false): String {
    val sign = when {
        cents < 0L -> "-"
        signed && cents > 0L -> "+"
        else -> ""
    }
    return "$sign${formatAmountNumber(cents.absoluteValue, decimalSeparator)} ${currencySymbol(currency)}"
}

/** "1234,50" - the number only, also used to prefill the amount field. */
fun formatAmountNumber(cents: Long, decimalSeparator: String): String {
    val whole = cents / 100
    val fraction = (cents % 100).toString().padStart(2, '0')
    return "$whole$decimalSeparator$fraction"
}

/** Whether [text] may stand in the amount field (also while typing). */
fun isValidAmountInput(text: String): Boolean = AMOUNT_INPUT_REGEX.matches(text)

/** "12" / "12.5" / "12,50" to cents; null when blank or not an amount. */
fun parseAmountToCents(text: String): Long? {
    if (text.isBlank() || !isValidAmountInput(text)) return null

    val parts = text.split('.', ',')
    val whole = parts[0].ifEmpty { "0" }.toLong()
    val fraction = parts.getOrNull(1).orEmpty().padEnd(2, '0').toLong()
    return whole * 100 + fraction
}

package org.lerchenflo.schneaggchatv3mp.utilities

import kotlin.math.abs
import kotlin.math.roundToLong

fun <T> MutableList<T>.toFormattedString(): String {
    if (isEmpty()) return "[]"
    return buildString {
        appendLine("[")
        this@toFormattedString.forEachIndexed { index, item ->
            appendLine("  [$index] $item")
        }
        append("]")
    }
}
/**
 * Fixed-decimal formatting, because kotlin common has no printf and toString() would fall back to
 * scientific notation on the extremes.
 */
fun Double.toFixed(decimals: Int): String {
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val scaled = (abs(this) * factor).roundToLong()
    val sign = if (this < 0) "-" else ""
    val whole = scaled / factor
    if (decimals == 0) return "$sign$whole"
    return "$sign$whole.${(scaled % factor).toString().padStart(decimals, '0')}"
}

package org.lerchenflo.schneaggchatv3mp.utilities

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
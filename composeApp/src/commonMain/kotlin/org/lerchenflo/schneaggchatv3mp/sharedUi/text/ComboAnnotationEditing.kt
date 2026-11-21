package org.lerchenflo.schneaggchatv3mp.sharedUi.text

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Keeps every annotation in an input field one token, the way the field shows it (one name):
 * - an edit that touches an annotation removes or replaces it whole, so one backspace or delete
 *   removes a whole mention;
 * - text typed (or a space deleted) right after an annotation gets a space in between, else it
 *   would become part of the raw id. A deleted space stays and the cursor jumps over it, so the
 *   next backspace removes the mention.
 * Any other change passes through unchanged.
 */
internal fun keepAnnotationsWhole(
    old: TextFieldValue,
    new: TextFieldValue,
    sources: List<ComboAnnotationSource>
): TextFieldValue {
    if (old.text == new.text) return new
    val annotations = findComboAnnotations(old.text, sources)
    if (annotations.isEmpty()) return new

    val edit = findTextEdit(old.text, new.text, new.selection.min)
    val touched = annotations.filter { it.range.first < edit.oldEnd && edit.start <= it.range.last }
    val start = minOf(edit.start, touched.firstOrNull()?.range?.first ?: edit.start)
    val end = maxOf(edit.oldEnd, touched.lastOrNull()?.range?.last?.plus(1) ?: edit.oldEnd)

    val followsAnnotation = annotations.any { it.range.last + 1 == start && it !in touched }
    val nextChar = (edit.inserted + old.text.substring(end)).firstOrNull()
    val separator = if (followsAnnotation && nextChar != null && nextChar.isAnnotationIdChar()) " " else ""

    if (touched.isEmpty() && separator.isEmpty()) return new
    return TextFieldValue(
        text = old.text.substring(0, start) + separator + edit.inserted + old.text.substring(end),
        selection = TextRange(if (edit.inserted.isEmpty()) start else start + separator.length + edit.inserted.length)
    )
}

/** [old] = text before the edit; the range start until oldEnd of it was replaced by [inserted]. */
private data class TextEdit(val start: Int, val oldEnd: Int, val inserted: String)

private fun findTextEdit(old: String, new: String, newCursor: Int): TextEdit {
    val shorter = minOf(old.length, new.length)
    var start = 0
    while (start < shorter && old[start] == new[start]) start++
    // With repeated letters the change fits several places; the cursor ends behind it
    start = minOf(start, newCursor)
    var suffix = 0
    while (suffix < shorter - start && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
    return TextEdit(start = start, oldEnd = old.length - suffix, inserted = new.substring(start, new.length - suffix))
}

/** Characters [ComboAnnotationType.regex] reads as part of an id. */
private fun Char.isAnnotationIdChar(): Boolean =
    this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this == '_' || this == '-'

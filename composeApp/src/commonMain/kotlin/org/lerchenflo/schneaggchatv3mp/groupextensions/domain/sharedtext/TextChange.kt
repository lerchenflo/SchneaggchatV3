package org.lerchenflo.schneaggchatv3mp.groupextensions.domain.sharedtext

/**
 * The one part where two texts differ: old[start, oldEnd) was replaced by new[start, newEnd).
 * Found by trimming the common start and end (see [textChange]), so several edits close together
 * count as one change.
 */
data class TextChange(
    val start: Int,
    val oldEnd: Int,
    val newEnd: Int,
) {
    /** How many chars the text grew by (negative when it shrank). */
    val lengthDelta: Int get() = newEnd - oldEnd

    /** Where a position of the old text (e.g. the cursor) ends up in the new text. */
    fun mapOffset(offset: Int): Int = when {
        offset <= start -> offset
        offset >= oldEnd -> offset + lengthDelta
        else -> newEnd
    }
}

/** The change from [old] to [new], or null when both are equal. Never splits a surrogate pair (emoji). */
fun textChange(old: String, new: String): TextChange? {
    if (old == new) return null

    val shorterLength = minOf(old.length, new.length)
    var prefix = 0
    while (prefix < shorterLength && old[prefix] == new[prefix]) prefix++
    if (prefix > 0 && old[prefix - 1].isHighSurrogate()) prefix--

    var suffix = 0
    while (suffix < shorterLength - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
    if (suffix > 0 && old[old.length - suffix].isLowSurrogate()) suffix--

    return TextChange(
        start = prefix,
        oldEnd = old.length - suffix,
        newEnd = new.length - suffix,
    )
}

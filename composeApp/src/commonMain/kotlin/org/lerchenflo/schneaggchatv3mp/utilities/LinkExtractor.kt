package org.lerchenflo.schneaggchatv3mp.utilities

/**
 * Matches an http(s) url or a bare `www.` one. Kept deliberately simple - this only has to find
 * links a user typed into a chat message, not validate them. Brackets and quotes end a match, so
 * a markdown link `[label](https://x.y)` yields its target rather than the whole construct.
 */
private val LINK_REGEX = Regex(
    """(?:https?://|www\.)[^\s<>"'()\[\]{}]+""",
    RegexOption.IGNORE_CASE
)

/** Trailing punctuation that almost always belongs to the sentence, not to the url. */
private val TRAILING_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?')

/**
 * All urls contained in [text], in the order they appear, without duplicates.
 *
 * A bare `www.x.y` is returned as typed - that is what has to be shown to the reader, see
 * [toOpenableUrl] for the form a browser needs.
 */
fun extractLinks(text: String): List<String> {
    if (text.isEmpty()) return emptyList()

    return LINK_REGEX.findAll(text)
        .map { it.value.trimEnd(*TRAILING_PUNCTUATION) }
        .filter { it.isNotEmpty() }
        .distinct()
        .toList()
}

/**
 * The url made openable: a bare `www.` link needs a scheme before a browser will take it, so it
 * gets https prepended. Anything that already carries a scheme is handed back untouched.
 */
fun String.toOpenableUrl(): String =
    if (startsWith("www.", ignoreCase = true)) "https://$this" else this

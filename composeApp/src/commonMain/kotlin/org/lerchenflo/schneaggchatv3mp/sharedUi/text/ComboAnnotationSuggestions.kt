package org.lerchenflo.schneaggchatv3mp.sharedUi.text

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal sealed interface AnnotationSuggestion {
    /** An annotation type ("@ function"); picking it narrows the names to this type. */
    data class Type(val source: ComboAnnotationSource) : AnnotationSuggestion

    /** A concrete entity of a type. */
    data class Name(
        val source: ComboAnnotationSource,
        val id: String,
        val name: String
    ) : AnnotationSuggestion
}

/** The row text: the type's icon with its trigger or with the name. */
internal val AnnotationSuggestion.label: String
    get() = when (this) {
        is AnnotationSuggestion.Type -> source.type.displayPrefix + " " + source.type.trigger
        is AnnotationSuggestion.Name -> source.type.displayName(name)
    }

internal data class AnnotationSuggestionState(
    /** Index of the `@` that started the token. */
    val tokenStart: Int,
    /** Cursor position; the token is the text between tokenStart and cursor. */
    val cursor: Int,
    val suggestions: List<AnnotationSuggestion>
)

private const val MAX_NAME_SUGGESTIONS = 8
private const val MAX_QUERY_LENGTH = 30

/**
 * Between a fully typed trigger and the name query, e.g. `@map Zu`. Not `/`: `@user/Max` already
 * reads as a raw annotation with the id "Max".
 */
private const val TRIGGER_SEPARATOR = ' '

/**
 * Suggestions for the `@` token right before the cursor, or null when there is none:
 * - `@` + text searches the names of every type at once; types whose trigger starts with the
 *   token are offered too (`@` alone: every type, then the best ranked names);
 * - a full trigger (`@map`, `@user`, ...) narrows the names to that type, text after a space
 *   filters them.
 * Names are ordered by [suggestionRank]. Names come only from the local tables in [sources].
 */
internal fun findAnnotationSuggestions(
    value: TextFieldValue,
    sources: List<ComboAnnotationSource>,
    contextIds: Set<String>
): AnnotationSuggestionState? {
    if (sources.isEmpty()) return null
    val tokenStart = findTokenStart(value, sources) ?: return null
    val cursor = value.selection.start
    val token = value.text.substring(tokenStart, cursor)

    val narrowedSource = sources.firstOrNull { token.isTriggerOf(it) }
    val names = if (narrowedSource != null) {
        val query = token.substring(narrowedSource.type.trigger.length).trimStart(TRIGGER_SEPARATOR)
        val narrowed = matchingNames(listOf(narrowedSource), query, contextIds)
        // Exactly "@map" may also be the start of a name such as "Mapper"
        if (token.length == narrowedSource.type.trigger.length) {
            narrowed + matchingNames(sources, token.drop(1), contextIds)
        } else narrowed
    } else {
        matchingNames(sources, token.drop(1), contextIds)
    }

    val types = if (narrowedSource != null) emptyList() else sources
        .filter { it.type.trigger.startsWith(token, ignoreCase = true) }
        .map { AnnotationSuggestion.Type(it) }

    return AnnotationSuggestionState(
        tokenStart = tokenStart,
        cursor = cursor,
        suggestions = types + names.distinct().take(MAX_NAME_SUGGESTIONS)
    )
}

/** Inserts the picked suggestion in place of the typed token. */
internal fun applySuggestion(
    value: TextFieldValue,
    state: AnnotationSuggestionState,
    suggestion: AnnotationSuggestion
): TextFieldValue {
    val insertion = when (suggestion) {
        // The separator keeps the narrowing while the user types the name
        is AnnotationSuggestion.Type -> suggestion.source.type.trigger + TRIGGER_SEPARATOR
        is AnnotationSuggestion.Name -> suggestion.source.type.build(suggestion.id) + " "
    }
    val newText = value.text.replaceRange(state.tokenStart, state.cursor, insertion)
    return TextFieldValue(
        text = newText,
        selection = TextRange(state.tokenStart + insertion.length)
    )
}

/**
 * Index of the `@` the cursor is typing after, or null: it must start the text or follow
 * whitespace (rules out email addresses), stay on one line, and not be part of an annotation
 * that is already in the text.
 */
private fun findTokenStart(value: TextFieldValue, sources: List<ComboAnnotationSource>): Int? {
    if (!value.selection.collapsed) return null
    val cursor = value.selection.start
    val textBefore = value.text.substring(0, cursor)
    val atIndex = textBefore.lastIndexOf('@')
    if (atIndex == -1) return null
    if (atIndex > 0 && !textBefore[atIndex - 1].isWhitespace()) return null
    if ('\n' in textBefore.substring(atIndex)) return null
    if (findComboAnnotations(value.text, sources).any { atIndex in it.range }) return null
    return atIndex
}

/** True for exactly the trigger, or the trigger followed by a separator and the name query. */
private fun String.isTriggerOf(source: ComboAnnotationSource): Boolean {
    val trigger = source.type.trigger
    if (!startsWith(trigger, ignoreCase = true)) return false
    return length == trigger.length || this[trigger.length] == TRIGGER_SEPARATOR
}

private fun matchingNames(
    sources: List<ComboAnnotationSource>,
    query: String,
    contextIds: Set<String>
): List<AnnotationSuggestion.Name> {
    if (query.length > MAX_QUERY_LENGTH) return emptyList()
    return sources
        .flatMap { source ->
            source.names.entries
                .filter { it.value.contains(query, ignoreCase = true) }
                .map { AnnotationSuggestion.Name(source, it.key, it.value) }
        }
        .sortedWith(
            compareBy<AnnotationSuggestion.Name> { it.suggestionRank(contextIds) }
                .thenBy { if (it.name.hasWordStartingWith(query)) 0 else 1 }
                .thenBy { it.name.lowercase() }
        )
}

/** 0 = the screen's context (e.g. the chat's members), 1 = the source's preferred ids (friends), 2 = the rest. */
private fun AnnotationSuggestion.Name.suggestionRank(contextIds: Set<String>): Int = when (id) {
    in contextIds -> 0
    in source.preferredIds -> 1
    else -> 2
}

/** True when [query] matches at the start of any word of this name ("mu" in "Max Muster"). */
private fun String.hasWordStartingWith(query: String): Boolean =
    indices.any { i -> (i == 0 || !this[i - 1].isLetterOrDigit()) && startsWith(query, startIndex = i, ignoreCase = true) }

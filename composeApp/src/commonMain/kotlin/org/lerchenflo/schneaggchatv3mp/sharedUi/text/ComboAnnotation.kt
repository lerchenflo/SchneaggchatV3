package org.lerchenflo.schneaggchatv3mp.sharedUi.text

/**
 * A category of inline text annotation with the raw format `@<key>/<24-char-hex-id>`,
 * e.g. `@map/location/68ab34f2c91d05e7b8a41c22`.
 *
 * To support a new entity (e.g. user mentions) add an instance to [ComboAnnotationTypes]
 * and pass a matching [ComboAnnotationSource] to [ComboText] / [ComboInputField] —
 * no changes to the composables needed.
 */
class ComboAnnotationType(
    /** Path-like identifier used in the raw text, e.g. "map/location". */
    val key: String,
    /** Prefix shown in front of a resolved name, e.g. a pin emoji. */
    val displayPrefix: String,
    /** What the user has to type before autocomplete opens, e.g. "@map". */
    val trigger: String = "@" + key.substringBefore('/')
) {
    // Lookahead so the id can't silently match the first 24 chars of a longer hex string
    /** Matches one raw annotation of this type; group 1 is the entity id (MongoDB ObjectId). */
    val regex = Regex("@${Regex.escape(key)}/([0-9a-fA-F]{24})(?![0-9a-fA-F])")

    /** Fake uri scheme used to route markdown link clicks back to the app. */
    val linkScheme = "schneaggchat://$key/"

    fun build(id: String): String = "@$key/$id"

    fun displayName(name: String): String = displayPrefix + name
}

object ComboAnnotationTypes {
    val MAP_LOCATION = ComboAnnotationType(key = "map/location", displayPrefix = "📍")
    // Future: val USER = ComboAnnotationType(key = "user", displayPrefix = "@")
}

/**
 * One annotation type together with its resolvable names and click handling —
 * everything the combo composables need to render, complete and open annotations.
 */
data class ComboAnnotationSource(
    val type: ComboAnnotationType,
    /** entity id -> display name */
    val names: Map<String, String>,
    val onClick: (id: String) -> Unit = {}
)

/** A resolved annotation match inside a raw text. */
internal data class ComboAnnotationMatch(
    val range: IntRange,
    val id: String,
    val name: String,
    val source: ComboAnnotationSource
)

/**
 * Finds all annotations of all [sources] in [text], ordered by position.
 * Annotations whose id has no known name are skipped (they stay raw text).
 */
internal fun findComboAnnotations(
    text: String,
    sources: List<ComboAnnotationSource>
): List<ComboAnnotationMatch> =
    sources.flatMap { source ->
        source.type.regex.findAll(text).mapNotNull { match ->
            val id = match.groupValues[1]
            val name = source.names[id] ?: return@mapNotNull null
            ComboAnnotationMatch(match.range, id, name, source)
        }
    }.sortedBy { it.range.first }

/** Replaces all resolvable annotations with their display name (plain text, e.g. for previews). */
fun resolveComboAnnotationsToPlainText(
    text: String,
    sources: List<ComboAnnotationSource>
): String {
    val matches = findComboAnnotations(text, sources)
    if (matches.isEmpty()) return text
    return buildString {
        var last = 0
        matches.forEach { match ->
            append(text, last, match.range.first)
            append(match.source.type.displayName(match.name))
            last = match.range.last + 1
        }
        append(text, last, text.length)
    }
}

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
    /** Matches one raw annotation of this type; group 1 is the entity id (ObjectId or slug key). */
    val regex = Regex("@${Regex.escape(key)}/([a-zA-Z0-9_-]{1,64})(?![a-zA-Z0-9_-])")

    /** Fake uri scheme used to route markdown link clicks back to the app. */
    val linkScheme = "schneaggchat://$key/"

    fun build(id: String): String = "@$key/$id"

    fun displayName(name: String): String = displayPrefix + name
}

object ComboAnnotationTypes {
    val MAP_LOCATION = ComboAnnotationType(key = "map/location", displayPrefix = "📍")
    val USER = ComboAnnotationType(key = "user", displayPrefix = "👤")
    val GAME = ComboAnnotationType(key = "game", displayPrefix = "🎮")
    val EVENT = ComboAnnotationType(key = "event", displayPrefix = "📅")
}

/**
 * One annotation type together with its resolvable names and click handling —
 * everything the combo composables need to render, complete and open annotations.
 */
data class ComboAnnotationSource(
    val type: ComboAnnotationType,
    /** entity id -> display name */
    val names: Map<String, String>,
    /** Shown in place of the raw text when an id is unknown (deleted or not yet synced). */
    val unresolvedName: String,
    val onClick: (id: String) -> Unit = {}
)

/** A resolved (or placeholder) annotation match inside a raw text. */
internal data class ComboAnnotationMatch(
    val range: IntRange,
    val id: String,
    val name: String,
    val source: ComboAnnotationSource,
    /** false when [name] is the source's placeholder because the id is unknown. */
    val resolved: Boolean
)

/**
 * Finds all annotations of all [sources] in [text], ordered by position.
 *
 * An unknown id still yields a match carrying the source's placeholder name, so the raw
 * `@user/<id>` never reaches the reader. Pass [includeUnresolved] = false where the raw text
 * has to stay editable (the input field).
 */
internal fun findComboAnnotations(
    text: String,
    sources: List<ComboAnnotationSource>,
    includeUnresolved: Boolean = true
): List<ComboAnnotationMatch> =
    sources.flatMap { source ->
        source.type.regex.findAll(text).mapNotNull { match ->
            val id = match.groupValues[1]
            val name = source.names[id]
            if (name == null && !includeUnresolved) return@mapNotNull null
            ComboAnnotationMatch(
                range = match.range,
                id = id,
                name = name ?: source.unresolvedName,
                source = source,
                resolved = name != null
            )
        }
    }.sortedBy { it.range.first }

/** Replaces all annotations (resolved or not) with their display name (plain text, e.g. for previews). */
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

package org.lerchenflo.schneaggchatv3mp.utilities.annotations

/**
 * A category of inline text annotation with the raw format `@<key>/<24-char-hex-id>`,
 * e.g. `@map/location/68ab34f2c91d05e7b8a41c22`.
 *
 * The text format only, shared by the UI (`sharedUi/text`: ComboText, ComboInputField) and the
 * features that read annotations in stored texts (e.g. mentions of the own user). To support a new
 * entity add an instance to [ComboAnnotationTypes] and pass a matching `ComboAnnotationSource` to
 * ComboText / ComboInputField — no changes to the composables needed.
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
    /** A feature request or bug report of the feedback board. */
    val FEEDBACK = ComboAnnotationType(key = "feedback", displayPrefix = "💡")
    /** A settings page; the id is a slug key like the games' (see SettingsAnnotationPage). */
    val SETTINGS = ComboAnnotationType(key = "settings", displayPrefix = "⚙️")
}

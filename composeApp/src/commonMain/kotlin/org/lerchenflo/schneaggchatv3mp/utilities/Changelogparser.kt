package org.lerchenflo.schneaggchatv3mp.utilities

/**
 * Parses a changelog section for a specific version from a Markdown README string.
 *
 * Versions are expected to be marked as `### X.Y.Z` headings.
 */
object ChangelogParser {

    /**
     * Returns the raw Markdown text for the given [version], or `null` if not found.
     *
     * The returned text includes everything from the version heading up to (but not
     * including) the next `###`-level heading or the end of the changelog block.
     *
     * @param readme  The full README / changelog string.
     * @param version The version to look up, e.g. `"3.0.6"`.
     */
    fun getChangelog(readme: String, version: String): String? {
        val lines = readme.lines()

        // Find the line index of the requested version heading
        val startIndex = lines.indexOfFirst { line ->
            line.trimStart().startsWith("### ") &&
                    line.trimStart().removePrefix("### ").trim().startsWith(version)
        }

        if (startIndex == -1) return null

        // Collect lines until the next ### heading (or end of string)
        val changelogLines = mutableListOf<String>()
        for (i in startIndex until lines.size) {
            // Stop at the next version heading (but not the one we started on)
            if (i != startIndex && lines[i].trimStart().startsWith("### ")) break
            changelogLines.add(lines[i])
        }

        return changelogLines.joinToString("\n").trim()
    }

    /**
     * Returns a [ChangelogEntry] with parsed [ChangelogEntry.features] and
     * [ChangelogEntry.bugfixes] lists for the given [version], or `null` if not found.
     */
    fun getParsedChangelog(readme: String, version: String): ChangelogEntry? {
        val raw = getChangelog(readme, version) ?: return null
        val lines = raw.lines()

        val features = mutableListOf<String>()
        val bugfixes = mutableListOf<String>()
        var currentSection: MutableList<String>? = null

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#### Features") -> currentSection = features
                trimmed.startsWith("#### Bugfixes") -> currentSection = bugfixes
                trimmed.startsWith("- ") -> currentSection?.add(trimmed.removePrefix("- ").trim())
            }
        }

        return ChangelogEntry(version = version, features = features, bugfixes = bugfixes)
    }
}

data class ChangelogEntry(
    val version: String,
    val features: List<String>,
    val bugfixes: List<String>,
)
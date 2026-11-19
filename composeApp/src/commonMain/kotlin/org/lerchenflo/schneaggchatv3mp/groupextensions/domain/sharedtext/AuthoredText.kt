package org.lerchenflo.schneaggchatv3mp.groupextensions.domain.sharedtext

import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.TextWriterRun

/** Highest length of a shared text, mirrors the server limit. */
const val MAX_SHARED_TEXT_LENGTH = 20_000

/**
 * A shared text together with who wrote each part: [runs] are consecutive and add up to
 * text.length. Text without a known writer belongs to [UNKNOWN_WRITER].
 */
data class AuthoredText(
    val text: String,
    val runs: List<TextWriterRun>,
) {
    val length: Int get() = text.length

    /** Everyone who wrote a part of the text. */
    val writerIds: Set<String>
        get() = runs.asSequence().map { it.writtenBy }.filter { it != UNKNOWN_WRITER }.toSet()

    /** The part from [start] (inclusive) to [end] (exclusive), with its writers. */
    fun slice(start: Int, end: Int): AuthoredText =
        AuthoredText(text.substring(start, end), cutRuns(runs, start, end))

    operator fun plus(other: AuthoredText): AuthoredText =
        AuthoredText(text + other.text, joinRuns(runs + other.runs))

    /** Replaces the part from [start] to [end] with [replacement], which keeps its own writers. */
    fun replace(start: Int, end: Int, replacement: AuthoredText): AuthoredText =
        slice(0, start) + replacement + slice(end, length)

    /** This text after [writerId] changed it to [newText]: the changed part is credited to [writerId]. */
    fun edit(newText: String, writerId: String): AuthoredText {
        val change = textChange(text, newText) ?: return this
        val inserted = writtenBy(newText.substring(change.start, change.newEnd), writerId)
        return replace(change.start, change.oldEnd, inserted)
    }

    companion object {
        const val UNKNOWN_WRITER = ""

        val EMPTY = AuthoredText("", emptyList())

        /** [text] written completely by [writerId]. */
        fun writtenBy(text: String, writerId: String): AuthoredText =
            AuthoredText(text, if (text.isEmpty()) emptyList() else listOf(TextWriterRun(text.length, writerId)))

        /**
         * Pairs [text] with the writer [runs] the server sent, repaired so they cover the text
         * exactly: an uncovered end counts as [UNKNOWN_WRITER], runs past the end are cut off.
         */
        fun fromServer(text: String, runs: List<TextWriterRun>): AuthoredText {
            val covered = runs.sumOf { it.length.coerceAtLeast(0) }
            val padded = if (covered < text.length) runs + TextWriterRun(text.length - covered, UNKNOWN_WRITER) else runs
            return AuthoredText(text, cutRuns(padded, 0, text.length))
        }
    }
}

/** The parts of [runs] between the text positions [start] and [end], with neighbours of the same writer joined. */
private fun cutRuns(runs: List<TextWriterRun>, start: Int, end: Int): List<TextWriterRun> {
    val cut = mutableListOf<TextWriterRun>()
    var runStart = 0
    for (run in runs) {
        if (runStart >= end) break
        val runEnd = runStart + run.length.coerceAtLeast(0)
        val from = maxOf(runStart, start)
        val to = minOf(runEnd, end)
        if (from < to) cut += TextWriterRun(length = to - from, writtenBy = run.writtenBy)
        runStart = runEnd
    }
    return joinRuns(cut)
}

/** Joins neighbouring runs of the same writer and drops empty ones. */
private fun joinRuns(runs: List<TextWriterRun>): List<TextWriterRun> {
    val joined = mutableListOf<TextWriterRun>()
    for (run in runs) {
        if (run.length <= 0) continue
        val last = joined.lastOrNull()
        if (last != null && last.writtenBy == run.writtenBy) {
            joined[joined.lastIndex] = last.copy(length = last.length + run.length)
        } else {
            joined += run
        }
    }
    return joined
}

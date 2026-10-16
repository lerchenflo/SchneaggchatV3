package org.lerchenflo.schneaggchatv3mp.games.presentation.cchallenge

import kotlinx.datetime.LocalDate
import org.lerchenflo.schneaggchatv3mp.games.domain.SplitMix64

/** Day of challenge #1. */
private val FIRST_CHALLENGE_DAY = LocalDate(2026, 10, 8).toEpochDays()

private const val OPTION_COUNT = 4

/**
 * Today's challenge, identical for everyone on the same local day. The type rotates through
 * all four kinds in a shuffled order every four days, so the same kind never comes up
 * three days in a row.
 */
fun generateDailyCChallenge(epochDay: Long): CChallenge {
    val number = (epochDay - FIRST_CHALLENGE_DAY + 1).toInt().coerceAtLeast(1)

    val typeOrder = CChallengeType.entries.toMutableList()
    SplitMix64(epochDay.floorDiv(CChallengeType.entries.size) * 0x5DEECE66DL + 0xC0DEL).shuffle(typeOrder)
    val type = typeOrder[epochDay.mod(typeOrder.size)]

    val rng = SplitMix64(epochDay * 0x2545F4914F6CDD1DL + 0xC0DEL)
    return when (type) {
        CChallengeType.FIX_SYNTAX -> fixSyntax(number, rng)
        CChallengeType.FILL_BLANK -> fillBlank(number, rng)
        CChallengeType.PREDICT_OUTPUT -> predictOutput(number, rng)
        CChallengeType.ORDER_LINES -> orderLines(number, rng)
    }
}

private fun fixSyntax(number: Int, rng: SplitMix64): CChallenge {
    val snippet = C_SNIPPETS[rng.nextInt(C_SNIPPETS.size)]
    // Pick the kind of bug first, so the common missing semicolon does not crowd out the rest
    val candidatesByBug = CSyntaxBug.entries.associateWith { bug ->
        snippet.lines.indices.mapNotNull { index -> breakLine(snippet.lines[index], bug)?.let { index to it } }
    }.filterValues { it.isNotEmpty() }
    val bug = candidatesByBug.keys.toList()[rng.nextInt(candidatesByBug.size)]
    val candidates = candidatesByBug.getValue(bug)
    val (line, broken) = candidates[rng.nextInt(candidates.size)]

    return CChallenge(
        number = number,
        type = CChallengeType.FIX_SYNTAX,
        codeLines = snippet.lines.toMutableList().apply { this[line] = broken },
        highlightLine = line,
        bug = bug,
        solutionLines = snippet.lines,
        solutionHighlight = setOf(line),
    )
}

private val KEYWORD_TYPOS = mapOf(
    "return" to "retrun",
    "while" to "whlie",
    "for" to "fro",
    "if" to "fi",
    "else" to "esle",
    "int" to "itn",
)
private val KEYWORD_REGEX = Regex("\\b(${KEYWORD_TYPOS.keys.joinToString("|")})\\b")

/** [line] with [bug] injected so it no longer compiles, or null when the bug does not fit the line. */
private fun breakLine(line: String, bug: CSyntaxBug): String? {
    val trimmed = line.trim()
    // Comments and preprocessor lines are never the culprit
    if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#")) return null

    return when (bug) {
        CSyntaxBug.MISSING_SEMICOLON ->
            if (line.endsWith(";")) line.dropLast(1) else null

        CSyntaxBug.MISSING_PARENTHESIS -> {
            val index = line.lastIndexOf(')')
            if (index >= 0) line.removeRange(index, index + 1) else null
        }

        CSyntaxBug.MISSING_QUOTE -> {
            val open = line.indexOf('"')
            val close = if (open >= 0) line.indexOf('"', open + 1) else -1
            if (close >= 0) line.removeRange(close, close + 1) else null
        }

        CSyntaxBug.KEYWORD_TYPO -> {
            val match = KEYWORD_REGEX.find(line) ?: return null
            line.replaceRange(match.range, KEYWORD_TYPOS.getValue(match.value))
        }

        CSyntaxBug.WRONG_BRACKET -> {
            val index = line.indexOf(']')
            if (index >= 0) line.replaceRange(index, index + 1, ")") else null
        }
    }
}

private fun fillBlank(number: Int, rng: SplitMix64): CChallenge {
    val snippet = C_SNIPPETS[rng.nextInt(C_SNIPPETS.size)]
    val blank = snippet.blanks[rng.nextInt(snippet.blanks.size)]
    val original = snippet.lines[blank.line]
    val indent = original.takeWhile { it == ' ' }
    val correct = original.trim()

    val options = (blank.distractors + correct).toMutableList()
    rng.shuffle(options)

    return CChallenge(
        number = number,
        type = CChallengeType.FILL_BLANK,
        codeLines = snippet.lines.toMutableList().apply { this[blank.line] = indent + BLANK_LINE },
        options = options,
        correctOption = options.indexOf(correct),
        highlightLine = blank.line,
        solutionLines = snippet.lines,
        solutionHighlight = setOf(blank.line),
    )
}

private fun predictOutput(number: Int, rng: SplitMix64): CChallenge {
    val puzzle = C_OUTPUT_TEMPLATES[rng.nextInt(C_OUTPUT_TEMPLATES.size)](rng)

    val wrong = puzzle.wrong.distinct().filter { it != puzzle.output }.toMutableList()
    rng.shuffle(wrong)
    val options = (wrong.take(OPTION_COUNT - 1) + puzzle.output).toMutableList()
    // Safety net for templates whose wrong answers collapsed onto each other
    val numeric = puzzle.output.toIntOrNull()
    var offset = 1
    while (numeric != null && options.size < OPTION_COUNT) {
        val candidate = (numeric + offset).toString()
        if (candidate !in options) options += candidate
        offset++
    }
    rng.shuffle(options)

    val program = listOf("#include <stdio.h>", "", "int main(void) {") +
        puzzle.body.map { "    $it" } +
        listOf("    return 0;", "}")

    return CChallenge(
        number = number,
        type = CChallengeType.PREDICT_OUTPUT,
        codeLines = program,
        options = options,
        correctOption = options.indexOf(puzzle.output),
        solutionLines = program,
    )
}

private fun orderLines(number: Int, rng: SplitMix64): CChallenge {
    val snippet = C_SNIPPETS[rng.nextInt(C_SNIPPETS.size)]
    val range = snippet.orderRange
    val block = snippet.lines.subList(range.first, range.last + 1)

    val pool = block.toMutableList()
    // A pool that happens to already be in order would be no puzzle at all
    for (attempt in 0 until 10) {
        rng.shuffle(pool)
        if (pool.map { it.trim() } != block.map { it.trim() }) break
    }

    return CChallenge(
        number = number,
        type = CChallengeType.ORDER_LINES,
        codeLines = snippet.lines.filterIndexed { index, _ -> index !in range },
        highlightLine = range.first,
        orderPool = pool,
        solutionLines = snippet.lines,
        solutionHighlight = range.toSet(),
    )
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.cchallenge

enum class CChallengeType {
    /** Tap the one line that does not compile. */
    FIX_SYNTAX,

    /** Pick the missing line of a function. */
    FILL_BLANK,

    /** Pick what the program prints. */
    PREDICT_OUTPUT,

    /** Put shuffled lines back into the right order. */
    ORDER_LINES,
}

/** The kinds of compile errors the syntax challenge injects; each one has its own explanation text. */
enum class CSyntaxBug {
    MISSING_SEMICOLON,
    MISSING_PARENTHESIS,
    MISSING_QUOTE,
    KEYWORD_TYPO,
    WRONG_BRACKET,
}

/**
 * One generated daily challenge. Everything is derived from the day, so it is never persisted:
 * restoring a run regenerates the same challenge and only the player's progress is saved.
 */
data class CChallenge(
    /** Running number of the daily challenge, shown as "#n". */
    val number: Int,
    val type: CChallengeType,
    /** Code shown while solving. FILL_BLANK has [BLANK_LINE] at [highlightLine], ORDER_LINES a gap there. */
    val codeLines: List<String>,
    /** Answer options of FILL_BLANK and PREDICT_OUTPUT. */
    val options: List<String> = emptyList(),
    val correctOption: Int = -1,
    /**
     * FIX_SYNTAX: the broken line the player has to tap. FILL_BLANK: the blanked line.
     * ORDER_LINES: where the ordered block is inserted into [codeLines].
     */
    val highlightLine: Int = -1,
    val bug: CSyntaxBug? = null,
    /** ORDER_LINES: the lines to order, shuffled, with their original indentation. */
    val orderPool: List<String> = emptyList(),
    /** Complete correct program, shown once the challenge is over. */
    val solutionLines: List<String> = emptyList(),
    /** Lines of [solutionLines] the player had to work out, highlighted in the solution. */
    val solutionHighlight: Set<Int> = emptySet(),
) {
    /** ORDER_LINES: the pool in its correct order, compared without indentation. */
    val orderSolution: List<String>
        get() = solutionLines.subList(highlightLine, highlightLine + orderPool.size).map { it.trim() }
}

const val BLANK_LINE = "____________"

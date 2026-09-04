package org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter

/**
 * One dart pad segment. `points` mirrors the actualScore rule already used in
 * DartCounterViewmodel.GameManager.subtractScore (base * multiplier), kept here so the
 * checkout solver and the pad agree on what a segment is worth.
 */
data class DartSegment(
    val base: Int,
    val multiplier: Int,
    val isDouble: Boolean,
    val isTriple: Boolean
) {
    val points: Int get() = base * multiplier

    companion object {
        val MISS = DartSegment(0, 1, isDouble = false, isTriple = false)
        val OUTER_BULL = DartSegment(25, 1, isDouble = false, isTriple = false)
        val BULLSEYE = DartSegment(25, 2, isDouble = true, isTriple = false)

        fun single(base: Int) = DartSegment(base, 1, isDouble = false, isTriple = false)
        fun double(base: Int) = DartSegment(base, 2, isDouble = true, isTriple = false)
        fun triple(base: Int) = DartSegment(base, 3, isDouble = false, isTriple = true)

        /** Scoring segments only (no Miss) - what a checkout dart can actually be. */
        val SCORING_SEGMENTS: List<DartSegment> by lazy {
            buildList {
                for (base in 20 downTo 1) {
                    add(triple(base))
                    add(double(base))
                }
                add(BULLSEYE)
                for (base in 20 downTo 1) add(single(base))
                add(OUTER_BULL)
            }.sortedByDescending { it.points }
        }
    }
}

/** Preferred finishing-double order darts players commonly favour; -1 marks the bullseye. */
private val DOUBLE_PREFERENCE = listOf(20, 16, 8, 4, 12, 10, 18, 2, -1, 14, 6, 11, 17, 3, 19, 7, 15, 1, 9, 13, 5)

private fun DartSegment.doublePreferenceRank(): Int {
    val key = if (base == 25 && isDouble) -1 else base
    val index = DOUBLE_PREFERENCE.indexOf(key)
    return if (index >= 0) index else DOUBLE_PREFERENCE.size
}

/** Max points reachable in [dartsLeft] darts; the last dart must be a double when [doubleOut]. */
fun maxReachable(dartsLeft: Int, doubleOut: Boolean): Int {
    if (dartsLeft <= 0) return 0
    if (!doubleOut) return dartsLeft * 60
    if (dartsLeft == 1) return 50
    return (dartsLeft - 1) * 60 + 50
}

private const val MAX_SEARCH_NODES = 20_000

/**
 * Best checkout paths to reduce [remaining] to exactly 0 within [dartsLeft] darts.
 * Under [doubleOut] the finishing dart of every path is a double (or the bullseye).
 * Returns an empty list when no checkout is possible.
 */
fun findCheckouts(remaining: Int, dartsLeft: Int, doubleOut: Boolean, maxResults: Int = 2): List<List<DartSegment>> {
    if (remaining <= 0 || dartsLeft <= 0) return emptyList()
    if (remaining > maxReachable(dartsLeft, doubleOut)) return emptyList()
    if (doubleOut && remaining == 1) return emptyList()

    val results = mutableListOf<List<DartSegment>>()
    val path = ArrayDeque<DartSegment>()
    var nodesVisited = 0
    val resultCap = maxResults * 25

    fun search(left: Int, dartsRemaining: Int) {
        if (results.size >= resultCap || nodesVisited >= MAX_SEARCH_NODES) return
        for (segment in DartSegment.SCORING_SEGMENTS) {
            nodesVisited++
            if (nodesVisited >= MAX_SEARCH_NODES) return

            val nextLeft = left - segment.points
            if (nextLeft < 0) continue

            if (nextLeft == 0) {
                if (doubleOut && !segment.isDouble) continue
                path.addLast(segment)
                results.add(path.toList())
                path.removeLast()
            } else if (dartsRemaining > 1) {
                if (doubleOut && nextLeft == 1) continue
                if (nextLeft > maxReachable(dartsRemaining - 1, doubleOut)) continue
                path.addLast(segment)
                search(nextLeft, dartsRemaining - 1)
                path.removeLast()
            }

            if (results.size >= resultCap) return
        }
    }

    search(remaining, dartsLeft)

    return results
        .distinct()
        .sortedWith(
            compareBy<List<DartSegment>> { it.size }
                .thenBy { if (doubleOut) it.last().doublePreferenceRank() else 0 }
                .thenByDescending { it.firstOrNull()?.points ?: 0 }
        )
        .take(maxResults)
}

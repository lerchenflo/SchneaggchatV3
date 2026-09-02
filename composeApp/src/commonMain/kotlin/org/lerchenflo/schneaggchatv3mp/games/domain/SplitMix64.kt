package org.lerchenflo.schneaggchatv3mp.games.domain

/**
 * splitmix64 — tiny explicit PRNG so daily content is identical on every
 * platform and Kotlin version (kotlin.random.Random does not guarantee that).
 * GridRush keeps its own private copy of the same algorithm.
 */
class SplitMix64(private var state: Long) {
    fun nextLong(): Long {
        state += -0x61c8864680b583ebL
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    fun nextInt(bound: Int): Int = ((nextLong() ushr 1) % bound).toInt()

    /** Random value in [from, until) */
    fun nextInt(from: Int, until: Int): Int = from + nextInt(until - from)

    /** Deterministic in-place Fisher-Yates shuffle. */
    fun <T> shuffle(list: MutableList<T>) {
        for (i in list.size - 1 downTo 1) {
            val j = nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.oddoneout

import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import kotlin.math.roundToInt
import kotlin.random.Random

private const val BASE_ROUND_SCORE = 100
private const val MIN_ROUND_SCORE = 20

/** Difficulty controls board size, how much time each round allows and how obvious the odd tile is. */
data class DifficultyConfig(
    val gridSize: Int,
    val startRoundTimeMillis: Long,
    val minRoundTimeMillis: Long,
    val roundTimeStepMillis: Long,
    val startOddDelta: Float,
    val minOddDelta: Float,
)

fun difficultyConfig(difficulty: GameDifficulty): DifficultyConfig = when (difficulty) {
    GameDifficulty.LOW -> DifficultyConfig(
        gridSize = 3,
        startRoundTimeMillis = 6000L,
        minRoundTimeMillis = 3200L,
        roundTimeStepMillis = 150L,
        startOddDelta = 0.42f,
        minOddDelta = 0.24f,
    )
    GameDifficulty.MEDIUM -> DifficultyConfig(
        gridSize = 4,
        startRoundTimeMillis = 4500L,
        minRoundTimeMillis = 2200L,
        roundTimeStepMillis = 150L,
        startOddDelta = 0.30f,
        minOddDelta = 0.14f,
    )
    GameDifficulty.HIGH -> DifficultyConfig(
        gridSize = 5,
        startRoundTimeMillis = 3500L,
        minRoundTimeMillis = 1500L,
        roundTimeStepMillis = 120L,
        startOddDelta = 0.22f,
        minOddDelta = 0.08f,
    )
}

/** Round time shrinks (down to a floor) as rounds progress, ramping up the pressure. */
fun roundTimeMillis(config: DifficultyConfig, round: Int): Long =
    (config.startRoundTimeMillis - config.roundTimeStepMillis * round).coerceAtLeast(config.minRoundTimeMillis)

/** The odd tile gets harder to spot (smaller visual delta) as rounds progress, down to a floor. */
fun oddDelta(config: DifficultyConfig, round: Int): Float {
    val roundsToFloor = 12
    val step = (config.startOddDelta - config.minOddDelta) / roundsToFloor
    return (config.startOddDelta - step * round).coerceAtLeast(config.minOddDelta)
}

data class GeneratedRound(
    val tiles: List<OddOneOutTile>,
    val oddIndex: Int,
    val variant: OddTileVariant,
    val lighten: Boolean,
    val delta: Float,
)

fun generateRound(config: DifficultyConfig, round: Int, random: Random): GeneratedRound {
    val count = config.gridSize * config.gridSize
    val palette = TilePalette.entries[random.nextInt(TilePalette.entries.size)]
    val oddIndex = random.nextInt(count)
    val variant = if (random.nextBoolean()) OddTileVariant.COLOR else OddTileVariant.SHAPE
    val tiles = List(count) { index -> OddOneOutTile(palette = palette, isOdd = index == oddIndex) }
    return GeneratedRound(
        tiles = tiles,
        oddIndex = oddIndex,
        variant = variant,
        lighten = random.nextBoolean(),
        delta = oddDelta(config, round),
    )
}

/** More points the faster the odd tile is found, relative to the time this round allowed. */
fun roundScore(reactionMillis: Long, roundTimeMillis: Long): Int {
    val speedFraction = (1f - reactionMillis.toFloat() / roundTimeMillis.toFloat()).coerceIn(0f, 1f)
    return (MIN_ROUND_SCORE + (BASE_ROUND_SCORE - MIN_ROUND_SCORE) * speedFraction).roundToInt()
}

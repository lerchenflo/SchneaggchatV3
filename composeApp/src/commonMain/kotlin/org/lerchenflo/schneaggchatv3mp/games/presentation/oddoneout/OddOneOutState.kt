package org.lerchenflo.schneaggchatv3mp.games.presentation.oddoneout

const val ODDONEOUT_STARTING_LIVES = 3

sealed interface OddOneOutAction {
    data object StartGame : OddOneOutAction
    data object StopGame : OddOneOutAction
    data object RestartGame : OddOneOutAction
    data class OnTileTapped(val index: Int) : OddOneOutAction
}

/** Which trait makes the odd tile stand out this round. */
enum class OddTileVariant { COLOR, SHAPE }

/** Logical tile palette; the screen maps these to actual render colors. */
enum class TilePalette { A, B, C, D, E }

data class OddOneOutTile(
    val palette: TilePalette,
    val isOdd: Boolean,
)

data class OddOneOutState(
    val isPlaying: Boolean = false,
    val isGameOver: Boolean = false,
    val score: Int = 0,
    val lives: Int = ODDONEOUT_STARTING_LIVES,
    val round: Int = 0,
    val gridSize: Int = 3,
    val tiles: List<OddOneOutTile> = emptyList(),
    val oddIndex: Int = -1,
    val variant: OddTileVariant = OddTileVariant.COLOR,
    /** Whether the odd tile is a lightened or darkened version of the palette color (COLOR variant only) */
    val oddLighten: Boolean = true,
    /** 0f..1f visual strength of the odd tile's difference — bigger is easier to spot */
    val oddDelta: Float = 0.3f,
    val roundTimeMillis: Long = 4000L,
    val roundTimeRemainingMillis: Long = 4000L,
    val elapsedMillis: Long = 0L,
    /** Tile index most recently tapped incorrectly; briefly highlighted red, cleared on the next round */
    val wrongIndex: Int? = null,
)

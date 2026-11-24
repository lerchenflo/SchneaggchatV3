package org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow

import kotlinx.serialization.Serializable

const val FOUR_IN_A_ROW_MIN_PLAYERS = 2
const val FOUR_IN_A_ROW_MAX_PLAYERS = 4
const val FOUR_IN_A_ROW_WIN_LENGTH = 4

/** Move timer lengths the setup offers, in seconds. */
val FOUR_IN_A_ROW_MOVE_SECONDS = listOf(10, 20, 30)

/** Series lengths the setup offers; null in the setup means an open match without a series. */
val FOUR_IN_A_ROW_BEST_OF = listOf(3, 5)

/**
 * Rules of one match. More players get a wider and higher board so there is still room to block;
 * four in a row always wins. The twists can be combined freely.
 */
@Serializable
data class FourInARowConfig(
    val playerCount: Int,
    val teams: Boolean = false,
    val powerDiscs: Boolean = false,
    val boardTwist: Boolean = false,
    val popOut: Boolean = false,
    /** Move timer in seconds, null without a timer. */
    val moveSeconds: Int? = null,
) {
    val columns: Int
        get() = when {
            playerCount <= 2 -> 7
            playerCount == 3 -> 9
            else -> 10
        }

    val rows: Int
        get() = when {
            playerCount <= 2 -> 6
            playerCount == 3 -> 7
            else -> 8
        }

    /** Teams only exist with four players: 1st + 3rd against 2nd + 4th, so the turns alternate between them. */
    val hasTeams: Boolean get() = teams && playerCount == 4

    /** Who can win: every player alone, or the two teams. */
    val sideCount: Int get() = if (hasTeams) 2 else playerCount

    fun sideOf(player: Int): Int = if (hasTeams) player % 2 else player

    /** Board twist: the board twists after every this many turns (three full rounds). */
    val turnsPerTwist: Int get() = playerCount * 3
}

/** Row 0 is the top row, column 0 the left column. */
@Serializable
data class GridCell(val row: Int, val column: Int)

/** Once per round, every player may drop each of these instead of a normal disc. */
@Serializable
enum class PowerDisc {
    /** Explodes where it lands and clears every disc around it. */
    BOMB,

    /** Smashes through its column, clearing it, and stays at the bottom as a normal disc. */
    ANVIL,

    /** Counts for every side's line. */
    JOKER,
}

/** [id] grows with every dropped disc, so the board can follow a disc while it falls or slides. */
@Serializable
data class Disc(
    val id: Long,
    val owner: Int,
    val cell: GridCell,
    val joker: Boolean = false,
)

@Serializable
data class UsedPower(val player: Int, val power: PowerDisc)

@Serializable
enum class TwistKind {
    /** The board turns upside down: every column's discs land in reverse order. */
    FLIP,

    /** One row slides a cell sideways (wrapping around); discs above gaps fall. */
    ROW_SLIDE,
}

@Serializable
data class BoardTwist(
    val kind: TwistKind,
    val row: Int = 0,
    val forward: Boolean = true,
)

/** One game on the board, from the first disc to a win or a draw. Players are referenced by index. */
@Serializable
data class FourInARowRound(
    val config: FourInARowConfig,
    val startingPlayer: Int,
    val currentPlayer: Int = startingPlayer,
    val discs: List<Disc> = emptyList(),
    val turnCount: Int = 0,
    val nextDiscId: Long = 0,
    val usedPowers: Set<UsedPower> = emptySet(),
    /** The winning player, or the winning team with teams (see [FourInARowConfig.sideOf]). */
    val winnerSide: Int? = null,
    val winLine: List<GridCell> = emptyList(),
    val draw: Boolean = false,
    /** What the last turn did, so the board can animate it. */
    val lastDroppedId: Long? = null,
    val lastBlast: GridCell? = null,
    val lastAnvilColumn: Int? = null,
    val lastTwist: BoardTwist? = null,
    /** The player whose move timer ran out last turn. */
    val lastSkippedPlayer: Int? = null,
) {
    val isOver: Boolean get() = winnerSide != null || draw

    fun discAt(cell: GridCell): Disc? = discs.firstOrNull { it.cell == cell }

    fun columnHeight(column: Int): Int = discs.count { it.cell.column == column }

    fun isColumnFull(column: Int): Boolean = columnHeight(column) >= config.rows

    fun canUsePower(player: Int, power: PowerDisc): Boolean =
        config.powerDiscs && !isOver && UsedPower(player, power) !in usedPowers

    /** Pop-out: the bottom disc of [column] belongs to [player]'s side (jokers belong to nobody). */
    fun canPopOut(player: Int, column: Int): Boolean {
        if (!config.popOut || isOver) return false
        val bottom = discAt(GridCell(config.rows - 1, column)) ?: return false
        return !bottom.joker && config.sideOf(bottom.owner) == config.sideOf(player)
    }

    fun canPopAny(player: Int): Boolean = (0 until config.columns).any { canPopOut(player, it) }

    /** Board twist: turns left until the board twists, null without the twist. */
    val turnsUntilTwist: Int?
        get() = if (config.boardTwist) config.turnsPerTwist - turnCount % config.turnsPerTwist else null

    /** The same board without the last turn's one-shot effects, for a round shown again (restore, undo). */
    fun withoutLastTurnEffects(): FourInARowRound = copy(
        lastDroppedId = null,
        lastBlast = null,
        lastAnvilColumn = null,
        lastTwist = null,
        lastSkippedPlayer = null,
    )
}

sealed interface FourInARowMove {
    /** A disc into [column]; with [power] one of the player's power discs instead. */
    data class Drop(val column: Int, val power: PowerDisc? = null) : FourInARowMove

    /** Pop-out: takes the player's own disc out of the bottom of [column]. */
    data class PopOut(val column: Int) : FourInARowMove

    /** The move timer ran out. */
    data object Skip : FourInARowMove
}

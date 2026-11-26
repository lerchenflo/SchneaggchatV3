package org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow

import kotlin.math.abs
import kotlin.random.Random

/**
 * Pure rules: no Compose, coroutines or clock. Randomness (board twist) comes in through [random].
 */
class FourInARowEngine(private val random: Random = Random.Default) {

    fun newRound(config: FourInARowConfig, startingPlayer: Int): FourInARowRound =
        FourInARowRound(config = config, startingPlayer = startingPlayer)

    /** The round after [move]; returns [round] unchanged when the move is not allowed. */
    fun apply(round: FourInARowRound, move: FourInARowMove): FourInARowRound {
        if (round.isOver) return round
        val config = round.config
        val mover = round.currentPlayer

        val turn = when (move) {
            is FourInARowMove.Drop -> drop(round, mover, move) ?: return round
            is FourInARowMove.PopOut -> popOut(round, mover, move.column) ?: return round
            FourInARowMove.Skip -> TurnResult(discs = round.discs, nextDiscId = round.nextDiscId)
        }
        var discs = settle(turn.discs, config.rows)

        val turnCount = round.turnCount + 1
        var twist: BoardTwist? = null
        if (config.boardTwist && turnCount % config.turnsPerTwist == 0 && discs.isNotEmpty()) {
            twist = randomTwist(discs)
            discs = settle(twisted(discs, twist, config), config.rows)
        }

        val (winnerSide, winLine) = decideWinner(discs, config, mover)
        val usedPowers = turn.power?.let { round.usedPowers + UsedPower(mover, it) } ?: round.usedPowers
        val afterMove = round.copy(
            discs = discs,
            turnCount = turnCount,
            nextDiscId = turn.nextDiscId,
            usedPowers = usedPowers,
            winnerSide = winnerSide,
            winLine = winLine,
            lastDroppedId = turn.droppedId,
            lastBlast = turn.blast,
            lastAnvilColumn = turn.anvilColumn,
            lastTwist = twist,
            lastSkippedPlayer = mover.takeIf { move == FourInARowMove.Skip },
        )
        if (winnerSide != null) return afterMove

        // A player without any move is passed; nobody able to move is a draw
        val nextPlayer = (1..config.playerCount)
            .map { (mover + it) % config.playerCount }
            .firstOrNull { hasMove(afterMove, it) }
        return if (nextPlayer == null) afterMove.copy(draw = true) else afterMove.copy(currentPlayer = nextPlayer)
    }

    /** Loser starts: the first player after the last mover who is not on the winning side; after a draw the next one. */
    fun nextStartingPlayer(round: FourInARowRound): Int {
        val playerCount = round.config.playerCount
        val winnerSide = round.winnerSide ?: return (round.startingPlayer + 1) % playerCount
        return (1..playerCount)
            .map { (round.currentPlayer + it) % playerCount }
            .first { round.config.sideOf(it) != winnerSide }
    }

    private class TurnResult(
        val discs: List<Disc>,
        val nextDiscId: Long,
        val power: PowerDisc? = null,
        val droppedId: Long? = null,
        val blast: GridCell? = null,
        val anvilColumn: Int? = null,
    )

    private fun drop(round: FourInARowRound, mover: Int, move: FourInARowMove.Drop): TurnResult? {
        val config = round.config
        val column = move.column
        if (column !in 0 until config.columns) return null
        val power = move.power
        if (power != null && !round.canUsePower(mover, power)) return null

        // The anvil clears its column, so it also fits into a full one
        if (power == PowerDisc.ANVIL) {
            val id = round.nextDiscId
            val anvil = Disc(id = id, owner = mover, cell = GridCell(config.rows - 1, column))
            return TurnResult(
                discs = round.discs.filter { it.cell.column != column } + anvil,
                nextDiscId = id + 1,
                power = power,
                droppedId = id,
                anvilColumn = column,
            )
        }

        if (round.isColumnFull(column)) return null
        val landing = GridCell(config.rows - 1 - round.columnHeight(column), column)
        if (power == PowerDisc.BOMB) {
            return TurnResult(
                discs = round.discs.filterNot { it.cell.touches(landing) },
                nextDiscId = round.nextDiscId,
                power = power,
                blast = landing,
            )
        }
        val id = round.nextDiscId
        return TurnResult(
            discs = round.discs + Disc(id = id, owner = mover, cell = landing, joker = power == PowerDisc.JOKER),
            nextDiscId = id + 1,
            power = power,
            droppedId = id,
        )
    }

    private fun popOut(round: FourInARowRound, mover: Int, column: Int): TurnResult? {
        if (!round.canPopOut(mover, column)) return null
        val bottom = GridCell(round.config.rows - 1, column)
        return TurnResult(discs = round.discs.filterNot { it.cell == bottom }, nextDiscId = round.nextDiscId)
    }

    private fun hasMove(round: FourInARowRound, player: Int): Boolean =
        (0 until round.config.columns).any { !round.isColumnFull(it) } ||
            round.canPopAny(player) ||
            round.canUsePower(player, PowerDisc.ANVIL)

    private fun GridCell.touches(other: GridCell): Boolean =
        abs(row - other.row) <= 1 && abs(column - other.column) <= 1

    /**
     * The mover's side wins with a line of its own. A line that only appeared through a fall,
     * a pop-out or a twist still counts: then the first side after the mover (in turn order) that has one wins.
     */
    private fun decideWinner(discs: List<Disc>, config: FourInARowConfig, mover: Int): Pair<Int?, List<GridCell>> {
        val sidesInTurnOrder = (0 until config.playerCount)
            .map { config.sideOf((mover + it) % config.playerCount) }
            .distinct()
        for (side in sidesInTurnOrder) {
            val line = findLine(discs, side, config)
            if (line != null) return side to line
        }
        return null to emptyList()
    }

    private fun findLine(discs: List<Disc>, side: Int, config: FourInARowConfig): List<GridCell>? {
        val counting = discs.filter { it.joker || config.sideOf(it.owner) == side }.map { it.cell }.toSet()
        if (counting.size < FOUR_IN_A_ROW_WIN_LENGTH) return null
        // A line of jokers alone belongs to nobody; it needs at least one disc of the side
        val own = discs.filter { !it.joker && config.sideOf(it.owner) == side }.map { it.cell }.toSet()
        val directions = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        for (start in counting) {
            for ((rowStep, columnStep) in directions) {
                val line = (0 until FOUR_IN_A_ROW_WIN_LENGTH).map { step ->
                    GridCell(start.row + rowStep * step, start.column + columnStep * step)
                }
                if (line.all { it in counting } && line.any { it in own }) return line
            }
        }
        return null
    }

    /** Every disc falls to the bottom of its column, keeping the order within the column. */
    private fun settle(discs: List<Disc>, rows: Int): List<Disc> =
        discs.groupBy { it.cell.column }.flatMap { (column, columnDiscs) ->
            columnDiscs
                .sortedByDescending { it.cell.row }
                .mapIndexed { index, disc -> disc.copy(cell = GridCell(rows - 1 - index, column)) }
        }

    private fun randomTwist(discs: List<Disc>): BoardTwist {
        if (random.nextBoolean()) return BoardTwist(kind = TwistKind.FLIP)
        val usedRows = discs.map { it.cell.row }.distinct()
        return BoardTwist(kind = TwistKind.ROW_SLIDE, row = usedRows.random(random), forward = random.nextBoolean())
    }

    private fun twisted(discs: List<Disc>, twist: BoardTwist, config: FourInARowConfig): List<Disc> =
        when (twist.kind) {
            // Mirrored rows; settle() then lets them land in reverse order
            TwistKind.FLIP -> discs.map { it.copy(cell = it.cell.copy(row = config.rows - 1 - it.cell.row)) }
            TwistKind.ROW_SLIDE -> {
                val step = if (twist.forward) 1 else config.columns - 1
                discs.map { disc ->
                    if (disc.cell.row != twist.row) {
                        disc
                    } else {
                        disc.copy(cell = disc.cell.copy(column = (disc.cell.column + step) % config.columns))
                    }
                }
            }
        }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import androidx.compose.ui.graphics.Color
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import kotlin.random.Random

data class SchneaggaHusMap(
    val gridWidth: Int,
    val gridHeight: Int,
    val spawn: Position,
    val trackList: List<TrackTile>,
    val houseList: List<Schneaggahus>,
)

/** Identity colors shared by houses and schneaggs; the theme has no eight distinguishable hues. */
internal val HOUSE_COLORS = listOf(
    Color(0xFFEF5350), // red
    Color(0xFF42A5F5), // blue
    Color(0xFF66BB6A), // green
    Color(0xFFFFCA28), // yellow
    Color(0xFFAB47BC), // purple
    Color(0xFFFF7043), // orange
    Color(0xFF26C6DA), // cyan
    Color(0xFFEC407A), // pink
)

/** Portrait grids (width × height): phones are tall, and the tree grows downwards from the tunnel. */
internal fun gridSize(difficulty: GameDifficulty): Pair<Int, Int> = when (difficulty) {
    GameDifficulty.LOW -> 6 to 9
    GameDifficulty.MEDIUM -> 7 to 11
    GameDifficulty.HIGH -> 8 to 13
}

internal fun baseHouseCount(difficulty: GameDifficulty): Int = when (difficulty) {
    GameDifficulty.LOW -> 3
    GameDifficulty.MEDIUM -> 4
    GameDifficulty.HIGH -> 6
}

/** Denser trees stop fitting these grids; above this the generator would mostly fail. */
internal fun maxHouseCount(difficulty: GameDifficulty): Int = when (difficulty) {
    GameDifficulty.LOW -> 5
    GameDifficulty.MEDIUM -> 6
    GameDifficulty.HIGH -> 7
}

private const val ATTEMPTS_PER_HOUSE_COUNT = 20
private const val CARVE_BUDGET = 3000
private const val STRAIGHT_WEIGHT = 2f
private const val SOUTH_WEIGHT = 1.5f
/** Cells next to other track are still allowed, just rarely picked, so layouts stay readable. */
private const val CROWDED_WEIGHT = 0.25f

/**
 * Generates a random track tree with [houseCount] houses for one wave. The tunnel
 * sits in the top row, every branch heads east, south or west, and each leaf is a
 * house. Should a tree that dense not fit, the house count is stepped down.
 */
fun generateSchneaggaHusMap(
    difficulty: GameDifficulty,
    houseCount: Int = baseHouseCount(difficulty),
    random: Random = Random.Default,
): SchneaggaHusMap {
    val (width, height) = gridSize(difficulty)
    val requested = houseCount.coerceIn(2, HOUSE_COLORS.size)
    for (houses in requested downTo 2) {
        repeat(ATTEMPTS_PER_HOUSE_COUNT) {
            MapBuilder(width, height, houses, random).build()?.let { return it }
        }
    }
    return fallbackMap(width, height)
}

/** Straight tunnel-to-house line — only used if generation somehow never succeeds. */
private fun fallbackMap(width: Int, height: Int): SchneaggaHusMap {
    val spawn = Position(width / 2, 0)
    val tracks = (0 until height - 1).map { y ->
        TrackTile(Position(spawn.x, y), DIRECTION.NORTH, listOf(DIRECTION.SOUTH))
    }
    return SchneaggaHusMap(
        gridWidth = width,
        gridHeight = height,
        spawn = spawn,
        trackList = tracks,
        houseList = listOf(Schneaggahus(Position(spawn.x, height - 1), HOUSE_COLORS.first())),
    )
}

private class MapBuilder(
    private val width: Int,
    private val height: Int,
    private val houseCount: Int,
    private val random: Random,
) {
    private val occupied = HashSet<Position>()
    private val tracks = LinkedHashMap<Position, TrackTile>()
    private val housePositions = mutableListOf<Position>()
    private var carveCalls = 0

    fun build(): SchneaggaHusMap? {
        // The top row belongs to the tunnel and the next-up preview, nothing else is carved there
        for (x in 0 until width) occupied += Position(x, 0)
        val spawn = Position(random.nextInt(1, width - 1), 0)
        tracks[spawn] = TrackTile(spawn, DIRECTION.NORTH, listOf(DIRECTION.SOUTH))

        carve(spawn, DIRECTION.SOUTH, houseCount, remainingSteps = random.nextInt(2, 4)) ?: return null

        val colors = HOUSE_COLORS.shuffled(random)
        return SchneaggaHusMap(
            gridWidth = width,
            gridHeight = height,
            spawn = spawn,
            trackList = tracks.values.toList(),
            houseList = housePositions.mapIndexed { index, position ->
                Schneaggahus(position, colors[index])
            },
        )
    }

    /**
     * Claims the cell next to [from] in [heading] and continues the walk:
     * plain track while [remainingSteps] is left, then either a house (subtree
     * done) or a switch splitting [housesToPlace] onto two branches.
     * Returns the cells of the finished subtree, or null after releasing them
     * again so the caller can try another direction (backtracking).
     */
    private fun carve(from: Position, heading: DIRECTION, housesToPlace: Int, remainingSteps: Int): List<Position>? {
        if (++carveCalls > CARVE_BUDGET) return null
        val cell = from.step(heading)
        if (!isFree(cell)) return null
        val entry = heading.opposite()
        occupied += cell

        if (remainingSteps > 0) {
            for (next in weightedOrder(headingOptions(cell, heading), cell, straight = heading)) {
                val subtree = carve(cell, next, housesToPlace, remainingSteps - 1) ?: continue
                tracks[cell] = TrackTile(cell, entry, listOf(next))
                return subtree + cell
            }
            occupied -= cell
            return null
        }

        if (housesToPlace == 1) {
            // A house stands clear of everything except its own rail
            if (occupiedNeighbours(cell, except = from) > 0) {
                occupied -= cell
                return null
            }
            housePositions += cell
            return listOf(cell)
        }

        // Switch: split the remaining houses roughly in half onto two free directions
        val exits = weightedOrder(exitOptions(cell, entry), cell, straight = null)
        for (i in exits.indices) {
            for (j in i + 1 until exits.size) {
                val firstHouses = (housesToPlace + random.nextInt(0, 2)) / 2
                val first = carve(cell, exits[i], firstHouses, newSegmentLength()) ?: continue
                val second = carve(cell, exits[j], housesToPlace - firstHouses, newSegmentLength())
                if (second == null) {
                    release(first)
                    continue
                }
                tracks[cell] = TrackTile(cell, entry, listOf(exits[i], exits[j]))
                return first + second + cell
            }
        }
        occupied -= cell
        return null
    }

    /** Track tiles between a branch start and the next switch or house. */
    private fun newSegmentLength(): Int = random.nextInt(1, 4)

    /** Straight on or a turn, never back up towards the tunnel. */
    private fun headingOptions(cell: Position, heading: DIRECTION): List<DIRECTION> =
        listOf(heading, heading.turnLeft(), heading.turnRight())
            .filter { it != DIRECTION.NORTH && isFree(cell.step(it)) }

    private fun exitOptions(cell: Position, entry: DIRECTION): List<DIRECTION> =
        DIRECTION.entries.filter { it != entry && it != DIRECTION.NORTH && isFree(cell.step(it)) }

    /**
     * Random order of [options], drawn without replacement by weight: going
     * straight and going south are preferred so tracks read as a tree flowing
     * downwards, crowded cells are avoided when possible.
     */
    private fun weightedOrder(options: List<DIRECTION>, cell: Position, straight: DIRECTION?): List<DIRECTION> {
        val remaining = options.toMutableList()
        val weights = remaining.map { direction ->
            var weight = 1f
            if (direction == straight) weight *= STRAIGHT_WEIGHT
            if (direction == DIRECTION.SOUTH) weight *= SOUTH_WEIGHT
            if (occupiedNeighbours(cell.step(direction), except = cell) > 0) weight *= CROWDED_WEIGHT
            weight
        }.toMutableList()

        val ordered = ArrayList<DIRECTION>(remaining.size)
        while (remaining.isNotEmpty()) {
            var pick = random.nextFloat() * weights.sum()
            var index = 0
            while (index < weights.size - 1 && pick >= weights[index]) {
                pick -= weights[index]
                index++
            }
            ordered += remaining.removeAt(index)
            weights.removeAt(index)
        }
        return ordered
    }

    private fun release(cells: List<Position>) {
        occupied -= cells.toSet()
        cells.forEach {
            tracks.remove(it)
            housePositions.remove(it)
        }
    }

    /** Occupied 4-neighbours of [cell] other than [except]; the reserved top row does not count. */
    private fun occupiedNeighbours(cell: Position, except: Position?): Int =
        DIRECTION.entries.count { direction ->
            val neighbour = cell.step(direction)
            neighbour != except && neighbour.y > 0 && neighbour in occupied
        }

    private fun isFree(position: Position): Boolean {
        return position.x in 0 until width &&
                position.y in 0 until height &&
                position !in occupied
    }
}

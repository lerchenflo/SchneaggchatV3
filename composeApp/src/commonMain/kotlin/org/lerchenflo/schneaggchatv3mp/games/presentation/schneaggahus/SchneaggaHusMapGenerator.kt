package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import androidx.compose.ui.graphics.Color
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import kotlin.random.Random

data class SchneaggaHusMap(
    val gridWidth: Int,
    val gridHeight: Int,
    val spawn: Position,
    val firstTrack: Position,
    val trackList: List<TrackTile>,
    val houseList: List<Schneaggahus>,
)

private val HOUSE_COLORS = listOf(
    Color(0xFFEF5350), // red
    Color(0xFF42A5F5), // blue
    Color(0xFF66BB6A), // green
    Color(0xFFFFCA28), // yellow
    Color(0xFFAB47BC), // purple
    Color(0xFFFF7043), // orange
    Color(0xFF26C6DA), // cyan
    Color(0xFFEC407A), // pink
)

/**
 * Generates a random track tree for one run. Higher difficulty means a bigger
 * grid with more houses (and therefore more switches to keep an eye on).
 */
fun generateSchneaggaHusMap(
    difficulty: GameDifficulty,
    random: Random = Random.Default,
): SchneaggaHusMap {
    val (width, height, houseCount) = when (difficulty) {
        GameDifficulty.LOW -> Triple(8, 6, 3)
        GameDifficulty.MEDIUM -> Triple(11, 8, 4)
        GameDifficulty.HIGH -> Triple(14, 10, 6)
    }

    // Random walks can dead-end; retry until a full tree fits (practically instant)
    repeat(200) {
        val map = MapBuilder(width, height, houseCount, random).build()
        if (map != null) return map
    }
    return fallbackMap(width, height)
}

/** Straight spawn-to-house line — only used if generation somehow never succeeds. */
private fun fallbackMap(width: Int, height: Int): SchneaggaHusMap {
    val spawn = Position(width / 2, 0)
    val tracks = (1 until height - 1).map { y ->
        TrackTile(Position(spawn.x, y), DIRECTION.NORTH, listOf(DIRECTION.SOUTH))
    }
    return SchneaggaHusMap(
        gridWidth = width,
        gridHeight = height,
        spawn = spawn,
        firstTrack = Position(spawn.x, 1),
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
    private val tracks = mutableListOf<TrackTile>()
    private val housePositions = mutableListOf<Position>()

    fun build(): SchneaggaHusMap? {
        val spawn = Position(random.nextInt(1, width - 1), 0)
        occupied += spawn

        if (!carve(spawn, DIRECTION.SOUTH, houseCount, remainingSteps = random.nextInt(2, 4))) return null

        val colors = HOUSE_COLORS.shuffled(random)
        return SchneaggaHusMap(
            gridWidth = width,
            gridHeight = height,
            spawn = spawn,
            firstTrack = spawn.step(DIRECTION.SOUTH),
            trackList = tracks,
            houseList = housePositions.mapIndexed { index, position ->
                Schneaggahus(position, colors[index])
            },
        )
    }

    /**
     * Claims the cell next to [from] in [heading] and continues the walk:
     * plain track while [remainingSteps] is left, then either a house (subtree
     * done) or a switch splitting [housesToPlace] onto two branches.
     * Returns false when the walk got stuck; the whole attempt is retried then.
     */
    private fun carve(from: Position, heading: DIRECTION, housesToPlace: Int, remainingSteps: Int): Boolean {
        val cell = from.step(heading)
        if (!isFree(cell)) return false
        occupied += cell
        val entry = heading.opposite()

        if (remainingSteps > 0) {
            val nextHeading = chooseHeading(cell, heading) ?: return false
            tracks += TrackTile(cell, entry, listOf(nextHeading))
            return carve(cell, nextHeading, housesToPlace, remainingSteps - 1)
        }

        if (housesToPlace == 1) {
            housePositions += cell
            return true
        }

        // Switch: split the remaining houses roughly in half onto two free directions
        val options = DIRECTION.entries.filter { it != entry && isFree(cell.step(it)) }.shuffled(random)
        if (options.size < 2) return false
        tracks += TrackTile(cell, entry, listOf(options[0], options[1]))

        val firstHouses = (housesToPlace + random.nextInt(0, 2)) / 2
        return carve(cell, options[0], firstHouses, newSegmentLength()) &&
                carve(cell, options[1], housesToPlace - firstHouses, newSegmentLength())
    }

    /** Track tiles between a branch start and the next switch or house. */
    private fun newSegmentLength(): Int = random.nextInt(1, 4)

    /** Mostly keeps going straight so tracks do not zigzag wildly. */
    private fun chooseHeading(cell: Position, heading: DIRECTION): DIRECTION? {
        val candidates = listOf(heading, heading.turnLeft(), heading.turnRight())
            .filter { isFree(cell.step(it)) }
        if (candidates.isEmpty()) return null
        return if (heading in candidates && random.nextFloat() < 0.6f) heading else candidates.random(random)
    }

    private fun isFree(position: Position): Boolean {
        return position.x in 0 until width &&
                position.y in 0 until height &&
                position !in occupied
    }
}

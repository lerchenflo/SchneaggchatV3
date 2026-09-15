package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus.DIRECTION.EAST
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus.DIRECTION.NORTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus.DIRECTION.SOUTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus.DIRECTION.WEST

/**
 * Deterministic mid-game state for previews: a hand-built 7×11 map with straights,
 * switches set both ways and turns in every orientation, schneaggs on a straight,
 * mid-arc, heading west and emerging from the tunnel, plus one correct and one
 * wrong delivery pulse in flight. [showBanner] puts the run right after a wave start.
 */
internal fun previewSchneaggaHusState(showBanner: Boolean = false): SchneaggaHusState {
    val spawn = Position(3, 0)
    fun straight(x: Int, y: Int, entry: DIRECTION = NORTH) =
        TrackTile(Position(x, y), entry, listOf(entry.opposite()))
    fun turn(x: Int, y: Int, entry: DIRECTION, exit: DIRECTION) =
        TrackTile(Position(x, y), entry, listOf(exit))
    fun switch(x: Int, y: Int, entry: DIRECTION, exits: List<DIRECTION>, active: Int) =
        TrackTile(Position(x, y), entry, exits, active)

    val red = HOUSE_COLORS[0]
    val blue = HOUSE_COLORS[1]
    val green = HOUSE_COLORS[2]
    val yellow = HOUSE_COLORS[3]

    val tracks = listOf(
        straight(3, 0), straight(3, 1), straight(3, 2),
        switch(3, 3, NORTH, listOf(WEST, EAST), active = 1),
        // East branch
        turn(4, 3, WEST, SOUTH), straight(4, 4),
        switch(4, 5, NORTH, listOf(SOUTH, EAST), active = 0),
        straight(4, 6),
        straight(5, 5, entry = WEST), turn(6, 5, WEST, SOUTH),
        // West branch
        straight(2, 3, entry = EAST), turn(1, 3, EAST, SOUTH), straight(1, 4),
        switch(1, 5, NORTH, listOf(SOUTH, EAST), active = 1),
        straight(1, 6),
        straight(2, 5, entry = WEST), turn(3, 5, WEST, SOUTH), straight(3, 6),
    )
    val houses = listOf(
        Schneaggahus(Position(4, 7), red),
        Schneaggahus(Position(6, 6), blue),
        Schneaggahus(Position(1, 7), green),
        Schneaggahus(Position(3, 7), yellow),
    )
    val elapsed = 47_300L
    val schneaggs = listOf(
        Schneagg(0, red, Position(3, 1), NORTH, SOUTH, 0.6f),
        Schneagg(1, blue, Position(4, 3), WEST, SOUTH, 0.5f),
        Schneagg(2, green, Position(1, 3), EAST, SOUTH, 0.25f),
        Schneagg(3, yellow, Position(3, 5), WEST, SOUTH, 0.9f),
        Schneagg(4, red, spawn, NORTH, SOUTH, 0.3f),
    )

    return SchneaggaHusState(
        isPlaying = true,
        score = 140,
        lives = 2,
        elapsedMillis = elapsed,
        gridWidth = 7,
        gridHeight = 11,
        spawn = spawn,
        schneaggList = schneaggs,
        schneagghusList = houses,
        trackList = tracks,
        wave = 3,
        waveSnailTotal = 12,
        waveDelivered = 5,
        upcoming = listOf(blue, green, red),
        waveStartedAtElapsed = if (showBanner) elapsed - 400 else elapsed - 10_000,
        feedback = listOf(
            DeliveryFeedback(Position(6, 6), correct = true, points = 30, atElapsedMillis = elapsed - 300),
            DeliveryFeedback(Position(1, 7), correct = false, points = 0, atElapsedMillis = elapsed - 200),
        ),
    )
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable

/** Version 2: per-tile schneagg movement and waves; older saves are discarded. */
const val SCHNEAGGAHUS_SNAPSHOT_VERSION = 2

/** A travelling schneagg with its color stored as ARGB, since Compose [Color] is not serializable. */
@Serializable
data class SchneaggSnapshot(
    val id: Int,
    val colorArgb: Int,
    val tile: Position,
    val entry: DIRECTION,
    val exit: DIRECTION,
    val progress: Float,
)

@Serializable
data class HouseSnapshot(
    val position: Position,
    val colorArgb: Int,
)

/**
 * Persisted mid-run state. The map is generated per wave, so the whole track
 * layout (including switch positions), the houses and the pre-rolled color
 * queue are stored too. All timers are run time (ms), never wall-clock.
 */
@Serializable
data class SchneaggaHusSnapshot(
    val gridWidth: Int,
    val gridHeight: Int,
    val spawn: Position,
    val trackList: List<TrackTile>,
    val houses: List<HouseSnapshot>,
    val schneaggs: List<SchneaggSnapshot>,
    val score: Int,
    val lives: Int,
    val elapsedMillis: Long,
    val nextSchneaggId: Int,
    /** Run time (ms) at which the next schneagg is due, so resuming does not spawn one instantly. */
    val nextSpawnAtElapsed: Long,
    /** Spawn gap multiplier from the player's recent performance (1 = base gap). */
    val pace: Float = 1f,
    val wave: Int,
    val waveSnailTotal: Int,
    val waveDelivered: Int,
    val upcomingArgb: List<Int>,
    val waveStartedAtElapsed: Long,
    val intermissionUntilElapsed: Long?,
)

fun Schneagg.toSnapshot() = SchneaggSnapshot(
    id = id,
    colorArgb = color.toArgb(),
    tile = tile,
    entry = entry,
    exit = exit,
    progress = progress,
)

fun SchneaggSnapshot.toSchneagg() = Schneagg(
    id = id,
    color = Color(colorArgb),
    tile = tile,
    entry = entry,
    exit = exit,
    progress = progress,
)

fun Schneaggahus.toSnapshot() = HouseSnapshot(position = position, colorArgb = color.toArgb())

fun HouseSnapshot.toHouse() = Schneaggahus(position = position, color = Color(colorArgb))

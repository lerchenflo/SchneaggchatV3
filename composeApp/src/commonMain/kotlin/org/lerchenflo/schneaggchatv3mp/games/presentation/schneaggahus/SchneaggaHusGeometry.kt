package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * A point on the track in tile units (a tile spans 1×1 with its origin in the
 * top-left corner) plus the travel heading in degrees, 0° = east, clockwise.
 */
data class TrackPoint(val x: Float, val y: Float, val headingDeg: Float)

/**
 * Quarter circle of a turning tile for drawArc, in tile units: the circle is
 * centered on the tile corner shared by the entry and exit sides with radius ½.
 */
data class ArcSpec(
    val cornerX: Float,
    val cornerY: Float,
    val startAngleDeg: Float,
    val sweepAngleDeg: Float,
)

private const val QUARTER_TURN_RAD = (PI / 2).toFloat()
private const val QUARTER_ARC_LENGTH = (PI / 4).toFloat()
private const val RAD_TO_DEG = (180.0 / PI).toFloat()

fun isTurn(entry: DIRECTION, exit: DIRECTION): Boolean = exit != entry.opposite()

/** Path length through one tile in tile units: a straight crossing, or a quarter circle of radius ½. */
fun pathLength(entry: DIRECTION, exit: DIRECTION): Float =
    if (isTurn(entry, exit)) QUARTER_ARC_LENGTH else 1f

/**
 * Where a schneagg is while crossing [tile] from the [entry] side to the [exit]
 * side. Straight tiles are a line between the two side middles, turns follow a
 * quarter circle so the schneagg glides around the corner instead of snapping.
 */
fun trackPoint(tile: Position, entry: DIRECTION, exit: DIRECTION, progress: Float): TrackPoint {
    val t = progress.coerceIn(0f, 1f)
    val centerX = tile.x + 0.5f
    val centerY = tile.y + 0.5f

    if (!isTurn(entry, exit)) {
        return TrackPoint(
            x = centerX + (t - 0.5f) * exit.dx,
            y = centerY + (t - 0.5f) * exit.dy,
            headingDeg = exit.angleDeg,
        )
    }

    // Arc around the corner between the entry and the exit side
    val angle = t * QUARTER_TURN_RAD
    val cosA = cos(angle)
    val sinA = sin(angle)
    val cornerX = centerX + 0.5f * (entry.dx + exit.dx)
    val cornerY = centerY + 0.5f * (entry.dy + exit.dy)
    val tangentX = exit.dx * sinA - entry.dx * cosA
    val tangentY = exit.dy * sinA - entry.dy * cosA
    return TrackPoint(
        x = cornerX - 0.5f * exit.dx * cosA - 0.5f * entry.dx * sinA,
        y = cornerY - 0.5f * exit.dy * cosA - 0.5f * entry.dy * sinA,
        headingDeg = atan2(tangentY, tangentX) * RAD_TO_DEG,
    )
}

/** Arc parameters for a turning tile; only valid when [isTurn] is true. */
fun arcSpec(tile: Position, entry: DIRECTION, exit: DIRECTION): ArcSpec {
    val startAngle = exit.opposite().angleDeg
    var sweep = entry.opposite().angleDeg - startAngle
    if (sweep > 180f) sweep -= 360f
    if (sweep <= -180f) sweep += 360f
    return ArcSpec(
        cornerX = tile.x + 0.5f + 0.5f * (entry.dx + exit.dx),
        cornerY = tile.y + 0.5f + 0.5f * (entry.dy + exit.dy),
        startAngleDeg = startAngle,
        sweepAngleDeg = sweep,
    )
}

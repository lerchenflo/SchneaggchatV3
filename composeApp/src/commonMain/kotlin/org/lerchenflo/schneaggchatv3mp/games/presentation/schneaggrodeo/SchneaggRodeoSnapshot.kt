package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo

import kotlinx.serialization.Serializable

const val SCHNEAGG_RODEO_SNAPSHOT_VERSION = 1

/**
 * Persisted mid-run progress. Only what the run has earned is kept (distance, pace, chase gap,
 * points, caught snails, super jump charges); fences and snails on the track are not, so a
 * restored run continues on an empty stretch of track.
 */
@Serializable
data class SchneaggRodeoSnapshot(
    val speed: Float,
    val distance: Float,
    val elapsedSeconds: Float,
    val runTimeSeconds: Float,
    val chaseGap: Float,
    val bonusPoints: Int,
    val fenceCount: Int,
    val snailsCaught: Int,
    val superJumpCharges: Int,
    val nextRunnerIn: Float,
)

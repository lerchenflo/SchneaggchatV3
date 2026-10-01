package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty

const val SCHNEAGG_RODEO_SNAPSHOT_VERSION = 1

/**
 * Persisted mid-run progress. Only what the run has earned is kept (distance, pace, chase gap,
 * the cowboy's wounds, points, caught snails, super jump charges, lucky charms, the horse and a friend riding along); fences and snails on the track are not, so a
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
    /** Added later; saves from before default to none. */
    val luckyCharms: Int = 0,
    /** The ridden horse's strength (see RodeoHorseStats); saves from before start on a fresh horse. */
    val horseLevel: Int = 1,
    val horseLives: Float = 3f,
    val horseCoat: Int = -1,
    /** A lassoed friend riding along, if any. */
    val passengerId: String? = null,
    val passengerName: String? = null,
    /** Hits the cowboy took from the ravens; saves from before start unhurt. */
    val cowboyWounds: Int = 0,
    /** The level picked for the run; saves from before were all medium. */
    val level: GameDifficulty = GameDifficulty.MEDIUM,
)

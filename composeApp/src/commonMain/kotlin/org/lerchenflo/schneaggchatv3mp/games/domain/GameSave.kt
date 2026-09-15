package org.lerchenflo.schneaggchatv3mp.games.domain

import kotlinx.serialization.Serializable

/**
 * Persisted run of one game. [data] is the game-specific snapshot; the envelope
 * carries what every game needs to decide whether the save is still usable.
 */
@Serializable
data class GameSave<T>(
    /** Bumped by the game when its snapshot layout changes; mismatches are discarded. */
    val schemaVersion: Int,
    val savedAtMillis: Long,
    /** Local calendar day (epoch days) the snapshot was taken — daily games only restore same-day saves. */
    val epochDay: Long,
    /** The run's own difficulty; the global selector may have changed since. */
    val difficulty: GameDifficulty,
    val data: T,
)

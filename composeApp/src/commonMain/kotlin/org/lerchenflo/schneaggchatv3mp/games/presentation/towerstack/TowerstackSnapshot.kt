package org.lerchenflo.schneaggchatv3mp.games.presentation.towerstack

import kotlinx.serialization.Serializable

const val TOWERSTACK_SNAPSHOT_VERSION = 1

/** Persisted mid-run tower state; the timer is re-anchored from [elapsedMillis] on restore. */
@Serializable
data class TowerstackSnapshot(
    val platforms: List<Platform>,
    val currentPlatform: Platform?,
    val score: Int,
    val gameSpeed: Float,
    val elapsedMillis: Long,
    val perfectStreak: Int,
)

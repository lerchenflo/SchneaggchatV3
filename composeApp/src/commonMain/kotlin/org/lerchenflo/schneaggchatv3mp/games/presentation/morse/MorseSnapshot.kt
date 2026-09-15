package org.lerchenflo.schneaggchatv3mp.games.presentation.morse

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.LanguageSetting

const val MORSE_SNAPSHOT_VERSION = 1

/** Persisted mid-run challenge; free practice mode has nothing worth keeping. */
@Serializable
data class MorseSnapshot(
    val targetText: String,
    val currentIndex: Int,
    val errors: Int,
    val score: Int,
    val elapsedMillis: Long,
    val charTimeLimitMs: Long,
    val charTimeRemainingMs: Long,
    /** Word list language of the run, so the endless text keeps growing in the same language. */
    val language: LanguageSetting,
)

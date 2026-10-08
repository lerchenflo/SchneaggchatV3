package org.lerchenflo.schneaggchatv3mp.games.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first

data class CChallengeStreak(
    /** Days solved in a row, ending today or yesterday; 0 once a day was missed or failed. */
    val current: Int = 0,
    val best: Int = 0,
)

/**
 * Local daily streak of the C challenge. Unlike the run save it has to outlive the day,
 * so it lives in its own keys instead of the (daily-expiring) game save.
 */
class CChallengeStreakRepository(
    private val prefs: DataStore<Preferences>,
) {
    suspend fun load(todayEpochDay: Long): CChallengeStreak {
        val data = prefs.data.first()
        val lastSolved = data[LAST_SOLVED_DAY] ?: Long.MIN_VALUE
        val stored = data[STREAK] ?: 0
        // Not solved yesterday or today: the streak is already broken
        val current = if (lastSolved >= todayEpochDay - 1) stored else 0
        return CChallengeStreak(current = current, best = data[BEST_STREAK] ?: 0)
    }

    /** Records the result of [epochDay]'s challenge once; later calls for the same day are ignored. */
    suspend fun recordResult(epochDay: Long, solved: Boolean) {
        prefs.edit { data ->
            if (data[LAST_PLAYED_DAY] == epochDay) return@edit
            data[LAST_PLAYED_DAY] = epochDay

            if (solved) {
                val continues = data[LAST_SOLVED_DAY] == epochDay - 1
                val streak = if (continues) (data[STREAK] ?: 0) + 1 else 1
                data[STREAK] = streak
                data[LAST_SOLVED_DAY] = epochDay
                data[BEST_STREAK] = maxOf(streak, data[BEST_STREAK] ?: 0)
            } else {
                data[STREAK] = 0
            }
        }
    }

    private companion object {
        val STREAK = intPreferencesKey("c_challenge_streak")
        val BEST_STREAK = intPreferencesKey("c_challenge_best_streak")
        val LAST_SOLVED_DAY = longPreferencesKey("c_challenge_last_solved_day")
        val LAST_PLAYED_DAY = longPreferencesKey("c_challenge_last_played_day")
    }
}

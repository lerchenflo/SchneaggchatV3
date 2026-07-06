package org.lerchenflo.schneaggchatv3mp.games.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.difficulty_high
import schneaggchatv3mp.composeapp.generated.resources.difficulty_low
import schneaggchatv3mp.composeapp.generated.resources.difficulty_medium

/**
 * In-memory, app-wide difficulty selection per game. Observable from Compose
 * (backed by a snapshot state map) and read by the game ViewModels when a run starts.
 */
object GameDifficultySelection {
    private val selections = mutableStateMapOf<GameId, GameDifficulty>()

    fun get(game: GameId): GameDifficulty = selections[game] ?: GameDifficulty.MEDIUM

    fun set(game: GameId, difficulty: GameDifficulty) {
        selections[game] = difficulty
    }
}

@Composable
fun GameDifficulty.stringRes(): StringResource = when (this) {
    GameDifficulty.LOW    -> Res.string.difficulty_low
    GameDifficulty.MEDIUM -> Res.string.difficulty_medium
    GameDifficulty.HIGH   -> Res.string.difficulty_high
}

/**
 * Row of chips to pick a [GameDifficulty].
 */
@Composable
fun DifficultySelector(
    selected: GameDifficulty,
    onSelect: (GameDifficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GameDifficulty.entries.forEach { difficulty ->
            FilterChip(
                selected = difficulty == selected,
                onClick = { onSelect(difficulty) },
                label = { Text(stringResource(difficulty.stringRes())) },
            )
        }
    }
}

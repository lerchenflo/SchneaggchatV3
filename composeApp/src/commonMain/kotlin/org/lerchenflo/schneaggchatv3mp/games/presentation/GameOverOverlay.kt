package org.lerchenflo.schneaggchatv3mp.games.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.game_exit
import schneaggchatv3mp.composeapp.generated.resources.game_over
import schneaggchatv3mp.composeapp.generated.resources.game_over_final_score
import schneaggchatv3mp.composeapp.generated.resources.game_over_final_time
import schneaggchatv3mp.composeapp.generated.resources.game_restart
import schneaggchatv3mp.composeapp.generated.resources.highscores_title

/**
 * Unified game-over overlay for all games: shows the final score with a difficulty
 * selection for the next round, a restart button, a button opening the server
 * highscores of [game] for the selected difficulty and an exit button leaving the game.
 */
@Composable
fun GameOverOverlay(
    game: GameId,
    finalScore: Long,
    finalTimeMillis: Long,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showHighscores by remember { mutableStateOf(false) }
    val selectedDifficulty = GameDifficultySelection.selected

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
            // Consume touches so the finished game underneath does not receive input
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(Res.string.game_over),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.game_over_final_score, finalScore),
                    style = MaterialTheme.typography.titleLarge,
                )

                Text(
                    text = stringResource(Res.string.game_over_final_time, formatGameTime(finalTimeMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                DifficultySelector(
                    selected = selectedDifficulty,
                    onSelect = { GameDifficultySelection.selected = it },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.game_restart))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showHighscores = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.highscores_title))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.game_exit))
                }
            }
        }
    }

    if (showHighscores) {
        HighscoresDialog(
            game = game,
            initialDifficulty = selectedDifficulty,
            onDismiss = { showHighscores = false }
        )
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.HighscoreEntry
import org.lerchenflo.schneaggchatv3mp.games.domain.LeaderboardPeriod
import org.lerchenflo.schneaggchatv3mp.games.domain.dartCounterCountdown
import org.lerchenflo.schneaggchatv3mp.games.domain.formatScore
import org.lerchenflo.schneaggchatv3mp.games.domain.hasTimedScores
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.highscores_empty
import schneaggchatv3mp.composeapp.generated.resources.highscores_error
import schneaggchatv3mp.composeapp.generated.resources.highscores_period_all_time
import schneaggchatv3mp.composeapp.generated.resources.highscores_period_daily
import schneaggchatv3mp.composeapp.generated.resources.highscores_period_weekly
import schneaggchatv3mp.composeapp.generated.resources.highscores_period_yearly
import schneaggchatv3mp.composeapp.generated.resources.highscores_title

/**
 * Highscores dialog: asks its ViewModel for the leaderboard of [game] and lets the player
 * switch board and time window. [initialDifficulty] is only the board it opens on.
 */
@Composable
fun HighscoresDialog(
    game: GameId,
    initialDifficulty: GameDifficulty,
    onDismiss: () -> Unit,
    viewModel: HighscoresViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(game, initialDifficulty) {
        viewModel.onAction(HighscoresAction.OnOpen(game, initialDifficulty))
    }

    HighscoresDialogContent(
        state = state,
        onAction = viewModel::onAction,
        onDismiss = onDismiss,
    )
}

@Composable
fun HighscoresDialogContent(
    state: HighscoresState,
    onAction: (HighscoresAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val game = state.game
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        },
        text = {
            Column {
                if (game == GameId.DART_COUNTER) {
                    // Dart boards are split by countdown, not by difficulty
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.boards.forEach { board ->
                            FilterChip(
                                selected = board == state.selectedDifficulty,
                                onClick = { onAction(HighscoresAction.OnSelectDifficulty(board)) },
                                label = { Text(dartCounterCountdown(board).toString()) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (state.boards.size > 1) {
                    DifficultySelector(
                        selected = state.selectedDifficulty,
                        onSelect = { onAction(HighscoresAction.OnSelectDifficulty(it)) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                PeriodSelector(
                    selected = state.selectedPeriod,
                    onSelect = { onAction(HighscoresAction.OnSelectPeriod(it)) },
                )

                Spacer(modifier = Modifier.height(8.dp))

                GameHighscores(
                    state = state,
                    modifier = Modifier.heightIn(max = 400.dp),
                    formatScore = { score -> game?.formatScore(score) ?: score.toString() },
                    showTime = game?.hasTimedScores ?: true,
                )
            }
        }
    )
}

@Composable
internal fun LeaderboardPeriod.stringRes(): StringResource = when (this) {
    LeaderboardPeriod.DAILY    -> Res.string.highscores_period_daily
    LeaderboardPeriod.WEEKLY   -> Res.string.highscores_period_weekly
    LeaderboardPeriod.YEARLY   -> Res.string.highscores_period_yearly
    LeaderboardPeriod.ALL_TIME -> Res.string.highscores_period_all_time
}

/**
 * Row of chips to pick the leaderboard time window.
 */
@Composable
internal fun PeriodSelector(
    selected: LeaderboardPeriod,
    onSelect: (LeaderboardPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LeaderboardPeriod.entries.forEach { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onSelect(period) },
                label = { Text(stringResource(period.stringRes())) },
            )
        }
    }
}

/**
 * Reusable leaderboard for game screens. Shows the live server highscores of one game,
 * highlighting the logged-in user's own entry.
 */
@Composable
fun GameHighscores(
    state: HighscoresState,
    modifier: Modifier = Modifier,
    formatScore: (Long) -> String = { it.toString() },
    showTime: Boolean = true,
) {
    val ownUserId = SessionCache.requireLoggedIn()?.userId

    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.highscores_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
            state.hasError -> {
                Text(
                    text = stringResource(Res.string.highscores_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.entries.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.highscores_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    state.entries.forEach { entry ->
                        HighscoreRow(
                            entry = entry,
                            isOwn = entry.userId == ownUserId,
                            scoreText = formatScore(entry.score),
                            showTime = showTime,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HighscoreRow(
    entry: HighscoreEntry,
    isOwn: Boolean,
    scoreText: String,
    showTime: Boolean,
) {
    val contentColor = if (isOwn) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isOwn) Modifier.background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${entry.rank}.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = entry.username,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isOwn) FontWeight.Bold else FontWeight.Normal,
            color = contentColor,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = scoreText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
        if (showTime) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatGameTime(entry.timeMillis),
                style = MaterialTheme.typography.bodySmall,
                color = if (isOwn) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GlobalRankingEntry
import org.lerchenflo.schneaggchatv3mp.games.domain.LeaderboardPeriod
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.global_ranking_boards
import schneaggchatv3mp.composeapp.generated.resources.global_ranking_empty
import schneaggchatv3mp.composeapp.generated.resources.global_ranking_error
import schneaggchatv3mp.composeapp.generated.resources.global_ranking_explanation
import schneaggchatv3mp.composeapp.generated.resources.global_ranking_points
import schneaggchatv3mp.composeapp.generated.resources.global_ranking_title

/**
 * UI state for the cross-game leaderboard.
 */
data class GlobalRankingUiState(
    val entries: List<GlobalRankingEntry> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
)

/**
 * Self-contained ranking dialog over all games: fetches the global leaderboard from the server
 * every time it is opened or another period is selected. Unlike the per-game highscores this
 * has no difficulty filter — the server sums points over every game and difficulty.
 */
@Composable
fun GlobalRankingDialog(
    onDismiss: () -> Unit,
) {
    val repository = koinInject<GameHighscoreRepository>()
    var selectedPeriod by remember { mutableStateOf(LeaderboardPeriod.YEARLY) }
    var state by remember { mutableStateOf(GlobalRankingUiState(isLoading = true)) }

    LaunchedEffect(selectedPeriod) {
        state = GlobalRankingUiState(isLoading = true)
        state = when (val result = repository.getGlobalRanking(selectedPeriod)) {
            is NetworkResult.Success -> GlobalRankingUiState(entries = result.data)
            is NetworkResult.Error -> GlobalRankingUiState(hasError = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        },
        text = {
            Column {
                PeriodSelector(
                    selected = selectedPeriod,
                    onSelect = { selectedPeriod = it },
                )

                Spacer(modifier = Modifier.height(8.dp))

                GlobalRanking(
                    state = state,
                    modifier = Modifier.heightIn(max = 400.dp)
                )
            }
        }
    )
}

/**
 * Leaderboard over all games, highlighting the logged-in user's own entry.
 */
@Composable
fun GlobalRanking(
    state: GlobalRankingUiState,
    modifier: Modifier = Modifier,
) {
    val ownUserId = SessionCache.requireLoggedIn()?.userId

    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.global_ranking_title),
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = stringResource(Res.string.global_ranking_explanation),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
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
                    text = stringResource(Res.string.global_ranking_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.entries.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.global_ranking_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    state.entries.forEach { entry ->
                        GlobalRankingRow(
                            entry = entry,
                            isOwn = entry.userId == ownUserId,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalRankingRow(
    entry: GlobalRankingEntry,
    isOwn: Boolean,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.username,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isOwn) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
                maxLines = 1,
            )
            Text(
                text = stringResource(
                    Res.string.global_ranking_boards,
                    entry.boardsPlayed,
                    entry.gamesPlayed
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (isOwn) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.global_ranking_points, entry.points),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
    }
}

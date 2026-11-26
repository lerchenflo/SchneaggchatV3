package org.lerchenflo.schneaggchatv3mp.games.presentation.fourinarow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameResetButton
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadDialog
import org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector.PlayerSelector
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_rules
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_rules_text
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_rules_title
import schneaggchatv3mp.composeapp.generated.resources.games_fourinarow_title

@Composable
fun FourInARowRoot(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FourInARowViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Leaving the screen (e.g. opening a chat from a notification) keeps the running match
    DisposableEffect(Unit) {
        onDispose { viewModel.persist() }
    }
    // The move timer must not run out while nobody can see the board
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(FourInARowAction.OnScreenVisibleChange(visible = true))
        onPauseOrDispose { viewModel.onAction(FourInARowAction.OnScreenVisibleChange(visible = false)) }
    }

    FourInARowScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
fun FourInARowScreen(
    state: FourInARowState,
    onAction: (FourInARowAction) -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_fourinarow_title),
            onBackClick = onBackClick,
            actions = {
                IconButton(onClick = { onAction(FourInARowAction.OnShowRules) }) {
                    Icon(Icons.Default.Info, contentDescription = stringResource(Res.string.fourinarow_rules))
                }
                // Back to setup with the same players and options; also drops the saved match
                if (state.round != null) {
                    GameResetButton(onReset = { onAction(FourInARowAction.OnResetGame) })
                }
            }
        )

        val round = state.round
        if (round == null) {
            FourInARowSetupContent(state = state, onAction = onAction)
        } else {
            FourInARowMatchContent(state = state, round = round, onAction = onAction)
        }
    }

    if (state.showPlayerSelector) {
        PlayerSelector(
            onDismiss = { onAction(FourInARowAction.OnHidePlayerSelector) },
            onFinish = { players -> onAction(FourInARowAction.OnPlayersSelected(players)) }
        )
    }

    if (state.showRules) {
        AlertDialog(
            onDismissRequest = { onAction(FourInARowAction.OnHideRules) },
            title = { Text(stringResource(Res.string.fourinarow_rules_title)) },
            text = {
                Text(
                    text = stringResource(Res.string.fourinarow_rules_text),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { onAction(FourInARowAction.OnHideRules) }) {
                    Text(stringResource(Res.string.close))
                }
            }
        )
    }

    // Asked once when a match is finished - wins only reach the leaderboard on explicit confirmation
    HighscoreUploadDialog(
        state = state.highscoreUpload,
        onUpload = { onAction(FourInARowAction.OnUploadHighscores) },
        onDecline = { onAction(FourInARowAction.OnDeclineHighscoreUpload) },
    )
}

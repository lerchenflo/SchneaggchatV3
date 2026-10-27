package org.lerchenflo.schneaggchatv3mp.games.presentation.tictactoe

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.domain.GamePlayer
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.TIC_TAC_TOE_MAX_PLAYERS
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.TicTacToeRound
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameResetButton
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadDialog
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadDoneText
import org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector.PlayerSelector
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.games_tictactoe_title
import schneaggchatv3mp.composeapp.generated.resources.remove
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_board_info
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_bomb
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_bomb_armed
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_bomb_used
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_choose_players
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_draw
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_finish_match
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_next_round
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_players_hint
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_players_title
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_round
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_rules
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_rules_text
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_rules_title
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_shift_column
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_shift_countdown
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_shift_row
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_start
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_too_many_players
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_turn
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_twist_bombs
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_twist_bombs_description
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_twist_chaos
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_twist_chaos_description
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_twist_gravity
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_twist_gravity_description
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_twist_vanishing
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_twist_vanishing_description
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_twists_title
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_winner

@Composable
fun TicTacToeRoot(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TicTacToeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Leaving the screen (e.g. opening a chat from a notification) keeps the running match
    DisposableEffect(Unit) {
        onDispose { viewModel.persist() }
    }

    TicTacToeScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
fun TicTacToeScreen(
    state: TicTacToeState,
    onAction: (TicTacToeAction) -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_tictactoe_title),
            onBackClick = onBackClick,
            actions = {
                IconButton(onClick = { onAction(TicTacToeAction.OnShowRules) }) {
                    Icon(Icons.Default.Info, contentDescription = stringResource(Res.string.tictactoe_rules))
                }
                // Back to setup with the same players and twists; also drops the saved match
                if (state.round != null) {
                    GameResetButton(onReset = { onAction(TicTacToeAction.OnResetGame) })
                }
            }
        )

        val round = state.round
        if (round == null) {
            SetupContent(state = state, onAction = onAction)
        } else {
            MatchContent(state = state, round = round, onAction = onAction)
        }
    }

    if (state.showPlayerSelector) {
        PlayerSelector(
            onDismiss = { onAction(TicTacToeAction.OnHidePlayerSelector) },
            onFinish = { players -> onAction(TicTacToeAction.OnPlayersSelected(players)) }
        )
    }

    if (state.showRules) {
        AlertDialog(
            onDismissRequest = { onAction(TicTacToeAction.OnHideRules) },
            title = { Text(stringResource(Res.string.tictactoe_rules_title)) },
            text = {
                Text(
                    text = stringResource(Res.string.tictactoe_rules_text),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { onAction(TicTacToeAction.OnHideRules) }) {
                    Text(stringResource(Res.string.close))
                }
            }
        )
    }

    // Asked once when a match is finished - wins only reach the leaderboard on explicit confirmation
    HighscoreUploadDialog(
        state = state.highscoreUpload,
        onUpload = { onAction(TicTacToeAction.OnUploadHighscores) },
        onDecline = { onAction(TicTacToeAction.OnDeclineHighscoreUpload) },
    )
}

// ─── Setup ────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.SetupContent(
    state: TicTacToeState,
    onAction: (TicTacToeAction) -> Unit,
) {
    val config = state.setupConfig

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Result of the upload offered when the last match was finished
        HighscoreUploadDoneText(state = state.highscoreUpload, modifier = Modifier.fillMaxWidth())

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.tictactoe_players_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                state.players.forEachIndexed { index, player ->
                    SetupPlayerRow(
                        index = index,
                        player = player,
                        onRemove = { onAction(TicTacToeAction.OnRemovePlayer(index)) },
                    )
                }
                if (state.players.size > TIC_TAC_TOE_MAX_PLAYERS) {
                    Text(
                        text = stringResource(Res.string.tictactoe_too_many_players, TIC_TAC_TOE_MAX_PLAYERS),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.tictactoe_players_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = { onAction(TicTacToeAction.OnShowPlayerSelector) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.tictactoe_choose_players))
                }
            }
        }

        Text(
            text = stringResource(Res.string.tictactoe_board_info, config.boardSize, config.winLength),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = stringResource(Res.string.tictactoe_twists_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                TwistRow(
                    icon = Icons.Default.HourglassBottom,
                    title = stringResource(Res.string.tictactoe_twist_vanishing),
                    description = stringResource(Res.string.tictactoe_twist_vanishing_description, config.marksKept),
                    checked = state.twists.vanishing,
                    onCheckedChange = { onAction(TicTacToeAction.OnTwistsChange(state.twists.copy(vanishing = it))) },
                )
                TwistRow(
                    icon = Icons.Default.VerticalAlignBottom,
                    title = stringResource(Res.string.tictactoe_twist_gravity),
                    description = stringResource(Res.string.tictactoe_twist_gravity_description),
                    checked = state.twists.gravity,
                    onCheckedChange = { onAction(TicTacToeAction.OnTwistsChange(state.twists.copy(gravity = it))) },
                )
                TwistRow(
                    icon = Icons.Default.LocalFireDepartment,
                    title = stringResource(Res.string.tictactoe_twist_bombs),
                    description = stringResource(Res.string.tictactoe_twist_bombs_description),
                    checked = state.twists.bombs,
                    onCheckedChange = { onAction(TicTacToeAction.OnTwistsChange(state.twists.copy(bombs = it))) },
                )
                TwistRow(
                    icon = Icons.Default.Shuffle,
                    title = stringResource(Res.string.tictactoe_twist_chaos),
                    description = stringResource(Res.string.tictactoe_twist_chaos_description, config.turnsPerShift),
                    checked = state.twists.chaos,
                    onCheckedChange = { onAction(TicTacToeAction.OnTwistsChange(state.twists.copy(chaos = it))) },
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { onAction(TicTacToeAction.OnStartGame) },
        enabled = state.canStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(Res.string.tictactoe_start))
    }
}

@Composable
private fun SetupPlayerRow(index: Int, player: GamePlayer, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Only the first four play; more are listed so they can be removed
        if (index < TIC_TAC_TOE_MAX_PLAYERS) {
            PlayerSymbol(player = index, modifier = Modifier.size(22.dp))
        } else {
            Spacer(Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = player.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.remove))
        }
    }
}

@Composable
private fun TwistRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ─── Match ────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.MatchContent(
    state: TicTacToeState,
    round: TicTacToeRound,
    onAction: (TicTacToeAction) -> Unit,
) {
    Text(
        text = stringResource(Res.string.tictactoe_round, state.roundNumber),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))

    Scoreboard(state = state, round = round)

    Spacer(Modifier.height(12.dp))
    StatusLine(state = state, round = round)
    Spacer(Modifier.height(12.dp))

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TicTacToeBoard(
            round = round,
            bombArmed = state.bombArmed,
            onCellClick = { onAction(TicTacToeAction.OnCellClick(it)) },
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
        )
    }

    Spacer(Modifier.height(12.dp))

    if (round.isOver) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { onAction(TicTacToeAction.OnFinishMatch) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(Res.string.tictactoe_finish_match), textAlign = TextAlign.Center)
            }
            Button(
                onClick = { onAction(TicTacToeAction.OnNextRound) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(Res.string.tictactoe_next_round), textAlign = TextAlign.Center)
            }
        }
    } else if (round.config.bombs) {
        BombButton(state = state, round = round, onAction = onAction)
    }
}

@Composable
private fun Scoreboard(state: TicTacToeState, round: TicTacToeRound) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.players.forEachIndexed { index, player ->
            val active = !round.isOver && index == round.currentPlayer || round.winner == index
            val scale by animateFloatAsState(if (active) 1.05f else 1f, label = "scoreScale")
            val borderColor by animateColorAsState(
                if (active) playerColor(index) else MaterialTheme.colorScheme.outlineVariant,
                label = "scoreBorder",
            )
            Card(
                modifier = Modifier
                    .weight(1f)
                    .scale(scale),
                border = BorderStroke(if (active) 2.dp else 1.dp, borderColor),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PlayerSymbol(player = index, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.wins.getOrElse(index) { 0 }.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = playerColor(index),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusLine(state: TicTacToeState, round: TicTacToeRound) {
    val winner = round.winner
    val shift = round.lastShift
    val (text, color) = when {
        winner != null -> stringResource(Res.string.tictactoe_winner, state.players.getOrNull(winner)?.name.orEmpty()) to playerColor(winner)
        round.draw -> stringResource(Res.string.tictactoe_draw) to MaterialTheme.colorScheme.onSurface
        state.bombArmed -> stringResource(Res.string.tictactoe_bomb_armed) to MaterialTheme.colorScheme.error
        else -> stringResource(Res.string.tictactoe_turn, state.players.getOrNull(round.currentPlayer)?.name.orEmpty()) to
            playerColor(round.currentPlayer)
    }
    val scale by animateFloatAsState(if (round.isOver) 1.15f else 1f, label = "statusScale")

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.scale(scale)) {
            if (!round.isOver && !state.bombArmed) {
                PlayerSymbol(player = round.currentPlayer, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
            )
        }
        // The chaos line: what just shifted, or how long until the next shuffle
        if (!round.isOver) {
            val chaosText = when {
                shift != null && shift.isRow -> stringResource(Res.string.tictactoe_shift_row, shift.index + 1)
                shift != null -> stringResource(Res.string.tictactoe_shift_column, shift.index + 1)
                else -> round.turnsUntilShift?.let { stringResource(Res.string.tictactoe_shift_countdown, it) }
            }
            if (chaosText != null) {
                Text(
                    text = chaosText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (shift != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BombButton(
    state: TicTacToeState,
    round: TicTacToeRound,
    onAction: (TicTacToeAction) -> Unit,
) {
    val used = round.currentPlayer in round.bombsUsed
    val label = if (used) Res.string.tictactoe_bomb_used else Res.string.tictactoe_bomb
    if (state.bombArmed) {
        Button(
            onClick = { onAction(TicTacToeAction.OnToggleBomb) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.LocalFireDepartment, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(label))
        }
    } else {
        FilledTonalButton(
            onClick = { onAction(TicTacToeAction.OnToggleBomb) },
            enabled = round.canBomb(round.currentPlayer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.LocalFireDepartment, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(label))
        }
    }
}

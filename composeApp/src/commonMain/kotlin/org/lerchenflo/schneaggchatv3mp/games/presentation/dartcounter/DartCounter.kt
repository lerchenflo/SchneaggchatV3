package org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.dartCounterDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartOutMode
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartSegment
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartThrow
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.toSegment
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadDialog
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadDoneText
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoresDialog
import org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector.PlayerSelector
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_add_players
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_avg_format
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_cancel
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_checkout_alt_format
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_checkout_title
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_configure_game_hint
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_countdown_label
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_dart_slot_empty
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_darts_format
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_darts_left_format
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_finished_suffix
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_game_configuration_title
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_highscores
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_list_separator
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_miss
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_multiplier_double
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_multiplier_single
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_multiplier_triple
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_out_mode_double_out
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_out_mode_label
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_out_mode_single_out
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_players_format
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_please_add_players_first
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_segment_bull
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_segment_double_format
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_segment_triple_format
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_start_game
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_stop_game
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_stop_game_confirmation_message
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_turn_total_format
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_undo
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_winners_format
import schneaggchatv3mp.composeapp.generated.resources.games_dartcounter_title

@Composable
fun DartCounterRoot(
    onBackClick: () -> Unit = {},
    viewModel: DartCounterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Leaving the screen (e.g. opening a chat from a notification) keeps the running game
    DisposableEffect(Unit) {
        onDispose { viewModel.persist() }
    }

    DartCounterScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
    )
}

@Composable
fun DartCounterScreen(
    state: DartCounterState,
    onAction: (DartCounterAction) -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_dartcounter_title),
            onBackClick = onBackClick
        )

        TopActionRow(state = state, onAction = onAction)

        // Board and player card share the remaining space so the card can never be pushed off screen
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val isWide = maxWidth > 600.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GameStatusOrHint(state = state, modifier = Modifier.weight(1f).fillMaxHeight())
                    DartBoard(
                        state = state,
                        onAction = onAction,
                        modifier = Modifier.weight(1.2f).fillMaxHeight()
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DartBoard(state = state, onAction = onAction, modifier = Modifier.weight(1.3f))
                    GameStatusOrHint(state = state, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // Dialogs
    if (state.showPlayerSetup) {
        PlayerSelector(
            onDismiss = { onAction(DartCounterAction.OnPlayerSetupDismiss) },
            onFinish = { selectedPlayers -> onAction(DartCounterAction.OnPlayersSelected(selectedPlayers)) }
        )
    }

    if (state.showGameConfig) {
        GameConfigDialog(state = state, onAction = onAction)
    }

    if (state.showStopGameDialog) {
        StopGameConfirmationDialog(onAction = onAction)
    }

    if (state.showHighscores) {
        HighscoresDialog(
            game = GameId.DART_COUNTER,
            initialDifficulty = dartCounterDifficulty(state.selectedCountdown),
            onDismiss = { onAction(DartCounterAction.OnHighscoresDismiss) }
        )
    }

    // Asked once every player finished - averages only reach the leaderboard on explicit confirmation
    HighscoreUploadDialog(
        state = state.highscoreUpload,
        onUpload = { onAction(DartCounterAction.OnUploadHighscores) },
        onDecline = { onAction(DartCounterAction.OnDeclineHighscoreUpload) },
    )
}

/** Button label that shrinks (and wraps to a second line if needed) instead of clipping on narrow buttons. */
@Composable
private fun FittingButtonText(text: String, maxFontSize: TextUnit = 14.sp) {
    Text(
        text = text,
        maxLines = 2,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = maxFontSize)
    )
}

private val CompactButtonPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)

@Composable
private fun TopActionRow(state: DartCounterState, onAction: (DartCounterAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Same fixed height for all three, tall enough for a two-line label
        val buttonModifier = Modifier
            .weight(1f)
            .height(52.dp)

        FilledTonalButton(
            onClick = { onAction(DartCounterAction.OnAddPlayersClick) },
            modifier = buttonModifier,
            enabled = !state.gameStarted,
            contentPadding = CompactButtonPadding
        ) {
            FittingButtonText(stringResource(Res.string.dartcounter_add_players))
        }

        if (state.gameStarted) {
            Button(
                onClick = { onAction(DartCounterAction.OnStopGameClick) },
                modifier = buttonModifier,
                contentPadding = CompactButtonPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                FittingButtonText(stringResource(Res.string.dartcounter_stop_game))
            }
        } else {
            Button(
                onClick = { onAction(DartCounterAction.OnConfigureGameClick) },
                modifier = buttonModifier,
                contentPadding = CompactButtonPadding
            ) {
                FittingButtonText(stringResource(Res.string.dartcounter_start_game))
            }
        }

        OutlinedButton(
            onClick = { onAction(DartCounterAction.OnHighscoresClick) },
            modifier = buttonModifier,
            contentPadding = CompactButtonPadding
        ) {
            FittingButtonText(stringResource(Res.string.dartcounter_highscores))
        }
    }
}

@Composable
private fun GameStatusOrHint(state: DartCounterState, modifier: Modifier = Modifier) {
    if (state.gameStarted) {
        GameStatusDisplay(state = state, modifier = modifier)
    } else {
        Card(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.dartcounter_configure_game_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun formatSegment(segment: DartSegment): String = when {
    segment.base == 25 && segment.isDouble -> stringResource(Res.string.dartcounter_segment_bull)
    segment.base == 0 -> stringResource(Res.string.dartcounter_miss)
    segment.isTriple -> stringResource(Res.string.dartcounter_segment_triple_format, segment.base)
    segment.isDouble -> stringResource(Res.string.dartcounter_segment_double_format, segment.base)
    else -> segment.base.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DartBoard(
    state: DartCounterState,
    onAction: (DartCounterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedMultiplier = state.selectedMultiplier
    val padEnabled = state.padEnabled
    val padColor = if (selectedMultiplier == DartMultiplier.SINGLE) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val padContentColor = if (selectedMultiplier == DartMultiplier.SINGLE) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onTertiary
    }

    Column(modifier = modifier) {
        // Multiplier buttons - the active one is highlighted and relabels the whole pad below
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            val multipliers = DartMultiplier.entries
            multipliers.forEachIndexed { index, multiplier ->
                SegmentedButton(
                    selected = selectedMultiplier == multiplier,
                    onClick = { onAction(DartCounterAction.OnMultiplierSelect(multiplier)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = multipliers.size),
                    // No checkmark: the highlight is enough and the icon steals width from the label
                    icon = {}
                ) {
                    val multiplierText = when (multiplier) {
                        DartMultiplier.SINGLE -> stringResource(Res.string.dartcounter_multiplier_single)
                        DartMultiplier.DOUBLE -> stringResource(Res.string.dartcounter_multiplier_double)
                        DartMultiplier.TRIPLE -> stringResource(Res.string.dartcounter_multiplier_triple)
                    }
                    Text(
                        text = multiplierText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 14.sp)
                    )
                }
            }
        }

        // Dart board numbers - cell height adapts to the space actually left over so the
        // player card below is never pushed off screen
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val rows = 5
            val spacing = 6.dp
            val cellHeight = ((maxHeight - spacing * (rows - 1)) / rows).coerceIn(36.dp, 72.dp)

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(20) { index ->
                    val base = index + 1
                    val segment = when (selectedMultiplier) {
                        DartMultiplier.SINGLE -> DartSegment.single(base)
                        DartMultiplier.DOUBLE -> DartSegment.double(base)
                        DartMultiplier.TRIPLE -> DartSegment.triple(base)
                    }
                    DartPadButton(
                        label = formatSegment(segment),
                        height = cellHeight,
                        containerColor = padColor,
                        contentColor = padContentColor,
                        enabled = padEnabled,
                        onClick = { onAction(DartCounterAction.OnDartThrow(segment)) }
                    )
                }

                // 25, bullseye and miss are not affected by the multiplier - always Single-coloured
                listOf(DartSegment.OUTER_BULL, DartSegment.BULLSEYE, DartSegment.MISS).forEach { segment ->
                    item {
                        DartPadButton(
                            label = formatSegment(segment),
                            height = cellHeight,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            enabled = padEnabled,
                            onClick = { onAction(DartCounterAction.OnDartThrow(segment)) }
                        )
                    }
                }

                // Undo button as last item in grid
                item {
                    DartPadButton(
                        label = stringResource(Res.string.dartcounter_undo),
                        height = cellHeight,
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        enabled = state.undoEnabled,
                        onClick = { onAction(DartCounterAction.OnUndoThrow) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DartPadButton(
    label: String,
    height: Dp,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        // The default 24dp side padding leaves almost no room for the label in a 5-column grid
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
            autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 18.sp)
        )
    }
}

/**
 * The bottom card: the active player's turn (remaining score, darts of this turn and the best
 * checkout path, see DartCheckout.findCheckouts) above the scoreboard of all players.
 */
@Composable
private fun GameStatusDisplay(state: DartCounterState, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.gameOver) {
                val names = state.winnerNames.joinToString(
                    separator = stringResource(Res.string.dartcounter_list_separator)
                )
                Text(
                    text = stringResource(Res.string.dartcounter_winners_format, names),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                HighscoreUploadDoneText(state = state.highscoreUpload)
            } else {
                CurrentTurnHeader(
                    playerName = state.currentPlayerName,
                    remainingScore = state.currentPlayerScore,
                    dartsLeft = state.dartsLeft,
                    turnDarts = state.turnDarts,
                    turnTotal = state.turnTotal,
                    checkout = state.checkout,
                    alternativeCheckout = state.alternativeCheckout,
                )
            }

            HorizontalDivider()

            val listState = rememberLazyListState()
            // Keep the active player in view
            LaunchedEffect(state.currentPlayerIndex) {
                listState.animateScrollToItem(state.currentPlayerIndex)
            }

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.players.size) { index ->
                    PlayerScoreRow(row = state.players[index])
                }
            }
        }
    }
}

@Composable
private fun CurrentTurnHeader(
    playerName: String,
    remainingScore: Int,
    dartsLeft: Int,
    turnDarts: List<DartThrow>,
    turnTotal: Int,
    checkout: List<DartSegment>,
    alternativeCheckout: List<DartSegment>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(Res.string.dartcounter_darts_left_format, dartsLeft),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = remainingScore.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }

        // Three slots for this turn's darts, filled as they are thrown
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { slot ->
                val dart = turnDarts.getOrNull(slot)
                DartSlot(
                    label = dart?.let { formatSegment(it.toSegment()) },
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = stringResource(Res.string.dartcounter_turn_total_format, turnTotal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 56.dp),
                textAlign = TextAlign.End
            )
        }

        // Checkout path, only once a finish is within reach this turn (at most 3 short labels per line)
        if (checkout.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.dartcounter_checkout_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                checkout.forEach { segment ->
                    Text(
                        text = formatSegment(segment),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
            if (alternativeCheckout.isNotEmpty()) {
                val alternative = alternativeCheckout.map { formatSegment(it) }.joinToString(" ")
                Text(
                    text = stringResource(Res.string.dartcounter_checkout_alt_format) + " " + alternative,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DartSlot(label: String?, modifier: Modifier = Modifier) {
    val filled = label != null
    Surface(
        modifier = modifier.height(32.dp),
        shape = MaterialTheme.shapes.small,
        color = if (filled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (filled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label ?: stringResource(Res.string.dartcounter_dart_slot_empty),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (filled) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 14.sp)
            )
        }
    }
}

@Composable
private fun PlayerScoreRow(row: DartPlayerUi) {
    // Non-current rows blend into the surrounding card
    val containerColor = if (row.isCurrent) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        CardDefaults.cardColors().containerColor
    }
    val contentColor = when {
        row.isCurrent -> MaterialTheme.colorScheme.onSecondaryContainer
        row.isFinished -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = row.name + if (row.isFinished) stringResource(Res.string.dartcounter_finished_suffix) else "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (row.isCurrent || row.isFinished) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(Res.string.dartcounter_avg_format, row.averageText),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                Text(
                    text = stringResource(Res.string.dartcounter_darts_format, row.dartsThrown),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            Text(
                text = row.score.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 48.dp)
            )
        }
    }
}

@Composable
private fun GameConfigDialog(state: DartCounterState, onAction: (DartCounterAction) -> Unit) {
    Dialog(onDismissRequest = { onAction(DartCounterAction.OnGameConfigDismiss) }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.dartcounter_game_configuration_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.dartcounter_countdown_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(301, 501).forEach { countdown ->
                        Button(
                            onClick = { onAction(DartCounterAction.OnCountdownSelect(countdown)) },
                            modifier = Modifier.weight(1f),
                            contentPadding = CompactButtonPadding,
                            colors = selectableButtonColors(state.selectedCountdown == countdown)
                        ) {
                            FittingButtonText(countdown.toString())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.dartcounter_out_mode_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DartOutMode.entries.forEach { mode ->
                        Button(
                            onClick = { onAction(DartCounterAction.OnOutModeSelect(mode)) },
                            modifier = Modifier.weight(1f),
                            contentPadding = CompactButtonPadding,
                            colors = selectableButtonColors(state.selectedOutMode == mode)
                        ) {
                            val modeText = when (mode) {
                                DartOutMode.SINGLE_OUT -> stringResource(Res.string.dartcounter_out_mode_single_out)
                                DartOutMode.DOUBLE_OUT -> stringResource(Res.string.dartcounter_out_mode_double_out)
                            }
                            FittingButtonText(modeText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(
                        Res.string.dartcounter_players_format,
                        state.playerNames.joinToString(
                            separator = stringResource(Res.string.dartcounter_list_separator)
                        )
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (state.playerNames.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.dartcounter_please_add_players_first),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { onAction(DartCounterAction.OnGameConfigDismiss) },
                        modifier = Modifier.weight(1f),
                        contentPadding = CompactButtonPadding
                    ) {
                        FittingButtonText(stringResource(Res.string.dartcounter_cancel))
                    }

                    Button(
                        onClick = { onAction(DartCounterAction.OnConfirmStartGame) },
                        modifier = Modifier.weight(1f),
                        contentPadding = CompactButtonPadding,
                        enabled = state.canStartGame
                    ) {
                        FittingButtonText(stringResource(Res.string.dartcounter_start_game))
                    }
                }
            }
        }
    }
}

@Composable
private fun StopGameConfirmationDialog(onAction: (DartCounterAction) -> Unit) {
    Dialog(onDismissRequest = { onAction(DartCounterAction.OnStopGameDismiss) }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.dartcounter_stop_game),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.dartcounter_stop_game_confirmation_message),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { onAction(DartCounterAction.OnStopGameDismiss) },
                        modifier = Modifier.weight(1f),
                        contentPadding = CompactButtonPadding
                    ) {
                        FittingButtonText(stringResource(Res.string.dartcounter_cancel))
                    }

                    Button(
                        onClick = { onAction(DartCounterAction.OnConfirmStopGame) },
                        modifier = Modifier.weight(1f),
                        contentPadding = CompactButtonPadding,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        FittingButtonText(stringResource(Res.string.dartcounter_stop_game))
                    }
                }
            }
        }
    }
}

/** Selected option filled in primary, the others in a quieter tonal color with a matching label color. */
@Composable
private fun selectableButtonColors(selected: Boolean) = ButtonDefaults.buttonColors(
    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
)

@Preview
@Composable
private fun DartCounterScreenPreview() {
    DartCounterScreen(
        state = DartCounterState(
            gameStarted = true,
            currentPlayerName = "Flo",
            currentPlayerScore = 141,
            dartsLeft = 3,
            players = listOf(
                DartPlayerUi("Flo", 141, "62.4", 18, isFinished = false, isCurrent = true),
                DartPlayerUi("Manu", 220, "48.1", 18, isFinished = false, isCurrent = false),
            ),
        ),
        onAction = {},
    )
}

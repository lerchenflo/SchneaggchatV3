package org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartSegment
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.findCheckouts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.dartCounterDifficulty
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
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_dart_slot_empty
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_configure_game_hint
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_countdown_label
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
import kotlin.math.round
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box

@Preview(
    showBackground = true,
    showSystemUi = true
)

@Composable
fun DartCounter(
    onBackClick: () -> Unit = {}
) {
    val viewmodel = koinViewModel<DartCounterViewModel>()

    // Leaving the screen (e.g. opening a chat from a notification) keeps the running game
    DisposableEffect(Unit) {
        onDispose { viewmodel.persist() }
    }

    // todo: he manu du musch noch an backbutton ine tua sunsch sind die Iphone user stuck

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_dartcounter_title),
            onBackClick = onBackClick
        )

        TopActionRow(viewmodel = viewmodel)

        // Board and player card share the remaining space so the card can never be pushed off screen
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val isWide = maxWidth > 600.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GameStatusOrHint(viewmodel = viewmodel, modifier = Modifier.weight(1f).fillMaxHeight())
                    DartBoard(viewmodel = viewmodel, modifier = Modifier.weight(1.2f).fillMaxHeight())
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DartBoard(viewmodel = viewmodel, modifier = Modifier.weight(1.3f))
                    GameStatusOrHint(viewmodel = viewmodel, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // Dialogs
    if (viewmodel.showPlayerSetup) {
        PlayerSelector(
            onDismiss = { viewmodel.hidePlayerSetupDialog() },
            onFinish = { selectedPlayers ->
                viewmodel.setPlayers(selectedPlayers)
                viewmodel.hidePlayerSetupDialog()
            }
        )
    }

    if (viewmodel.showGameConfig) {
        GameConfigDialog(viewmodel = viewmodel)
    }

    if (viewmodel.showStopGameDialog) {
        StopGameConfirmationDialog(viewmodel = viewmodel)
    }

    // Asked once every player finished - averages only reach the leaderboard on explicit confirmation
    val highscoreUploadState by viewmodel.highscoreUploadState.collectAsStateWithLifecycle()
    HighscoreUploadDialog(
        state = highscoreUploadState,
        onUpload = viewmodel::uploadHighscores,
        onDecline = viewmodel::declineHighscoreUpload,
    )
}

enum class Multiplier {
    SINGLE,
    DOUBLE,
    TRIPLE
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
private fun TopActionRow(viewmodel: DartCounterViewModel) {
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
            onClick = { viewmodel.showPlayerSetupDialog() },
            modifier = buttonModifier,
            enabled = !viewmodel.gameStarted,
            contentPadding = CompactButtonPadding
        ) {
            FittingButtonText(stringResource(Res.string.dartcounter_add_players))
        }

        if (viewmodel.gameStarted) {
            Button(
                onClick = { viewmodel.showStopGameConfirmation() },
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
                onClick = { viewmodel.showGameConfigDialog() },
                modifier = buttonModifier,
                contentPadding = CompactButtonPadding
            ) {
                FittingButtonText(stringResource(Res.string.dartcounter_start_game))
            }
        }

        var showHighscores by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = { showHighscores = true },
            modifier = buttonModifier,
            contentPadding = CompactButtonPadding
        ) {
            FittingButtonText(stringResource(Res.string.dartcounter_highscores))
        }

        if (showHighscores) {
            HighscoresDialog(
                game = GameId.DART_COUNTER,
                initialDifficulty = dartCounterDifficulty(viewmodel.selectedCountdown),
                onDismiss = { showHighscores = false }
            )
        }
    }
}

@Composable
private fun GameStatusOrHint(viewmodel: DartCounterViewModel, modifier: Modifier = Modifier) {
    val game = viewmodel.gameManager
    if (game != null) {
        GameStatusDisplay(game = game, viewmodel = viewmodel, modifier = modifier)
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

private fun DartCounterViewModel.DartThrow.toSegment(): DartSegment {
    val multiplier = if (isTriple) 3 else if (isDouble) 2 else 1
    return DartSegment(score, multiplier, isDouble, isTriple)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DartBoard(viewmodel: DartCounterViewModel, modifier: Modifier = Modifier) {

    var selectedMultiplier by remember { mutableStateOf(Multiplier.SINGLE) }
    val padEnabled = viewmodel.gameManager?.let { !it.gameOver } ?: false
    val padColor = if (selectedMultiplier == Multiplier.SINGLE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val padContentColor = if (selectedMultiplier == Multiplier.SINGLE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary

    Column(modifier = modifier) {
        // Multiplier buttons - the active one is highlighted and relabels the whole pad below
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            val multipliers = listOf(Multiplier.SINGLE, Multiplier.DOUBLE, Multiplier.TRIPLE)
            multipliers.forEachIndexed { index, multiplier ->
                SegmentedButton(
                    selected = selectedMultiplier == multiplier,
                    onClick = { selectedMultiplier = multiplier },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = multipliers.size),
                    // No checkmark: the highlight is enough and the icon steals width from the label
                    icon = {}
                ) {
                    val multiplierText = when (multiplier) {
                        Multiplier.SINGLE -> stringResource(Res.string.dartcounter_multiplier_single)
                        Multiplier.DOUBLE -> stringResource(Res.string.dartcounter_multiplier_double)
                        Multiplier.TRIPLE -> stringResource(Res.string.dartcounter_multiplier_triple)
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
                        Multiplier.SINGLE -> DartSegment.single(base)
                        Multiplier.DOUBLE -> DartSegment.double(base)
                        Multiplier.TRIPLE -> DartSegment.triple(base)
                    }
                    DartPadButton(
                        label = formatSegment(segment),
                        height = cellHeight,
                        containerColor = padColor,
                        contentColor = padContentColor,
                        enabled = padEnabled,
                        onClick = {
                            viewmodel.throwDart(segment)
                            selectedMultiplier = Multiplier.SINGLE
                        }
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
                            onClick = {
                                viewmodel.throwDart(segment)
                                selectedMultiplier = Multiplier.SINGLE
                            }
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
                        enabled = viewmodel.canUndoThrow(),
                        onClick = { viewmodel.undoLastThrow() }
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


private fun threeDartAverage(player: DartCounterViewModel.Player, game: DartCounterViewModel.GameManager): String {
    val totalScore = game.countdown - player.score
    return if (player.totalDartsThrown > 0) {
        val value = totalScore.toDouble() / player.totalDartsThrown * 3
        (round(value * 10) / 10).toString()
    } else {
        "0.0"
    }
}

/** Immutable copy of one scoreboard row, so rows recompose from values instead of mutable game fields. */
private data class DartPlayerRowUi(
    val name: String,
    val score: Int,
    val average: String,
    val dartsThrown: Int,
    val isFinished: Boolean,
    val isCurrent: Boolean,
)

/**
 * The bottom card: the active player's turn (remaining score, darts of this turn and the best
 * checkout path, see DartCheckout.findCheckouts) above the scoreboard of all players.
 */
@Composable
fun GameStatusDisplay(
    game: DartCounterViewModel.GameManager,
    viewmodel: DartCounterViewModel,
    modifier: Modifier = Modifier
) {
    // Game internals are not observable - reading the revision recomposes this card after every throw
    val revision = viewmodel.gameRevision
    val currentPlayer = game.getCurrentPlayer()
    val dartsLeft = viewmodel.dartsLeft
    val turnDarts = remember(revision, currentPlayer) { game.getCurrentTurnDarts() }
    val rows = remember(revision, game) {
        game.playerList.map { player ->
            DartPlayerRowUi(
                name = player.name,
                score = player.score,
                average = threeDartAverage(player, game),
                dartsThrown = player.totalDartsThrown,
                isFinished = player.isFinished,
                isCurrent = player === currentPlayer && !game.gameOver,
            )
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (game.gameOver) {
                val names = game.getWinners().joinToString(
                    separator = stringResource(Res.string.dartcounter_list_separator)
                ) { it.name }
                Text(
                    text = stringResource(Res.string.dartcounter_winners_format, names),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                val highscoreUploadState by viewmodel.highscoreUploadState.collectAsStateWithLifecycle()
                HighscoreUploadDoneText(state = highscoreUploadState)
            } else {
                CurrentTurnHeader(
                    playerName = currentPlayer.name,
                    remainingScore = currentPlayer.score,
                    dartsLeft = dartsLeft,
                    turnDarts = turnDarts,
                    doubleOut = game.doubleOut
                )
            }

            HorizontalDivider()

            val listState = rememberLazyListState()
            // Keep the active player in view
            LaunchedEffect(game.currentPlayerIndex) {
                listState.animateScrollToItem(game.currentPlayerIndex)
            }

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(rows.size) { index ->
                    PlayerScoreRow(row = rows[index])
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
    turnDarts: List<DartCounterViewModel.DartThrow>,
    doubleOut: Boolean,
) {
    val checkouts = remember(remainingScore, dartsLeft, doubleOut) {
        if (dartsLeft > 0) findCheckouts(remainingScore, dartsLeft, doubleOut) else emptyList()
    }

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
                text = stringResource(Res.string.dartcounter_turn_total_format, turnDarts.sumOf { it.actualScore }),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 56.dp),
                textAlign = TextAlign.End
            )
        }

        // Checkout path, only once a finish is within reach this turn (at most 3 short labels per line)
        if (checkouts.isNotEmpty()) {
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
                checkouts.first().forEach { segment ->
                    Text(
                        text = formatSegment(segment),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
            if (checkouts.size > 1) {
                val alternative = checkouts[1].map { formatSegment(it) }.joinToString(" ")
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
private fun PlayerScoreRow(row: DartPlayerRowUi) {
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
                    text = stringResource(Res.string.dartcounter_avg_format, row.average),
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
fun GameConfigDialog(viewmodel: DartCounterViewModel) {
    Dialog(onDismissRequest = { viewmodel.hideGameConfigDialog() }) {
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
                            onClick = { viewmodel.setCountdown(countdown) },
                            modifier = Modifier.weight(1f),
                            contentPadding = CompactButtonPadding,
                            colors = selectableButtonColors(viewmodel.selectedCountdown == countdown)
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
                    val modes = listOf(
                        DartCounterViewModel.OutMode.SINGLE_OUT,
                        DartCounterViewModel.OutMode.DOUBLE_OUT
                    )
                    modes.forEach { mode ->
                        Button(
                            onClick = { viewmodel.setOutMode(mode) },
                            modifier = Modifier.weight(1f),
                            contentPadding = CompactButtonPadding,
                            colors = selectableButtonColors(viewmodel.selectedOutMode == mode)
                        ) {
                            val modeText = when (mode) {
                                DartCounterViewModel.OutMode.SINGLE_OUT -> stringResource(Res.string.dartcounter_out_mode_single_out)
                                DartCounterViewModel.OutMode.DOUBLE_OUT -> stringResource(Res.string.dartcounter_out_mode_double_out)
                            }
                            FittingButtonText(modeText)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(
                        Res.string.dartcounter_players_format,
                        viewmodel.playerNames.joinToString(
                            separator = stringResource(Res.string.dartcounter_list_separator)
                        ) { it }
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (viewmodel.playerNames.isEmpty()) {
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
                        onClick = { viewmodel.hideGameConfigDialog() },
                        modifier = Modifier.weight(1f),
                        contentPadding = CompactButtonPadding
                    ) {
                        FittingButtonText(stringResource(Res.string.dartcounter_cancel))
                    }
                    
                    Button(
                        onClick = { viewmodel.startGame() },
                        modifier = Modifier.weight(1f),
                        contentPadding = CompactButtonPadding,
                        enabled = viewmodel.playerNames.isNotEmpty()
                    ) {
                        FittingButtonText(stringResource(Res.string.dartcounter_start_game))
                    }
                }
            }
        }
    }
}

@Composable
fun StopGameConfirmationDialog(viewmodel: DartCounterViewModel) {
    Dialog(onDismissRequest = { viewmodel.hideStopGameConfirmation() }) {
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
                        onClick = { viewmodel.hideStopGameConfirmation() },
                        modifier = Modifier.weight(1f),
                        contentPadding = CompactButtonPadding
                    ) {
                        FittingButtonText(stringResource(Res.string.dartcounter_cancel))
                    }
                    
                    Button(
                        onClick = { viewmodel.stopGame() },
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

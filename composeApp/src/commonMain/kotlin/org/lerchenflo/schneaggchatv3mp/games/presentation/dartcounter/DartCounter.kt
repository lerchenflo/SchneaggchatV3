package org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter

import androidx.compose.foundation.border
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartSegment
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.findCheckouts
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
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_current_player_format
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
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_no_checkout
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

@Preview(
    showBackground = true,
    showSystemUi = true
)

@Composable
fun DartCounter(
    onBackClick: () -> Unit = {}
) {
    val viewmodel = koinInject<DartCounterViewModel>()

    // todo: he manu du musch noch an backbutton ine tua sunsch sind die Iphone user stuck

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_dartcounter_title),
            onBackClick = onBackClick
        )
        // Top buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewmodel.showPlayerSetupDialog() },
                modifier = Modifier.weight(1f),
                enabled = !viewmodel.gameStarted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!viewmodel.gameStarted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Text(stringResource(Res.string.dartcounter_add_players))
            }

            Button(
                onClick = {
                    if(viewmodel.gameStarted) {
                        viewmodel.showStopGameConfirmation()
                    } else {
                        viewmodel.showGameConfigDialog()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewmodel.playerNames.isEmpty()) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                if(viewmodel.gameStarted){
                    Text(stringResource(Res.string.dartcounter_stop_game))
                }else{
                    Text(stringResource(Res.string.dartcounter_start_game))
                }
            }

            Button(
                onClick = { /* Highscores - leave as is */ },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(Res.string.dartcounter_highscores))
            }
        }

        // Board and score table share the remaining space so the table can never be pushed off screen
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val isWide = maxWidth > 600.dp
            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f)) {
                        GameStatusOrHint(viewmodel = viewmodel, modifier = Modifier.weight(1f))
                    }
                    Column(modifier = Modifier.weight(1.2f)) {
                        CheckoutStrip(viewmodel = viewmodel)
                        DartBoard(viewmodel = viewmodel, modifier = Modifier.weight(1f))
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    CheckoutStrip(viewmodel = viewmodel)
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
}

enum class Multiplier {
    SINGLE,
    DOUBLE,
    TRIPLE
}

@Composable
private fun GameStatusOrHint(viewmodel: DartCounterViewModel, modifier: Modifier = Modifier) {
    viewmodel.gameManager?.let { game ->
        GameStatusDisplay(game = game, viewmodel = viewmodel, modifier = modifier)
    } ?: run {
        Text(
            text = stringResource(Res.string.dartcounter_configure_game_hint),
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier.padding(vertical = 16.dp)
        )
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

/**
 * Shows the active player's best finishing path (differs between single and double out,
 * see DartCheckout.findCheckouts), plus the darts already thrown this turn.
 */
@Composable
fun CheckoutStrip(viewmodel: DartCounterViewModel) {
    val game = viewmodel.gameManager ?: return
    if (game.gameOver) return
    val player = game.getCurrentPlayer()
    if (player.isFinished) return
    val dartsLeft = viewmodel.dartsLeft
    if (dartsLeft <= 0) return

    val checkouts = remember(player.score, dartsLeft, game.doubleOut) {
        findCheckouts(player.score, dartsLeft, game.doubleOut)
    }
    val currentTurnDarts = game.getCurrentTurnDarts()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = player.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(text = player.score.toString(), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = stringResource(Res.string.dartcounter_darts_left_format, dartsLeft),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (currentTurnDarts.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    currentTurnDarts.forEach { dart ->
                        val multiplier = if (dart.isTriple) 3 else if (dart.isDouble) 2 else 1
                        val segment = DartSegment(dart.score, multiplier, dart.isDouble, dart.isTriple)
                        Text(text = formatSegment(segment), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = stringResource(Res.string.dartcounter_turn_total_format, currentTurnDarts.sumOf { it.actualScore }),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = stringResource(Res.string.dartcounter_checkout_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (checkouts.isEmpty()) {
                Text(
                    text = stringResource(Res.string.dartcounter_no_checkout),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    checkouts.first().forEach { segment ->
                        Text(
                            text = formatSegment(segment),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (checkouts.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = stringResource(Res.string.dartcounter_checkout_alt_format), style = MaterialTheme.typography.bodySmall)
                        checkouts[1].forEach { segment ->
                            Text(text = formatSegment(segment), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
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
                .padding(vertical = 8.dp)
        ) {
            val multipliers = listOf(Multiplier.SINGLE, Multiplier.DOUBLE, Multiplier.TRIPLE)
            multipliers.forEachIndexed { index, multiplier ->
                SegmentedButton(
                    selected = selectedMultiplier == multiplier,
                    onClick = { selectedMultiplier = multiplier },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = multipliers.size)
                ) {
                    val multiplierText = when (multiplier) {
                        Multiplier.SINGLE -> stringResource(Res.string.dartcounter_multiplier_single)
                        Multiplier.DOUBLE -> stringResource(Res.string.dartcounter_multiplier_double)
                        Multiplier.TRIPLE -> stringResource(Res.string.dartcounter_multiplier_triple)
                    }
                    Text(multiplierText)
                }
            }
        }

        // Dart board numbers - cell height adapts to the space actually left over so the
        // score table below is never pushed off screen
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val rows = 5
            val spacing = 4.dp
            val cellHeight = ((maxHeight - spacing * (rows - 1)) / rows).coerceIn(34.dp, 72.dp)

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

                // 25, bullseye and miss are not affected by the multiplier - always enabled, always Single-coloured
                item {
                    DartPadButton(
                        label = formatSegment(DartSegment.OUTER_BULL),
                        height = cellHeight,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        enabled = padEnabled,
                        onClick = {
                            viewmodel.throwDart(DartSegment.OUTER_BULL)
                            selectedMultiplier = Multiplier.SINGLE
                        }
                    )
                }
                item {
                    DartPadButton(
                        label = formatSegment(DartSegment.BULLSEYE),
                        height = cellHeight,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        enabled = padEnabled,
                        onClick = {
                            viewmodel.throwDart(DartSegment.BULLSEYE)
                            selectedMultiplier = Multiplier.SINGLE
                        }
                    )
                }
                item {
                    DartPadButton(
                        label = formatSegment(DartSegment.MISS),
                        height = cellHeight,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        enabled = padEnabled,
                        onClick = {
                            viewmodel.throwDart(DartSegment.MISS)
                            selectedMultiplier = Multiplier.SINGLE
                        }
                    )
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
            .padding(2.dp)
            .height(height),
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(text = label, fontSize = 16.sp)
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

@Composable
fun GameStatusDisplay(
    game: DartCounterViewModel.GameManager,
    viewmodel: DartCounterViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(vertical = 7.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(15.dp)
        ) {
            val winners = game.getWinners()
            
            if (winners.isNotEmpty()) {
                val names = winners.joinToString(
                    separator = stringResource(Res.string.dartcounter_list_separator)
                ) { it.name }
                Text(
                    text = stringResource(Res.string.dartcounter_winners_format, names),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (!game.getCurrentPlayer().isFinished) {
                Text(
                    text = stringResource(Res.string.dartcounter_current_player_format, game.getCurrentPlayer().name,
                    viewmodel.throwCount
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // LazyColumn with autoscroll to active player
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            
            // Auto-scroll to current player when it changes
            LaunchedEffect(game.currentPlayerIndex) {
                coroutineScope.launch {
                    listState.animateScrollToItem(game.currentPlayerIndex)
                }
            }
            
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(game.playerList.size) { index ->
                    val player = game.playerList[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(7.dp)
                            .let { modifier ->
                                when {
                                    player.isFinished -> {
                                        modifier.border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = MaterialTheme.shapes.small
                                        )
                                    }
                                    player == game.getCurrentPlayer() && !player.isFinished && !game.gameOver -> {
                                        modifier.border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.secondary,
                                            shape = MaterialTheme.shapes.small
                                        )
                                    }
                                    else -> modifier
                                }
                            }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${player.name}: ${player.score}${if (player.isFinished) stringResource(Res.string.dartcounter_finished_suffix) else ""}",
                                fontSize = 22.sp,
                                fontWeight = when {
                                    player.isFinished -> FontWeight.Bold
                                    player == game.getCurrentPlayer() && !game.gameOver -> FontWeight.Bold
                                    else -> FontWeight.Normal
                                },
                                color = when {
                                    player.isFinished -> MaterialTheme.colorScheme.primary
                                    player == game.getCurrentPlayer() && !game.gameOver -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        
                        val rowColor = when {
                            player.isFinished -> MaterialTheme.colorScheme.primary
                            player == game.getCurrentPlayer() && !game.gameOver -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(Res.string.dartcounter_avg_format, threeDartAverage(player, game)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = rowColor
                            )
                            Text(
                                text = stringResource(Res.string.dartcounter_darts_format, player.totalDartsThrown),
                                style = MaterialTheme.typography.bodySmall,
                                color = rowColor
                            )
                        }
                    }
                }
            }

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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewmodel.selectedCountdown == countdown) 
                                    MaterialTheme.colorScheme.primary else 
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(countdown.toString())
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewmodel.selectedOutMode == mode) 
                                    MaterialTheme.colorScheme.primary else 
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            val modeText = when (mode) {
                                DartCounterViewModel.OutMode.SINGLE_OUT -> stringResource(Res.string.dartcounter_out_mode_single_out)
                                DartCounterViewModel.OutMode.DOUBLE_OUT -> stringResource(Res.string.dartcounter_out_mode_double_out)
                            }
                            Text(modeText)
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.dartcounter_cancel))
                    }
                    
                    Button(
                        onClick = { viewmodel.startGame() },
                        modifier = Modifier.weight(1f),
                        enabled = viewmodel.playerNames.isNotEmpty()
                    ) {
                        Text(stringResource(Res.string.dartcounter_start_game))
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.dartcounter_cancel))
                    }
                    
                    Button(
                        onClick = { viewmodel.stopGame() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(Res.string.dartcounter_stop_game))
                    }
                }
            }
        }
    }
}
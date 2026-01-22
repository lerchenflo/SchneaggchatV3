package org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.compose.ui.window.Dialog
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_add
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_add_players
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_avg_format
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_cancel
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_configure_game_hint
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_countdown_label
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_current_player_format
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_dart_status_format
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_done
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
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_player_name_label
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_please_add_players_first
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_remove
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_stop_game
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_stop_game_confirmation_message
import schneaggchatv3mp.composeapp.generated.resources.dartcounter_start_game
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
        

        
        // Dart board
        DartBoard(viewmodel = viewmodel)

        // Game state display
        viewmodel.gameManager?.let { game ->
            GameStatusDisplay(game = game, viewmodel = viewmodel)
        } ?: run {
            Text(
                text = stringResource(Res.string.dartcounter_configure_game_hint),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
    
    // Dialogs
    if (viewmodel.showPlayerSetup) {
        PlayerSetupDialog(viewmodel = viewmodel)
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
fun DartBoard(viewmodel: DartCounterViewModel) {

    var selectedMultiplier by remember { mutableStateOf(Multiplier.SINGLE) }
    
    Column {
        // Multiplier buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val multipliers = listOf(
                Multiplier.SINGLE,
                Multiplier.DOUBLE,
                Multiplier.TRIPLE
            )
            multipliers.forEach { multiplier ->
                Button(
                    onClick = { selectedMultiplier = multiplier },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMultiplier == multiplier) 
                            MaterialTheme.colorScheme.primary else 
                            MaterialTheme.colorScheme.secondary
                    )
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
        
        // Dart board numbers
        LazyVerticalGrid(
            columns = GridCells.Adaptive(80.dp),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val numbers = (1..20).toList() + listOf(25, 50, 21)
            items(numbers.size) { index ->
                var number = numbers[index]
                val isBullseye = number == 25 || number == 50
                val isMultiplierSelected = selectedMultiplier == Multiplier.DOUBLE || selectedMultiplier == Multiplier.TRIPLE
                val isDisabled = isBullseye && isMultiplierSelected
                
                Button(
                    modifier = Modifier
                        .padding(2.dp)
                        .height(60.dp),
                    onClick = {
                        val isDouble = selectedMultiplier == Multiplier.DOUBLE
                        val isTriple = selectedMultiplier == Multiplier.TRIPLE
                        if (number==21){
                            number = 0
                        }
                        viewmodel.throwDart(number, isDouble, isTriple)
                        // Auto-reset to Single after throwing
                        selectedMultiplier = Multiplier.SINGLE
                    },
                    enabled = viewmodel.gameManager != null && !viewmodel.gameManager!!.gameOver && !isDisabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDisabled) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    if (number == 21) {
                        Text(
                            text = stringResource(Res.string.dartcounter_miss)
                        )

                    } else {
                        Text(
                            text = number.toString(),
                            fontSize = 16.sp,
                            color = if (isDisabled) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            }
                        )
                    }
                }
            }
            
            // Undo button as last item in grid
            item {
                Button(
                    modifier = Modifier
                        .padding(2.dp)
                        .height(60.dp),
                    enabled = viewmodel.canUndoThrow(),
                    onClick = { viewmodel.undoLastThrow() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.dartcounter_undo),
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}


@Composable
fun GameStatusDisplay(game: DartCounterViewModel.GameManager, viewmodel: DartCounterViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
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
                    text = stringResource(Res.string.dartcounter_current_player_format, game.getCurrentPlayer().name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Vertical player list with averages
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                game.playerList.forEach { player ->
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
                        
                        // Three dart average calculation
                        val dartsThrown = player.totalDartsThrown + if (player == game.getCurrentPlayer() && !player.isFinished) viewmodel.throwCount else 0
                        val totalScore = game.countdown - player.score

                        val average = if (dartsThrown > 0) {
                            val value = totalScore.toDouble() / dartsThrown * 3
                            (round(value * 10) / 10).toString()
                        } else {
                            "0.0"
                        }
                        Text(
                            text = stringResource(Res.string.dartcounter_avg_format, average),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                player.isFinished -> MaterialTheme.colorScheme.primary
                                player == game.getCurrentPlayer() && !game.gameOver -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }

            if (viewmodel.currentThrow > -1 && !game.getCurrentPlayer().isFinished) {
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = stringResource(
                        Res.string.dartcounter_dart_status_format,
                        viewmodel.currentThrow,
                        viewmodel.throwCount
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
@Composable
fun PlayerSetupDialog(viewmodel: DartCounterViewModel) {
    var playerName by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = { viewmodel.hidePlayerSetupDialog() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.dartcounter_add_players),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text(stringResource(Res.string.dartcounter_player_name_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Button(
                        onClick = {
                            viewmodel.addPlayerName(playerName)
                            playerName = ""
                        }
                    ) {
                        Text(stringResource(Res.string.dartcounter_add))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(viewmodel.playerNames) { name ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name)
                            Button(
                                onClick = { viewmodel.removePlayerName(name) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(stringResource(Res.string.dartcounter_remove))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { viewmodel.hidePlayerSetupDialog() }
                    ) {
                        Text(stringResource(Res.string.dartcounter_done))
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
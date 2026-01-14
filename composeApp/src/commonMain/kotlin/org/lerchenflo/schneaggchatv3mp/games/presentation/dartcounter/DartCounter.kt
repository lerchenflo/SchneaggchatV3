package org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
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

@Preview(
    showBackground = true,
    showSystemUi = true
)

@Composable
fun DartCounter() {
    val viewmodel = koinInject<DartCounterViewModel>()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                Text("Add Players")
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
                    Text("Stop Game")
                }else{
                    Text("Start Game")
                }
            }

            Button(
                onClick = { /* Highscores - leave as is */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Highscores")
            }
        }
        

        
        // Dart board
        DartBoard(viewmodel = viewmodel)

        // Game state display
        viewmodel.gameManager?.let { game ->
            GameStatusDisplay(game = game, viewmodel = viewmodel)
        } ?: run {
            Text(
                text = "Configure game and add players to start",
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



@Composable
fun DartBoard(viewmodel: DartCounterViewModel) {
    var selectedMultiplier by remember { mutableStateOf("Single") }
    
    Column {
        // Multiplier buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Single", "Double", "Triple").forEach { multiplier ->
                Button(
                    onClick = { selectedMultiplier = multiplier },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMultiplier == multiplier) 
                            MaterialTheme.colorScheme.primary else 
                            MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(multiplier)
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
                val isMultiplierSelected = selectedMultiplier == "Double" || selectedMultiplier == "Triple"
                val isDisabled = isBullseye && isMultiplierSelected
                
                Button(
                    modifier = Modifier
                        .padding(2.dp)
                        .height(60.dp),
                    onClick = {
                        val isDouble = selectedMultiplier == "Double"
                        val isTriple = selectedMultiplier == "Triple"
                        if (number==21){
                            number = 0
                        }
                        viewmodel.throwDart(number, isDouble, isTriple)
                        // Auto-reset to Single after throwing
                        selectedMultiplier = "Single"
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
                            text = "Miss"
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
                Text(
                    text = "Winners: ${winners.joinToString(", ") { it.name }}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (!game.getCurrentPlayer().isFinished) {
                Text(
                    text = "Current Player: ${game.getCurrentPlayer().name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Vertical player list with averages
            Column(
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
                                text = "${player.name}: ${player.score}${if (player.isFinished) " ✓" else ""}",
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
                        val average = if (dartsThrown > 0) String.format("%.1f", totalScore.toDouble() / dartsThrown * 3) else "0.0"
                        
                        Text(
                            text = "Avg: $average",
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
                    text = "Current Throw: ${viewmodel.currentThrow} (Dart ${viewmodel.throwCount}/3)",
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
                    text = "Add Players",
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
                        label = { Text("Player Name") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Button(
                        onClick = {
                            viewmodel.addPlayerName(playerName)
                            playerName = ""
                        }
                    ) {
                        Text("Add")
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
                                Text("Remove")
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
                        Text("Done")
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
                    text = "Game Configuration",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Countdown:",
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
                    text = "Out Mode:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Single Out", "Double Out").forEach { mode ->
                        Button(
                            onClick = { viewmodel.setOutMode(mode) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewmodel.selectedOutMode == mode) 
                                    MaterialTheme.colorScheme.primary else 
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(mode)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Players: ${viewmodel.playerNames.joinToString(", ") { it }}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (viewmodel.playerNames.isEmpty()) {
                    Text(
                        text = "Please add players first",
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
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = { viewmodel.startGame() },
                        modifier = Modifier.weight(1f),
                        enabled = viewmodel.playerNames.isNotEmpty()
                    ) {
                        Text("Start Game")
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
                    text = "Stop Game",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Are you sure you want to stop the current game? All progress will be lost.",
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
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = { viewmodel.stopGame() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Stop Game")
                    }
                }
            }
        }
    }
}
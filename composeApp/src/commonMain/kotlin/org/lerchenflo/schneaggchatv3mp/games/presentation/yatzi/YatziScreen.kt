package org.lerchenflo.schneaggchatv3mp.games.presentation.yatzi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.viewmodel.compose.viewModel
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route

@Composable
fun YatziSetupScreen(
    onBack: () -> Unit,
    onStartGame: () -> Unit,
    viewModel: YatziViewModel = viewModel { YatziViewModel() }
) {

    var newPlayerName by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Yahtzee Setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newPlayerName,
                    onValueChange = { newPlayerName = it },
                    label = { Text("Player Name") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (newPlayerName.isNotBlank()) {
                        viewModel.addPlayer(newPlayerName)
                        newPlayerName = ""
                    }
                }) {
                    Icon(Icons.Default.Add, "Add")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Players:", style = MaterialTheme.typography.titleMedium)
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.players) { player ->
                    Text(
                        text = player.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }
            }
            
            Button(
                onClick = {
                    if (state.players.isNotEmpty()) {
                        onStartGame()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.players.isNotEmpty()
            ) {
                Text("Start Game")
            }
        }
    }
}

@Composable
fun YatziGameScreen(
    onBack: () -> Unit,
    viewModel: YatziViewModel // Injected
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Yahtzee Game") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
         if (state.winner != null) {
             WinnerScreen(state.winner!!, onBack)
         } else if (state.players.isEmpty()) {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                 Text("No active game. Please go back and setup a new game.")
             }
         } else {
             Column(modifier = Modifier.padding(padding)) {
                 // Scorecard - takes most of the space but is scrollable
                 Scorecard(
                     modifier = Modifier.weight(1f),
                     players = state.players,
                     currentPlayerIndex = state.currentPlayerIndex,
                     onCategorySelect = { viewModel.selectCategory(it) },
                     viewModel = viewModel, // passing VM to calculate potential score
                     canScore = state.canScore,
                     state = state
                 )
                 
                 Spacer(modifier = Modifier.height(16.dp))
                 
                 // Game Header (Current Player, Rolls left)
                 state.currentPlayer
                 
                 // Dice Area - fixed size at bottom
                 DiceArea(
                     dice = state.dice,
                     onToggleDie = { viewModel.toggleDie(it) },
                     canRoll = state.canRoll,
                     onRoll = { viewModel.rollDice() },
                     state = state,
                     rollCount = state.currentRollCount
                 )
             }
         }
    }
}

@Composable
fun WinnerScreen(winner: YatziPlayer, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Game Over!", style = MaterialTheme.typography.headlineLarge)
            Text("Winner: ${winner.name}", style = MaterialTheme.typography.headlineMedium)
            Text("Score: ${winner.totalScore}", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) {
                Text("Back to Menu")
            }
        }
    }
}



@Composable
fun Scorecard(
    modifier: Modifier = Modifier,
    players: List<YatziPlayer>,
    currentPlayerIndex: Int,
    onCategorySelect: (YatziCategory) -> Unit,
    viewModel: YatziViewModel,
    canScore: Boolean,
    state: YatziState
) {
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    
    // Auto-scroll to current player when turn changes
    LaunchedEffect(currentPlayerIndex) {
        val playerColumnWidth = 80.dp
        val headerWidth = 120.dp
        val targetOffset = headerWidth + (currentPlayerIndex * playerColumnWidth)
        horizontalScrollState.animateScrollTo(targetOffset.value.toInt())
    }
    
    // Header Row
    Row(
        modifier = modifier
            .verticalScroll(scrollState)
            .horizontalScroll(horizontalScrollState)
    ) {
        Column {
            // Header
            Row(modifier = Modifier.height(50.dp).background(MaterialTheme.colorScheme.surface)) {
                Box(modifier = Modifier.width(120.dp).padding(8.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Category", fontWeight = FontWeight.Bold)
                }
                players.forEachIndexed { index, player ->
                    val isCurrent = index == currentPlayerIndex
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            player.name,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Categories
            YatziCategory.entries.forEach { category ->
                Row(modifier = Modifier.height(35.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    // Category Name
                    Box(modifier = Modifier.width(120.dp).padding(8.dp), contentAlignment = Alignment.CenterStart) {
                        Text(category.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    // Player Scores
                    players.forEachIndexed { index, player ->
                        val isCurrent = index == currentPlayerIndex
                        val score = player.scores[category]
                        val isSelectable = isCurrent && score == null && YatziCategory.selectable.contains(category)
                        
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(50.dp)
                                .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable(enabled = isSelectable && canScore) {
                                     if (isSelectable) onCategorySelect(category)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (score != null) {
                                val textColor = if (category.isUpper) {
                                    val expectedScore = when (category) {
                                        YatziCategory.ONES -> 3
                                        YatziCategory.TWOS -> 6
                                        YatziCategory.THREES -> 9
                                        YatziCategory.FOURS -> 12
                                        YatziCategory.FIVES -> 15
                                        YatziCategory.SIXES -> 18
                                        else -> 0
                                    }
                                    when {
                                        score == expectedScore -> MaterialTheme.colorScheme.onSurface
                                        score > expectedScore -> Color.Green
                                        else -> Color.Red
                                    }
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                                
                                Text(
                                    score.toString(), 
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            } else if (isCurrent && state.currentRollCount > 0) {
                                val potential = viewModel.calculatePotentialScore(category)
                                Text(
                                    potential.toString(),
                                    color = if (isSelectable && canScore) 
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) 
                                    else 
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                
                // Add boundary and sum after upper section (after SIXES)
                if (category == YatziCategory.SIXES) {
                    // Upper Section Sum Row
                    Row(modifier = Modifier.height(35.dp).border(2.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Box(modifier = Modifier.width(120.dp).padding(8.dp), contentAlignment = Alignment.CenterStart) {
                            Text("Upper Sum", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        players.forEachIndexed { index, player ->
                            val isCurrent = index == currentPlayerIndex
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(35.dp)
                                    .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    player.upperScore.toString(), 
                                    fontWeight = FontWeight.Bold,
                                    color = if (player.upperScore >= 63) Color.Green else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    // Bonus Row
                    Row(modifier = Modifier.height(35.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Box(modifier = Modifier.width(120.dp).padding(8.dp), contentAlignment = Alignment.CenterStart) {
                            Text("Bonus", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        players.forEachIndexed { index, player ->
                            val isCurrent = index == currentPlayerIndex
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(35.dp)
                                    .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (player.bonus > 0) "+${player.bonus}" else "0", 
                                    fontWeight = FontWeight.Bold,
                                    color = if (player.bonus > 0) Color.Green else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    // Separator Row - more subtle boundary
                    Row(modifier = Modifier.height(2.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))) {
                        Box(modifier = Modifier.fillMaxWidth()) {}
                    }
                }
            }
            
            // Totals
            Row(modifier = Modifier.height(50.dp).background(MaterialTheme.colorScheme.secondaryContainer)) {
                 Box(modifier = Modifier.width(120.dp).padding(8.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Total", fontWeight = FontWeight.Bold)
                 }
                 players.forEach { player ->
                     Box(modifier = Modifier.width(80.dp).padding(8.dp), contentAlignment = Alignment.Center) {
                         Text(player.totalScore.toString(), fontWeight = FontWeight.Bold)
                     }
                 }
            }
        }
    }
}

@Composable
fun DiceArea(
    dice: List<YatziDie>,
    onToggleDie: (Int) -> Unit,
    canRoll: Boolean,
    onRoll: () -> Unit,
    state: YatziState,
    rollCount: Int
) {
    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dice.forEachIndexed { index, die ->
                DieView(die, onClick = { onToggleDie(index) })
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onRoll,
            enabled = canRoll,
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Text(if (rollCount == 0) "Roll Dice, ${3 - state.currentRollCount} left" else "Roll Again, ${3 - state.currentRollCount} left")
        }
    }
}

@Composable
fun DieView(die: YatziDie, onClick: () -> Unit) {
    val backgroundColor = if (die.isKept) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (die.isKept) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = die.value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = contentColor
        )
    }
}

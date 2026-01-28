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
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector.PlayerSelector
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.yatzi_setup_title
import schneaggchatv3mp.composeapp.generated.resources.yatzi_game_title
import schneaggchatv3mp.composeapp.generated.resources.yatzi_add_manage_players
import schneaggchatv3mp.composeapp.generated.resources.yatzi_players
import schneaggchatv3mp.composeapp.generated.resources.yatzi_start_game
import schneaggchatv3mp.composeapp.generated.resources.yatzi_continue
import schneaggchatv3mp.composeapp.generated.resources.yatzi_restart
import schneaggchatv3mp.composeapp.generated.resources.yatzi_new_game
import schneaggchatv3mp.composeapp.generated.resources.yatzi_game_over
import schneaggchatv3mp.composeapp.generated.resources.yatzi_winner
import schneaggchatv3mp.composeapp.generated.resources.yatzi_score
import schneaggchatv3mp.composeapp.generated.resources.yatzi_back_to_menu
import schneaggchatv3mp.composeapp.generated.resources.yatzi_no_active_game
import schneaggchatv3mp.composeapp.generated.resources.yatzi_category
import schneaggchatv3mp.composeapp.generated.resources.yatzi_upper_sum
import schneaggchatv3mp.composeapp.generated.resources.yatzi_bonus
import schneaggchatv3mp.composeapp.generated.resources.yatzi_total
import schneaggchatv3mp.composeapp.generated.resources.yatzi_roll_dice
import schneaggchatv3mp.composeapp.generated.resources.yatzi_roll_again

@Composable
fun YatziSetupScreen(
    onBack: () -> Unit,
    onStartGame: () -> Unit,
    viewModel: YatziViewModel = viewModel { YatziViewModel() }
) {

    // var newPlayerName by remember { mutableStateOf("") } // Removed local state for player input
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(stringResource(Res.string.yatzi_setup_title)) },
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
                Button(
                    onClick = { viewModel.showPlayerSelector() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.gameStarted
                ) {
                   Text(stringResource(Res.string.yatzi_add_manage_players))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(stringResource(Res.string.yatzi_players), style = MaterialTheme.typography.titleMedium)
            
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
            
            if (!state.gameStarted) {
                Button(
                    onClick = {
                        if (state.players.isNotEmpty()) {
                            viewModel.startGame()
                            onStartGame()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.players.isNotEmpty()
                ) {
                    Text(stringResource(Res.string.yatzi_start_game))
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onStartGame,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.yatzi_continue))
                    }
                    Button(
                        onClick = {
                            viewModel.restartGame()
                            onStartGame()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.yatzi_restart))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.resetAll() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.yatzi_new_game))
                }
            }
        }
        }

    if (state.showPlayerSelector) {
        PlayerSelector(
            onDismiss = { viewModel.hidePlayerSelector() },
            onFinish = { selectedPlayers ->
                viewModel.setPlayers(selectedPlayers)
                viewModel.hidePlayerSelector()
            }
        )
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
                title = { Text(stringResource(Res.string.yatzi_game_title)) },
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
                 Text(stringResource(Res.string.yatzi_no_active_game))
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
            Text(stringResource(Res.string.yatzi_game_over), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(Res.string.yatzi_winner, winner.name), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(Res.string.yatzi_score, winner.totalScore.toString()), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) {
                Text(stringResource(Res.string.yatzi_back_to_menu))
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
                    Text(stringResource(Res.string.yatzi_category), fontWeight = FontWeight.Bold)
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
                            Text(stringResource(Res.string.yatzi_upper_sum), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
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
                            Text(stringResource(Res.string.yatzi_bonus), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
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
                    Text(stringResource(Res.string.yatzi_total), fontWeight = FontWeight.Bold)
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
            Text(if (rollCount == 0) stringResource(Res.string.yatzi_roll_dice, 3 - state.currentRollCount) else stringResource(Res.string.yatzi_roll_again, 3 - state.currentRollCount))
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

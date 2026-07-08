package org.lerchenflo.schneaggchatv3mp.games.presentation.towerstack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameHud
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameOverOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameStartOverlay
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_tower_stack_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_tower_stack_speed_up
import schneaggchatv3mp.composeapp.generated.resources.games_tower_stack_tap_to_place
import schneaggchatv3mp.composeapp.generated.resources.games_stack_tower

@Composable
fun TowerStackScreen(
    viewModel: TowerstackViewModel = koinInject(),
    onBackClick: () -> Unit = {}
) {
    val gameState by viewModel.gameState.collectAsState()
    val colors = MaterialTheme.colorScheme
    var explanationDismissed by rememberSaveable { mutableStateOf(false) }

    // After process death the dismissed flag is restored but the ViewModel run
    // is lost — start a fresh run instead of showing the explanation again.
    LaunchedEffect(Unit) {
        if (explanationDismissed && !gameState.isGameStarted) {
            viewModel.onAction(GameAction.StartGame)
        }
    }

    // Leaving the game ends the run so no loop/counter keeps running in the background
    DisposableEffect(Unit) {
        onDispose { viewModel.onAction(GameAction.ResetGame) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_stack_tower),
            onBackClick = onBackClick
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(colors.background)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (gameState.isGameStarted && !gameState.isGameOver) {
                        viewModel.onAction(GameAction.PlacePlatform)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            GameContent(
                gameState = gameState,
                onStop = {
                    explanationDismissed = false
                    viewModel.onAction(GameAction.ResetGame)
                },
                onRestart = {
                    viewModel.onAction(GameAction.ResetGame)
                    viewModel.onAction(GameAction.StartGame)
                },
                onExit = {
                    viewModel.onAction(GameAction.ResetGame)
                    onBackClick()
                }
            )

            if (!explanationDismissed && !gameState.isGameStarted) {
                GameStartOverlay(
                    title = stringResource(Res.string.games_stack_tower),
                    explanation = stringResource(Res.string.games_tower_stack_instructions),
                    onStart = {
                        explanationDismissed = true
                        viewModel.onAction(GameAction.StartGame)
                    }
                )
            }
        }
    }
}

@Composable
private fun GameContent(
    gameState: GameState,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Game canvas - now uses full available space
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 80.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Scale game coordinates to canvas size
            val scaleX = canvasWidth / 300f
            val scaleY = canvasHeight / 500f
            
            // Draw placed platforms with rainbow colors
            gameState.platforms.forEachIndexed { index, platform ->
                val color = getRainbowColor(index)
                drawRoundedPlatform(
                    platform = platform,
                    color = color,
                    scaleX = scaleX,
                    scaleY = scaleY
                )
            }
            
            // Draw current moving platform
            gameState.currentPlatform?.let { platform ->
                val color = if (gameState.isGameOver) 
                    Color.Red 
                else 
                    getRainbowColor(gameState.platforms.size)
                drawRoundedPlatform(
                    platform = platform,
                    color = color,
                    scaleX = scaleX,
                    scaleY = scaleY
                )
            }
        }
        
        // Unified time/points counter with stop button
        if (gameState.isGameStarted) {
            GameHud(
                score = gameState.score.toLong(),
                timeMillis = gameState.elapsedMillis,
                onStop = onStop,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }

        // Speed increase indicator
        if (gameState.score > 0 && gameState.score % 5 == 0) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = colors.tertiary.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.games_tower_stack_speed_up),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onTertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        
        // Unified game over overlay with restart, difficulty selection and highscores
        if (gameState.isGameOver) {
            GameOverOverlay(
                game = GameId.TOWERSTACK,
                finalScore = gameState.score.toLong(),
                finalTimeMillis = gameState.elapsedMillis,
                onRestart = onRestart,
                onExit = onExit
            )
        }
        
        // Instructions for first move
        if (gameState.isGameStarted && gameState.score == 0 && !gameState.isGameOver) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = colors.secondaryContainer.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.games_tower_stack_tap_to_place),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawRoundedPlatform(
    platform: Platform,
    color: Color,
    scaleX: Float,
    scaleY: Float
) {
    val scaledX = platform.x * scaleX
    val scaledY = platform.y * scaleY
    val scaledWidth = platform.width * scaleX
    val scaledHeight = maxOf(platform.height * scaleY, 4f) // Minimum thickness for visibility
    
    drawRoundRect(
        color = color,
        topLeft = Offset(scaledX, scaledY),
        size = Size(scaledWidth, scaledHeight),
        cornerRadius = CornerRadius(scaledHeight / 2f) // Rounded corners based on thickness
    )
}

private fun getRainbowColor(index: Int): Color {
    val colors = listOf(
        Color(0xFFFF0000), // Red
        Color(0xFFFF7F00), // Orange
        Color(0xFFFFFF00), // Yellow
        Color(0xFF00FF00), // Green
        Color(0xFF0000FF), // Blue
        Color(0xFF4B0082), // Indigo
        Color(0xFF9400D3)  // Violet
    )
    return colors[index % colors.size]
}


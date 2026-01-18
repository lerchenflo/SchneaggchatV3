package org.lerchenflo.schneaggchatv3mp.games.presentation.towerstack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.domain.GameAction
import org.lerchenflo.schneaggchatv3mp.games.domain.GameState

@Composable
fun TowerStackScreen(
    viewModel: TowerstackViewModel = koinViewModel()
) {
    val gameState by viewModel.gameState.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable {
                if (!gameState.isGameStarted) {
                    viewModel.onAction(GameAction.StartGame)
                } else if (!gameState.isGameOver) {
                    viewModel.onAction(GameAction.PlacePlatform)
                } else {
                    viewModel.onAction(GameAction.ResetGame)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (!gameState.isGameStarted) {
            StartScreen()
        } else {
            GameContent(
                gameState = gameState,
                onReset = { viewModel.onAction(GameAction.ResetGame) }
            )
        }
    }
}

@Composable
private fun StartScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Tower Stack",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Tap to start",
            fontSize = 18.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Stack the platforms as high as you can!\n\nTap to place each platform.\nPerfect alignment increases your score.",
            fontSize = 14.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GameContent(
    gameState: GameState,
    onReset: () -> Unit
) {
    Box {
        // Game canvas
        Canvas(
            modifier = Modifier
                .width(300.dp)
                .height(500.dp)
                .background(Color.White)
        ) {
            // Draw placed platforms
            gameState.platforms.forEach { platform ->
                drawRect(
                    color = Color.Blue,
                    topLeft = Offset(platform.x, platform.y),
                    size = androidx.compose.ui.geometry.Size(platform.width, platform.height)
                )
            }
            
            // Draw current moving platform
            gameState.currentPlatform?.let { platform ->
                drawRect(
                    color = if (gameState.isGameOver) Color.Red else Color.Green,
                    topLeft = Offset(platform.x, platform.y),
                    size = androidx.compose.ui.geometry.Size(platform.width, platform.height)
                )
            }
        }
        
        // Score display
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            Text(
                text = "Score: ${gameState.score}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            if (gameState.score > 0 && gameState.score % 5 == 0) {
                Text(
                    text = "Speed increased!",
                    fontSize = 12.sp,
                    color = Color.Yellow,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        // Game over overlay
        if (gameState.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Game Over!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Final Score: ${gameState.score}",
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onReset,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Play Again")
                    }
                }
            }
        }
        
        // Instructions for first move
        if (gameState.isGameStarted && gameState.score == 0 && !gameState.isGameOver) {
            Text(
                text = "Tap to place!",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}


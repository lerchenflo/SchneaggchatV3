package org.lerchenflo.schneaggchatv3mp.games.presentation.towerstack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle

@Composable
fun TowerStackScreen(
    viewModel: TowerstackViewModel = koinInject(),
    onBackClick: () -> Unit = {}
) {
    val gameState by viewModel.gameState.collectAsState()
    val colors = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ActivityTitle(
            title = "Tower Stack",
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
}

@Composable
private fun StartScreen() {
    val colors = MaterialTheme.colorScheme
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = "Tower Stack",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎮",
                    fontSize = 48.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Tap to Start",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Stack the platforms as high as you can!\n\nTap to place each platform.\nPerfect alignment increases your score.\nSpeed increases every 5 points!",
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun GameContent(
    gameState: GameState,
    onReset: () -> Unit
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
        
        // Score display with modern design
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(
                containerColor = colors.surfaceVariant.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Score",
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${gameState.score}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant
                )
            }
        }
        
        // Speed increase indicator
        if (gameState.score > 0 && gameState.score % 5 == 0) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = colors.tertiary.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "⚡ Speed Up!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onTertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        
        // Game over overlay with modern design
        if (gameState.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "💥",
                            fontSize = 64.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Game Over!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Final Score: ${gameState.score}",
                            fontSize = 18.sp,
                            color = colors.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = onReset,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                                //.pointerInput(block = Unit),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Play Again",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
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
                    text = "👆 Tap to place!",
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


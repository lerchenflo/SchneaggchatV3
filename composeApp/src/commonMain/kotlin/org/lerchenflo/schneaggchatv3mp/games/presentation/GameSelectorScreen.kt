package org.lerchenflo.schneaggchatv3mp.games.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.difficulty
import schneaggchatv3mp.composeapp.generated.resources.games_daily_section
import schneaggchatv3mp.composeapp.generated.resources.games_without_highscores
import schneaggchatv3mp.composeapp.generated.resources.show_global_ranking
import schneaggchatv3mp.composeapp.generated.resources.show_highscores
import schneaggchatv3mp.composeapp.generated.resources.tools_and_games

@Composable
fun GameSelectorScreen(
    onBackClick: () -> Unit,
    onGameSelection: (Route) -> Unit,
    viewModel: GameSelectorViewModel
){
    val gamesList = viewModel.gamesList

    val dev = SessionCache.requireLoggedIn()?.developer ?: return

    var highscoreGame by remember { mutableStateOf<GameId?>(null) }
    var showGlobalRanking by remember { mutableStateOf(false) }

    Column{
        ActivityTitle(
            title = stringResource(Res.string.tools_and_games),
            onBackClick = onBackClick
        )

        Text(
            text = stringResource(Res.string.difficulty),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DifficultySelector(
                selected = GameDifficultySelection.selected,
                onSelect = { GameDifficultySelection.selected = it },
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { showGlobalRanking = true }) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = stringResource(Res.string.show_global_ranking),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        val visibleGames = gamesList.filter { !it.inDev || dev }
        val (dailyGames, regularGames) = visibleGames.partition { it.daily }
        val (leaderboardGames, otherGames) = regularGames.partition { it.gameId != null }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
        ) {
            if (dailyGames.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.games_daily_section),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                items(dailyGames) { game ->
                    GameElementView(
                        icon = game.icon,
                        text = stringResource(game.title),
                        subtext = game.description?.let { stringResource(it) },
                        onClick = { onGameSelection(game.route) },
                        rightSideIcon = {
                            if (game.gameId != null) {
                                IconButton(onClick = { highscoreGame = game.gameId }) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = stringResource(Res.string.show_highscores),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
                }
            }

            items(leaderboardGames) { game ->
                GameElementView(
                    icon = game.icon,
                    text = stringResource(game.title),
                    subtext = game.description?.let { stringResource(it) },
                    onClick = { onGameSelection(game.route) },
                    rightSideIcon = {
                        IconButton(onClick = { highscoreGame = game.gameId }) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = stringResource(Res.string.show_highscores),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (otherGames.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = stringResource(Res.string.games_without_highscores),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                items(otherGames) { game ->
                    GameElementView(
                        icon = game.icon,
                        text = stringResource(game.title),
                        subtext = game.description?.let { stringResource(it) },
                        onClick = { onGameSelection(game.route) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    highscoreGame?.let { game ->
        HighscoresDialog(
            game = game,
            initialDifficulty = GameDifficultySelection.selected,
            onDismiss = { highscoreGame = null }
        )
    }

    if (showGlobalRanking) {
        GlobalRankingDialog(onDismiss = { showGlobalRanking = false })
    }
}

@Composable
fun GameElementView(
    icon: ImageVector,
    text: String,
    subtext: String? = null,
    onClick: () -> Unit,
    rightSideIcon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
    }
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.5f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container with background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (subtext != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right side icon
            rightSideIcon()
        }
    }
}
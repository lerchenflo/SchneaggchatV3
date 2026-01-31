package org.lerchenflo.schneaggchatv3mp.games.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.highscores_not_implemented_warning
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_infinite_custom_and_selected_answers_warning
import schneaggchatv3mp.composeapp.generated.resources.poll_settings_infinite_custom_answers_warning
import schneaggchatv3mp.composeapp.generated.resources.tools_and_games

@Composable
fun GameSelectorScreen(
    onBackClick: () -> Unit,
    onGameSelection: (Route) -> Unit,
    gamesList: List<GameScreenElement>

){
    Column{
        ActivityTitle(
            title = stringResource(Res.string.tools_and_games),
            onBackClick = onBackClick
        )

        Box(
            modifier = Modifier
                .padding(4.dp)
                .background(
                    color = Color(red = 255, green = 165, blue = 0),
                    shape = RoundedCornerShape(15.dp)
                )
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = stringResource(Res.string.highscores_not_implemented_warning),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(4.dp)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
        ) {
            items(gamesList) { game ->
                if (game.inDev) {
                    if (SessionCache.developer) {
                        GameElementView(
                            icon = game.icon,
                            text = game.title,
                            subtext = game.description,
                            onClick = { onGameSelection(game.route) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    GameElementView(
                        icon = game.icon,
                        text = game.title,
                        subtext = game.description,
                        onClick = { onGameSelection(game.route) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                }
            }
        }
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
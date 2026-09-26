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
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import io.github.lerchenflo.taptarget.tapTarget
import org.lerchenflo.schneaggchatv3mp.games.domain.BoardAxis
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.leaderboard
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.formatCountdown
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.rememberCountdownMillis
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.difficulty
import schneaggchatv3mp.composeapp.generated.resources.games_daily_section
import schneaggchatv3mp.composeapp.generated.resources.games_difficulty_info_countdown
import schneaggchatv3mp.composeapp.generated.resources.games_difficulty_info_global
import schneaggchatv3mp.composeapp.generated.resources.games_difficulty_info_hint
import schneaggchatv3mp.composeapp.generated.resources.games_difficulty_info_language
import schneaggchatv3mp.composeapp.generated.resources.games_tab_multiplayer
import schneaggchatv3mp.composeapp.generated.resources.games_tab_solo
import schneaggchatv3mp.composeapp.generated.resources.games_tab_tools
import schneaggchatv3mp.composeapp.generated.resources.games_without_highscores
import schneaggchatv3mp.composeapp.generated.resources.show_global_ranking
import schneaggchatv3mp.composeapp.generated.resources.show_highscores
import schneaggchatv3mp.composeapp.generated.resources.tools_and_games
import kotlin.time.Clock

private fun timeRemainingUntilNextMidnight(): Long {
    val tz = TimeZone.currentSystemDefault()
    val now = Clock.System.now()
    val today = now.toLocalDateTime(tz).date
    val tomorrow = today.plus(DatePeriod(days = 1))
    val nextMidnight = tomorrow.atStartOfDayIn(tz)
    return (nextMidnight.toEpochMilliseconds() - now.toEpochMilliseconds()).coerceAtLeast(0L)
}

@Composable
private fun dailyChallengeTimeRemainingText(): String {
    val timeRemaining = rememberCountdownMillis(key = Unit) { timeRemainingUntilNextMidnight() }
    return formatCountdown(timeRemaining)
}

@Composable
fun GameSelectorScreen(
    onBackClick: () -> Unit,
    onGameSelection: (Route) -> Unit,
    viewModel: GameSelectorViewModel
){
    val gamesList = viewModel.gamesList

    SessionCache.authStateValue // reactive read: recompose once autologin finishes instead of staying blank
    val dev = SessionCache.requireLoggedIn()?.developer ?: return

    var highscoreGame by remember { mutableStateOf<GameId?>(null) }
    var showGlobalRanking by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(SelectorTab.SOLO) }

    Column{
        ActivityTitle(
            title = stringResource(Res.string.tools_and_games),
            onBackClick = onBackClick,
            showBackButton = false
        )

        SelectorTabSwitch(
            selected = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // sharedDevice already marks exactly the games several people play on one phone
        val (multiplayerGames, soloGames) = gamesList.partition { it.gameId?.sharedDevice == true }

        when (selectedTab) {
            SelectorTab.SOLO -> GamesTabContent(
                gamesList = soloGames,
                dev = dev,
                onGameSelection = onGameSelection,
                onShowGlobalRanking = { showGlobalRanking = true },
                onShowHighscores = { highscoreGame = it },
            )

            // No difficulty row: all three shared-device boards ignore it, and the global ranking
            // deliberately excludes them, so neither control would mean anything here
            SelectorTab.MULTIPLAYER -> GamesTabContent(
                gamesList = multiplayerGames,
                dev = dev,
                onGameSelection = onGameSelection,
                onShowGlobalRanking = { showGlobalRanking = true },
                onShowHighscores = { highscoreGame = it },
                showDifficultyRow = false,
            )

            SelectorTab.TOOLS -> ToolsTabContent(
                toolsList = viewModel.toolsList,
                dev = dev,
                onToolSelection = onGameSelection,
            )
        }
    }

    highscoreGame?.let { game ->
        HighscoresDialog(
            game = game,
            initialDifficulty = game.initialBoard(),
            onDismiss = { highscoreGame = null }
        )
    }

    if (showGlobalRanking) {
        GlobalRankingDialog(onDismiss = { showGlobalRanking = false })
    }
}

/**
 * The card's difficulty marker. Signal bars for games the app-wide chips really govern - rising with
 * the selection, so the cards follow the chips above - and a tune icon for games whose axis is chosen
 * inside the game instead. Games with no axis at all (and games without a leaderboard) get nothing.
 * The wording that used to be a text line survives as the content description.
 */
@Composable
private fun GameDifficultyIcon(gameId: GameId?) {
    val axis = gameId?.leaderboard?.boardAxis ?: return
    val icon = when (axis) {
        BoardAxis.DIFFICULTY -> when (GameDifficultySelection.selected) {
            GameDifficulty.LOW -> Icons.Default.SignalCellularAlt1Bar
            GameDifficulty.MEDIUM -> Icons.Default.SignalCellularAlt2Bar
            GameDifficulty.HIGH -> Icons.Default.SignalCellularAlt
        }
        BoardAxis.LANGUAGE, BoardAxis.DART_COUNTDOWN -> Icons.Default.Tune
        BoardAxis.NONE -> return
    }
    val description = when (axis) {
        BoardAxis.DIFFICULTY -> stringResource(
            Res.string.games_difficulty_info_global,
            stringResource(GameDifficultySelection.selected.stringRes())
        )
        BoardAxis.LANGUAGE -> stringResource(Res.string.games_difficulty_info_language)
        BoardAxis.DART_COUNTDOWN -> stringResource(Res.string.games_difficulty_info_countdown)
        BoardAxis.NONE -> return
    }

    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
fun GameElementView(
    icon: ImageVector,
    text: String,
    subtext: String? = null,
    /** Small marker shown at the end of the row, before [rightSideIcon]. */
    badgeIcon: @Composable () -> Unit = {},
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

            badgeIcon()

            // Right side icon
            rightSideIcon()
        }
    }
}


/**
 * The board the highscores dialog should open on. The app-wide chips only mean a difficulty for
 * games keyed by one - for a language or a dart countdown they would silently pick English or 501,
 * so those games open on their own first board instead.
 */
private fun GameId.initialBoard(): GameDifficulty {
    val spec = leaderboard
    return if (spec.boardAxis == BoardAxis.DIFFICULTY) GameDifficultySelection.selected
    else spec.boards.first()
}

/** Games and tools share one screen; the switch mirrors the shared-content tabs in chat. */
@Composable
private fun SelectorTabSwitch(
    selected: SelectorTab,
    onSelect: (SelectorTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = SelectorTab.entries

    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        tabs.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size)
            ) {
                Text(text = stringResource(tab.labelRes()), maxLines = 1)
            }
        }
    }
}

private enum class SelectorTab { SOLO, MULTIPLAYER, TOOLS }

private fun SelectorTab.labelRes(): StringResource = when (this) {
    SelectorTab.SOLO -> Res.string.games_tab_solo
    SelectorTab.MULTIPLAYER -> Res.string.games_tab_multiplayer
    SelectorTab.TOOLS -> Res.string.games_tab_tools
}

/**
 * The games half: the app-wide difficulty, the daily challenges and every game with a leaderboard.
 * [onShowHighscores] takes a nullable id because the list is partitioned on it rather than
 * smart-cast, and a null simply opens no dialog.
 */
@Composable
private fun GamesTabContent(
    gamesList: List<GameScreenElement>,
    dev: Boolean,
    onGameSelection: (Route) -> Unit,
    onShowGlobalRanking: () -> Unit,
    onShowHighscores: (GameId?) -> Unit,
    showDifficultyRow: Boolean = true,
) {
    Column {
        if (showDifficultyRow) {
            Text(
                text = stringResource(Res.string.difficulty),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DifficultySelector(
                    selected = GameDifficultySelection.selected,
                    onSelect = { GameDifficultySelection.selected = it },
                    modifier = Modifier.weight(1f).tapTarget("games_difficulty_selector")
                )

                IconButton(
                    onClick = onShowGlobalRanking,
                    modifier = Modifier.tapTarget("games_global_ranking_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = stringResource(Res.string.show_global_ranking),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // The chips above only reach games whose board really is keyed by difficulty
            Text(
                text = stringResource(Res.string.games_difficulty_info_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
            )
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
                        text = stringResource(Res.string.games_daily_section, dailyChallengeTimeRemainingText()),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }

                items(dailyGames) { game ->
                    GameElementView(
                        icon = game.icon,
                        text = stringResource(game.title),
                        subtext = game.description?.let { stringResource(it) },
                        badgeIcon = { GameDifficultyIcon(game.gameId) },
                        onClick = { onGameSelection(game.route) },
                        rightSideIcon = {
                            if (game.gameId != null) {
                                IconButton(onClick = { onShowHighscores(game.gameId) }) {
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
                    badgeIcon = { GameDifficultyIcon(game.gameId) },
                    onClick = { onGameSelection(game.route) },
                    rightSideIcon = {
                        IconButton(onClick = { onShowHighscores(game.gameId) }) {
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
                        badgeIcon = { GameDifficultyIcon(game.gameId) },
                        onClick = { onGameSelection(game.route) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/** The tools half: utilities with no run, no score and therefore no difficulty and no trophy. */
@Composable
private fun ToolsTabContent(
    toolsList: List<GameScreenElement>,
    dev: Boolean,
    onToolSelection: (Route) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
    ) {
        items(toolsList.filter { !it.inDev || dev }) { tool ->
            GameElementView(
                icon = tool.icon,
                text = stringResource(tool.title),
                subtext = tool.description?.let { stringResource(it) },
                onClick = { onToolSelection(tool.route) }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

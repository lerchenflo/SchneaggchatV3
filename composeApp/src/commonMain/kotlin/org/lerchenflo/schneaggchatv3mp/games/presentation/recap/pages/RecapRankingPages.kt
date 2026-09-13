package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.RankedRowUi
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageTheme
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageThemes
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPalette
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_betatester_clean_record
import schneaggchatv3mp.composeapp.generated.resources.recap_betatester_exception_count
import schneaggchatv3mp.composeapp.generated.resources.recap_betatester_title
import schneaggchatv3mp.composeapp.generated.resources.recap_betatester_your_rank
import schneaggchatv3mp.composeapp.generated.resources.recap_games_rank
import schneaggchatv3mp.composeapp.generated.resources.recap_games_score
import schneaggchatv3mp.composeapp.generated.resources.recap_games_title
import schneaggchatv3mp.composeapp.generated.resources.recap_leaderboard_messages
import schneaggchatv3mp.composeapp.generated.resources.recap_leaderboard_not_ranked
import schneaggchatv3mp.composeapp.generated.resources.recap_leaderboard_title
import schneaggchatv3mp.composeapp.generated.resources.recap_map_all_time
import schneaggchatv3mp.composeapp.generated.resources.recap_map_contributions
import schneaggchatv3mp.composeapp.generated.resources.recap_map_created
import schneaggchatv3mp.composeapp.generated.resources.recap_map_edited
import schneaggchatv3mp.composeapp.generated.resources.recap_map_leaderboard_title
import schneaggchatv3mp.composeapp.generated.resources.recap_map_not_ranked
import schneaggchatv3mp.composeapp.generated.resources.recap_map_title
import schneaggchatv3mp.composeapp.generated.resources.recap_map_your_rank
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_friends
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_messages
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_rank_errors
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_rank_map
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_rank_messages
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_reactions
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_streak
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_subtitle
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_title
import schneaggchatv3mp.composeapp.generated.resources.recap_password_reset_label
import schneaggchatv3mp.composeapp.generated.resources.recap_password_reset_subtitle
import schneaggchatv3mp.composeapp.generated.resources.recap_password_reset_this_year
import schneaggchatv3mp.composeapp.generated.resources.recap_password_title
import schneaggchatv3mp.composeapp.generated.resources.recap_rank_number
import schneaggchatv3mp.composeapp.generated.resources.recap_your_rank

// Story pages built around a global ranking (messages, map, games, exceptions), the password
// reset gag, and the shareable outro summary.

/** Ranked list shared by every leaderboard page - the own row is highlighted through the theme. */
@Composable
private fun RankList(
    rows: List<RankedRowUi>,
    visible: Boolean,
    firstRevealIndex: Int,
    theme: RecapPageTheme,
    valueText: (RankedRowUi) -> String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEachIndexed { index, row ->
            RevealItem(visible, firstRevealIndex + index) {
                RankRow(row = row, valueText = valueText(row), theme = theme)
            }
        }
    }
}

@Composable
fun RecapLeaderboardPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Leaderboard
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_leaderboard_title), color = theme.accent)
        }
        Spacer(Modifier.height(20.dp))
        val rank = recap.myRank
        if (rank != null) {
            RevealItem(visible, 1) {
                RecapBody(
                    text = stringResource(Res.string.recap_your_rank),
                    color = theme.onBackground,
                    fontSize = 19.sp
                )
            }
            RevealItem(visible, 2) {
                RecapBigText(text = stringResource(Res.string.recap_rank_number, rank), color = theme.accent)
            }
            RevealItem(visible, 3) {
                RecapBody(
                    text = stringResource(Res.string.recap_leaderboard_messages, formatCount(recap.myLeaderboardMessageCount)),
                    color = theme.onBackground.copy(alpha = 0.75f),
                    fontSize = 17.sp
                )
            }
        } else {
            RevealItem(visible, 1) {
                RecapBody(
                    text = stringResource(Res.string.recap_leaderboard_not_ranked),
                    color = theme.onBackground.copy(alpha = 0.85f),
                    fontSize = 20.sp
                )
            }
        }
        if (recap.leaderboardTop.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            RankList(
                rows = recap.leaderboardTop,
                visible = visible,
                firstRevealIndex = 4,
                theme = theme,
                valueText = { formatCount(it.count) }
            )
        }
    }
}

@Composable
fun RecapMapPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Map
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_map_title), color = theme.accent)
        }
        Spacer(Modifier.height(24.dp))
        RecapBigNumber(target = recap.mapEntriesCreated, running = visible, color = theme.onBackground)
        RevealItem(visible, 1) {
            RecapBody(
                text = stringResource(Res.string.recap_map_created),
                color = theme.onBackground.copy(alpha = 0.8f),
                fontSize = 22.sp
            )
        }
        Spacer(Modifier.height(16.dp))
        RevealItem(visible, 2) {
            PillFlow(
                pills = listOf(
                    stringResource(Res.string.recap_map_edited, recap.mapEntriesEdited.toInt()),
                    stringResource(Res.string.recap_map_all_time, recap.mapEntriesCreatedAllTime.toInt())
                ),
                theme = theme
            )
        }
    }
}

/** Global "who shaped the map most" ranking - the own rank leads, top contributors follow. */
@Composable
fun RecapMapLeaderboardPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Map
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_map_leaderboard_title), color = theme.accent, maxFontSize = 36.sp)
        }
        Spacer(Modifier.height(20.dp))
        val rank = recap.myMapRank
        if (rank != null) {
            RevealItem(visible, 1) {
                RecapBody(
                    text = stringResource(Res.string.recap_map_your_rank),
                    color = theme.onBackground,
                    fontSize = 19.sp
                )
            }
            RevealItem(visible, 2) {
                RecapBigText(text = stringResource(Res.string.recap_rank_number, rank), color = theme.accent)
            }
            RevealItem(visible, 3) {
                RecapBody(
                    text = stringResource(Res.string.recap_map_contributions, formatCount(recap.myMapContributions)),
                    color = theme.onBackground.copy(alpha = 0.75f),
                    fontSize = 17.sp
                )
            }
        } else {
            RevealItem(visible, 1) {
                RecapBody(
                    text = stringResource(Res.string.recap_map_not_ranked),
                    color = theme.onBackground.copy(alpha = 0.85f),
                    fontSize = 20.sp
                )
            }
        }
        if (recap.mapLeaderboardTop.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            RankList(
                rows = recap.mapLeaderboardTop,
                visible = visible,
                firstRevealIndex = 4,
                theme = theme,
                valueText = { formatCount(it.count) }
            )
        }
    }
}

@Composable
fun RecapGamesPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Games
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_games_title), color = theme.accent)
        }
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            recap.games.forEachIndexed { index, game ->
                RevealItem(visible, index + 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.onBackground.copy(alpha = 0.10f))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${game.gameName} · ${game.difficulty}",
                                color = theme.onBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(Res.string.recap_games_score, formatCount(game.bestScore)),
                                color = theme.onBackground.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(Res.string.recap_games_rank, game.rank),
                            color = if (game.rank <= 3) theme.secondary else theme.accent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecapBetaTesterPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.BetaTester
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_betatester_title), color = theme.accent)
        }
        Spacer(Modifier.height(20.dp))
        val rank = recap.myBetaTesterRank
        if (rank != null) {
            RevealItem(visible, 1) {
                RecapBody(
                    text = stringResource(Res.string.recap_betatester_your_rank),
                    color = theme.onBackground,
                    fontSize = 19.sp
                )
            }
            RevealItem(visible, 2) {
                RecapBigText(text = stringResource(Res.string.recap_rank_number, rank), color = theme.accent)
            }
            RevealItem(visible, 3) {
                RecapBody(
                    text = stringResource(Res.string.recap_betatester_exception_count, formatCount(recap.myExceptionCount)),
                    color = theme.onBackground.copy(alpha = 0.75f),
                    fontSize = 17.sp
                )
            }
        } else {
            RevealItem(visible, 1) {
                RecapBody(
                    text = stringResource(Res.string.recap_betatester_clean_record),
                    color = theme.onBackground.copy(alpha = 0.85f),
                    fontSize = 20.sp
                )
            }
        }
        if (recap.betaTesterRows.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            // The full list can be long; the page scrolls, so cap the reveal stagger at row 5 -
            // otherwise the last rows would only fade in seconds after the page opened.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recap.betaTesterRows.forEachIndexed { index, row ->
                    RevealItem(visible, 4 + index.coerceAtMost(5)) {
                        RankRow(row = row, valueText = formatCount(row.count), theme = theme)
                    }
                }
            }
        }
    }
}

@Composable
fun RecapPasswordResetPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Password
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_password_title), color = theme.accent, maxFontSize = 36.sp)
        }
        Spacer(Modifier.height(24.dp))
        RecapBigNumber(target = recap.passwordResetEmailsSentAllTime, running = visible, color = theme.onBackground)
        RevealItem(visible, 1) {
            RecapBody(
                text = stringResource(Res.string.recap_password_reset_label),
                color = theme.onBackground.copy(alpha = 0.8f),
                fontSize = 20.sp
            )
        }
        if (recap.passwordResetEmailsSentThisYear > 0) {
            Spacer(Modifier.height(12.dp))
            RevealItem(visible, 2) {
                RecapBody(
                    text = stringResource(Res.string.recap_password_reset_this_year, recap.passwordResetEmailsSentThisYear.toInt()),
                    color = theme.accent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        RevealItem(visible, 3) {
            RecapBody(
                text = stringResource(Res.string.recap_password_reset_subtitle),
                color = theme.onBackground.copy(alpha = 0.6f),
                fontSize = 15.sp
            )
        }
    }
}

// Value/label/color for one shareable summary tile - color echoes the accent of the page the
// stat originally came from, so the recognizable palette carries over into the screenshot.
private data class OutroTileData(val value: String, val label: String, val valueColor: Color)

@Composable
fun RecapOutroPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Outro
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_outro_title, recap.username), color = theme.onBackground, maxFontSize = 36.sp)
        }
        Spacer(Modifier.height(28.dp))

        // Most important stats (streak, global ranks) first so they land in the top rows
        // of the tile grid - this page is meant to be screenshotted, so priority order matters.
        val tiles = buildList {
            add(OutroTileData(recap.longestStreakDays.toString(), stringResource(Res.string.recap_outro_streak), RecapPalette.Pink))
            recap.myRank?.let { rank ->
                add(OutroTileData(stringResource(Res.string.recap_rank_number, rank), stringResource(Res.string.recap_outro_rank_messages), RecapPalette.Gold))
            }
            recap.myMapRank?.let { rank ->
                add(OutroTileData(stringResource(Res.string.recap_rank_number, rank), stringResource(Res.string.recap_outro_rank_map), RecapPalette.Orange))
            }
            recap.myBetaTesterRank?.let { rank ->
                add(OutroTileData(stringResource(Res.string.recap_rank_number, rank), stringResource(Res.string.recap_outro_rank_errors), RecapPalette.Coral))
            }
            add(OutroTileData(formatCount(recap.messagesSent), stringResource(Res.string.recap_outro_messages), RecapPalette.Green))
            add(OutroTileData(formatCount(recap.friendsCount.toLong()), stringResource(Res.string.recap_outro_friends), RecapPalette.Lemon))
            add(OutroTileData(formatCount(recap.reactionsGiven), stringResource(Res.string.recap_outro_reactions), RecapPalette.Lilac))
        }

        val rows = tiles.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rows.forEachIndexed { rowIndex, rowTiles ->
                RevealItem(visible, rowIndex + 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        rowTiles.forEach { tile ->
                            OutroStat(tile.value, tile.label, tile.valueColor, theme, Modifier.weight(1f))
                        }
                        if (rowTiles.size < 2) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(36.dp))
        RevealItem(visible, rows.size + 1) {
            RecapBody(
                text = stringResource(Res.string.recap_outro_subtitle),
                color = theme.onBackground.copy(alpha = 0.9f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OutroStat(
    value: String,
    label: String,
    valueColor: Color,
    theme: RecapPageTheme,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(theme.onBackground.copy(alpha = 0.16f))
            .padding(16.dp)
    ) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            color = theme.onBackground.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2
        )
    }
}

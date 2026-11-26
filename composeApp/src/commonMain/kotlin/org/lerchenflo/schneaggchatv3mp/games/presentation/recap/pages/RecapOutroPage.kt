package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapBadge
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageTheme
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageThemes
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPalette
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_badge_alarm_clock
import schneaggchatv3mp.composeapp.generated.resources.recap_badge_bug_hunter
import schneaggchatv3mp.composeapp.generated.resources.recap_badge_chatterbox
import schneaggchatv3mp.composeapp.generated.resources.recap_badge_double_texter
import schneaggchatv3mp.composeapp.generated.resources.recap_badge_early_bird
import schneaggchatv3mp.composeapp.generated.resources.recap_badge_forgetful
import schneaggchatv3mp.composeapp.generated.resources.recap_badge_ghost
import schneaggchatv3mp.composeapp.generated.resources.recap_badge_icebreaker
import schneaggchatv3mp.composeapp.generated.resources.recap_badge_idea_machine
import schneaggchatv3mp.composeapp.generated.resources.recap_badge_night_owl
import schneaggchatv3mp.composeapp.generated.resources.recap_badge_quick_replier
import schneaggchatv3mp.composeapp.generated.resources.recap_badge_speed_reader
import schneaggchatv3mp.composeapp.generated.resources.recap_badge_streak_master
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_badges
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_friends
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_messages
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_rank_errors
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_rank_games
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_rank_map
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_rank_messages
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_reactions
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_streak
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_subtitle
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_title
import schneaggchatv3mp.composeapp.generated.resources.recap_rank_number

/** The overview keeps to two badge rows on a phone; the earliest declared badges win. */
private const val MAX_OUTRO_BADGES = 6

// Value/label/color for one shareable summary tile - color echoes the accent of the page the
// stat originally came from, so the recognizable palette carries over into the screenshot.
private data class OutroTileData(val value: String, val label: String, val valueColor: Color)

/**
 * The overview of the whole recap, the page people share: the global ranks first (messages, map,
 * games, beta tester), then the headline numbers, then the personality badges.
 */
@Composable
fun RecapOutroPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Outro
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_outro_title, recap.username), color = theme.onBackground, maxFontSize = 36.sp)
        }
        Spacer(Modifier.height(24.dp))

        val tiles = rankTiles(recap) + numberTiles(recap)
        val rows = tiles.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEachIndexed { rowIndex, rowTiles ->
                RevealItem(visible, rowIndex + 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

        val badges = recap.badges.take(MAX_OUTRO_BADGES)
        if (badges.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            RevealItem(visible, rows.size + 1) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecapEyebrow(text = stringResource(Res.string.recap_outro_badges), color = theme.secondary)
                    PillFlow(pills = badges.map { stringResource(it.label()) }, theme = theme)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        RevealItem(visible, rows.size + 2) {
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
private fun rankTiles(recap: RecapUi): List<OutroTileData> = buildList {
    val ranks = listOf(
        Triple(recap.myRank, Res.string.recap_outro_rank_messages, RecapPalette.Gold),
        Triple(recap.myMapRank, Res.string.recap_outro_rank_map, RecapPalette.Orange),
        Triple(recap.myGamesRank, Res.string.recap_outro_rank_games, RecapPalette.Aqua),
        Triple(recap.myBetaTesterRank, Res.string.recap_outro_rank_errors, RecapPalette.Coral),
    )
    ranks.forEach { (rank, label, color) ->
        if (rank != null) add(OutroTileData(stringResource(Res.string.recap_rank_number, rank), stringResource(label), color))
    }
}

@Composable
private fun numberTiles(recap: RecapUi): List<OutroTileData> = buildList {
    add(OutroTileData(formatCount(recap.messagesSent), stringResource(Res.string.recap_outro_messages), RecapPalette.Green))
    add(OutroTileData(recap.longestStreakDays.toString(), stringResource(Res.string.recap_outro_streak), RecapPalette.Pink))
    add(OutroTileData(formatCount(recap.friendsCount.toLong()), stringResource(Res.string.recap_outro_friends), RecapPalette.Lemon))
    add(OutroTileData(formatCount(recap.reactionsGiven), stringResource(Res.string.recap_outro_reactions), RecapPalette.Lilac))
}

private fun RecapBadge.label(): StringResource = when (this) {
    RecapBadge.BUG_HUNTER -> Res.string.recap_badge_bug_hunter
    RecapBadge.IDEA_MACHINE -> Res.string.recap_badge_idea_machine
    RecapBadge.NIGHT_OWL -> Res.string.recap_badge_night_owl
    RecapBadge.EARLY_BIRD -> Res.string.recap_badge_early_bird
    RecapBadge.SPEED_READER -> Res.string.recap_badge_speed_reader
    RecapBadge.QUICK_REPLIER -> Res.string.recap_badge_quick_replier
    RecapBadge.GHOST -> Res.string.recap_badge_ghost
    RecapBadge.DOUBLE_TEXTER -> Res.string.recap_badge_double_texter
    RecapBadge.ICEBREAKER -> Res.string.recap_badge_icebreaker
    RecapBadge.CHATTERBOX -> Res.string.recap_badge_chatterbox
    RecapBadge.STREAK_MASTER -> Res.string.recap_badge_streak_master
    RecapBadge.ALARM_CLOCK -> Res.string.recap_badge_alarm_clock
    RecapBadge.FORGETFUL -> Res.string.recap_badge_forgetful
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
            .padding(14.dp)
    ) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 26.sp,
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

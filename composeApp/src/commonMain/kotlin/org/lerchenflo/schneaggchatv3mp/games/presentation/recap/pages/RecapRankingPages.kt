package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.RankedRowUi
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageTheme
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageThemes
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_betatester_clean_record
import schneaggchatv3mp.composeapp.generated.resources.recap_betatester_exception_count
import schneaggchatv3mp.composeapp.generated.resources.recap_betatester_first_exception
import schneaggchatv3mp.composeapp.generated.resources.recap_betatester_push_failures
import schneaggchatv3mp.composeapp.generated.resources.recap_betatester_title
import schneaggchatv3mp.composeapp.generated.resources.recap_betatester_top_endpoint_label
import schneaggchatv3mp.composeapp.generated.resources.recap_betatester_top_exception_label
import schneaggchatv3mp.composeapp.generated.resources.recap_betatester_your_rank
import schneaggchatv3mp.composeapp.generated.resources.recap_games_not_ranked
import schneaggchatv3mp.composeapp.generated.resources.recap_games_points
import schneaggchatv3mp.composeapp.generated.resources.recap_games_title
import schneaggchatv3mp.composeapp.generated.resources.recap_games_your_rank
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
import schneaggchatv3mp.composeapp.generated.resources.recap_password_failed_label
import schneaggchatv3mp.composeapp.generated.resources.recap_password_failed_logins
import schneaggchatv3mp.composeapp.generated.resources.recap_password_reset_label
import schneaggchatv3mp.composeapp.generated.resources.recap_password_reset_subtitle
import schneaggchatv3mp.composeapp.generated.resources.recap_password_reset_this_year
import schneaggchatv3mp.composeapp.generated.resources.recap_password_title
import schneaggchatv3mp.composeapp.generated.resources.recap_rank_number
import schneaggchatv3mp.composeapp.generated.resources.recap_text_count
import schneaggchatv3mp.composeapp.generated.resources.recap_your_rank

// Story pages built around a global ranking (messages, map, games, exceptions) and the
// forgetful page (password resets, wrong passwords).

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

/** The public global games ranking: every game together, percentile points per leaderboard. */
@Composable
fun RecapGamesPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Games
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_games_title), color = theme.accent)
        }
        Spacer(Modifier.height(20.dp))
        val rank = recap.myGamesRank
        if (rank != null) {
            RevealItem(visible, 1) {
                RecapBody(
                    text = stringResource(Res.string.recap_games_your_rank),
                    color = theme.onBackground,
                    fontSize = 19.sp
                )
            }
            RevealItem(visible, 2) {
                RecapBigText(text = stringResource(Res.string.recap_rank_number, rank), color = theme.accent)
            }
            RevealItem(visible, 3) {
                RecapBody(
                    text = stringResource(
                        Res.string.recap_games_points,
                        formatCount(recap.myGamesPoints),
                        recap.myGamesPlayed
                    ),
                    color = theme.onBackground.copy(alpha = 0.75f),
                    fontSize = 17.sp
                )
            }
        } else {
            RevealItem(visible, 1) {
                RecapBody(
                    text = stringResource(Res.string.recap_games_not_ranked),
                    color = theme.onBackground.copy(alpha = 0.85f),
                    fontSize = 20.sp
                )
            }
        }
        if (recap.gamesLeaderboardTop.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            RankList(
                rows = recap.gamesLeaderboardTop,
                visible = visible,
                firstRevealIndex = 4,
                theme = theme,
                valueText = { formatCount(it.count) }
            )
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
        BetaTesterCrashFacts(recap = recap, visible = visible, theme = theme)
        if (recap.betaTesterRows.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            // The full list can be long; the page scrolls, so cap the reveal stagger at row 5 -
            // otherwise the last rows would only fade in seconds after the page opened.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recap.betaTesterRows.forEachIndexed { index, row ->
                    RevealItem(visible, 5 + index.coerceAtMost(5)) {
                        RankRow(row = row, valueText = formatCount(row.count), theme = theme)
                    }
                }
            }
        }
    }
}

/**
 * Your favourite crash, the most broken endpoint, your first crash and the pushes the server could
 * not deliver to you (not your fault, so they are not counted as your exceptions).
 */
@Composable
private fun BetaTesterCrashFacts(recap: RecapUi, visible: Boolean, theme: RecapPageTheme) {
    val cards = buildList {
        recap.myTopException?.let {
            add(stringResource(Res.string.recap_betatester_top_exception_label) to stringResource(Res.string.recap_text_count, it.text, formatCount(it.count)))
        }
        recap.myTopEndpoint?.let {
            add(stringResource(Res.string.recap_betatester_top_endpoint_label) to stringResource(Res.string.recap_text_count, it.text, formatCount(it.count)))
        }
    }
    val notes = buildList {
        recap.myFirstExceptionFormatted?.let { add(stringResource(Res.string.recap_betatester_first_exception, it)) }
        if (recap.myPushFailuresAllTime > 0) {
            add(
                stringResource(
                    Res.string.recap_betatester_push_failures,
                    formatCount(recap.myPushFailuresThisYear),
                    formatCount(recap.myPushFailuresAllTime)
                )
            )
        }
    }
    if (cards.isEmpty() && notes.isEmpty()) return

    Spacer(Modifier.height(24.dp))
    RevealItem(visible, 4) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            cards.forEach { (label, text) ->
                RecapLabeledCard(label = label, text = text, theme = theme)
            }
            notes.forEach { note ->
                RecapBody(text = note, color = theme.onBackground.copy(alpha = 0.8f), fontSize = 16.sp)
            }
        }
    }
}

/** The forgetful page: password reset emails and wrong passwords entered for your account. */
@Composable
fun RecapPasswordResetPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Password
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_password_title), color = theme.accent, maxFontSize = 36.sp)
        }
        // Reset emails lead; wrong passwords take the big number only when there were no resets
        val resetsLead = recap.passwordResetEmailsSentAllTime > 0
        val heroAllTime = if (resetsLead) recap.passwordResetEmailsSentAllTime else recap.failedLoginsAllTime
        val heroThisYear = if (resetsLead) recap.passwordResetEmailsSentThisYear else recap.failedLoginsThisYear
        Spacer(Modifier.height(24.dp))
        RecapBigNumber(target = heroAllTime, running = visible, color = theme.onBackground)
        RevealItem(visible, 1) {
            RecapBody(
                text = stringResource(if (resetsLead) Res.string.recap_password_reset_label else Res.string.recap_password_failed_label),
                color = theme.onBackground.copy(alpha = 0.8f),
                fontSize = 20.sp
            )
        }
        if (heroThisYear > 0) {
            Spacer(Modifier.height(12.dp))
            RevealItem(visible, 2) {
                RecapBody(
                    text = stringResource(Res.string.recap_password_reset_this_year, heroThisYear.toInt()),
                    color = theme.accent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        // Worded as "entered for your account": anyone who knows the username can cause one
        if (resetsLead && recap.failedLoginsAllTime > 0) {
            Spacer(Modifier.height(16.dp))
            RevealItem(visible, 3) {
                RecapBody(
                    text = stringResource(
                        Res.string.recap_password_failed_logins,
                        formatCount(recap.failedLoginsAllTime),
                        formatCount(recap.failedLoginsThisYear)
                    ),
                    color = theme.onBackground.copy(alpha = 0.8f),
                    fontSize = 16.sp
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        RevealItem(visible, 4) {
            RecapBody(
                text = stringResource(Res.string.recap_password_reset_subtitle),
                color = theme.onBackground.copy(alpha = 0.6f),
                fontSize = 15.sp
            )
        }
    }
}

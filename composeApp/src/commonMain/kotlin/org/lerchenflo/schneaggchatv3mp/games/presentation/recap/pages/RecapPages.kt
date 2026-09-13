package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageThemes
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_all_time_messages
import schneaggchatv3mp.composeapp.generated.resources.recap_avg_message_length
import schneaggchatv3mp.composeapp.generated.resources.recap_busiest_day
import schneaggchatv3mp.composeapp.generated.resources.recap_busiest_day_messages
import schneaggchatv3mp.composeapp.generated.resources.recap_busiest_hour
import schneaggchatv3mp.composeapp.generated.resources.recap_characters_typed
import schneaggchatv3mp.composeapp.generated.resources.recap_first_message_ever
import schneaggchatv3mp.composeapp.generated.resources.recap_hello
import schneaggchatv3mp.composeapp.generated.resources.recap_in_year_you_had_a_lot_to_say
import schneaggchatv3mp.composeapp.generated.resources.recap_logins_this_year
import schneaggchatv3mp.composeapp.generated.resources.recap_longest_message
import schneaggchatv3mp.composeapp.generated.resources.recap_longest_message_to
import schneaggchatv3mp.composeapp.generated.resources.recap_member_since
import schneaggchatv3mp.composeapp.generated.resources.recap_messages_to_favorite_humans
import schneaggchatv3mp.composeapp.generated.resources.recap_most_active_month
import schneaggchatv3mp.composeapp.generated.resources.recap_others_sent_you
import schneaggchatv3mp.composeapp.generated.resources.recap_others_sent_you_suffix
import schneaggchatv3mp.composeapp.generated.resources.recap_popular
import schneaggchatv3mp.composeapp.generated.resources.recap_rhythm_title
import schneaggchatv3mp.composeapp.generated.resources.recap_streak_days
import schneaggchatv3mp.composeapp.generated.resources.recap_streak_label
import schneaggchatv3mp.composeapp.generated.resources.recap_typing_title
import schneaggchatv3mp.composeapp.generated.resources.recap_using_schneaggchat_in_numbers
import schneaggchatv3mp.composeapp.generated.resources.recap_words_typed
import schneaggchatv3mp.composeapp.generated.resources.recap_you_sent
import schneaggchatv3mp.composeapp.generated.resources.recap_your_year

// One composable per recap story page. Every page receives the full RecapUi and a
// `visible` flag (true while it is the current pager page) that drives its animations.
// This file: the opening pages about the user's own messaging.

@Composable
fun RecapIntroPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Intro
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapEyebrow(text = stringResource(Res.string.recap_hello, recap.username), color = theme.onBackground.copy(alpha = 0.8f))
        }
        Spacer(Modifier.height(16.dp))
        RevealItem(visible, 1) {
            RecapHeadline(text = stringResource(Res.string.recap_your_year), color = theme.accent, maxFontSize = 34.sp)
        }
        RevealItem(visible, 2) {
            RecapBigText(text = recap.year.toString(), color = theme.secondary, maxFontSize = 120.sp)
        }
        RevealItem(visible, 3) {
            RecapHeadline(text = stringResource(Res.string.recap_using_schneaggchat_in_numbers), color = theme.accent, maxFontSize = 34.sp)
        }
        Spacer(Modifier.height(36.dp))
        RevealItem(visible, 4) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RecapBody(
                    text = stringResource(Res.string.recap_member_since, recap.memberSinceFormatted, recap.accountAgeDays),
                    color = theme.onBackground.copy(alpha = 0.75f),
                    fontSize = 16.sp
                )
                if (recap.loginCountThisYear > 0) {
                    RecapBody(
                        text = stringResource(Res.string.recap_logins_this_year, formatCount(recap.loginCountThisYear)),
                        color = theme.onBackground.copy(alpha = 0.75f),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RecapMessagesSentPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Sent
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(
                text = stringResource(Res.string.recap_in_year_you_had_a_lot_to_say, recap.year),
                color = theme.secondary,
                maxFontSize = 30.sp
            )
        }
        Spacer(Modifier.height(28.dp))
        RevealItem(visible, 1) {
            RecapEyebrow(text = stringResource(Res.string.recap_you_sent), color = theme.onBackground.copy(alpha = 0.8f))
        }
        RecapBigNumber(target = recap.messagesSent, running = visible, color = theme.accent)
        RevealItem(visible, 2) {
            RecapBody(
                text = stringResource(Res.string.recap_messages_to_favorite_humans),
                color = theme.onBackground.copy(alpha = 0.85f),
                fontSize = 19.sp
            )
        }
        if (recap.sentByType.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            RevealItem(visible, 3) {
                PillFlow(
                    pills = recap.sentByType.map { "${formatCount(it.count)} ${it.label.asString()}" },
                    theme = theme
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        RevealItem(visible, 4) {
            RecapBody(
                text = stringResource(Res.string.recap_all_time_messages, formatCount(recap.messagesSentAllTime)),
                color = theme.onBackground.copy(alpha = 0.6f),
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun RecapTypingPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Typing
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_typing_title), color = theme.accent)
        }
        Spacer(Modifier.height(28.dp))
        RecapBigNumber(target = recap.charactersTyped, running = visible, color = theme.onBackground, maxFontSize = 84.sp)
        RevealItem(visible, 1) {
            RecapBody(
                text = stringResource(Res.string.recap_characters_typed),
                color = theme.onBackground.copy(alpha = 0.8f),
                fontSize = 20.sp
            )
        }
        Spacer(Modifier.height(24.dp))
        RevealItem(visible, 2) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecapBody(
                    text = stringResource(Res.string.recap_words_typed, formatCount(recap.wordsTyped)),
                    color = theme.secondary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                RecapBody(
                    text = stringResource(Res.string.recap_avg_message_length, recap.averageMessageLength),
                    color = theme.onBackground.copy(alpha = 0.7f),
                    fontSize = 17.sp
                )
            }
        }
        recap.longestMessage?.let { longest ->
            Spacer(Modifier.height(28.dp))
            RevealItem(visible, 3) {
                RecapCard(theme = theme) {
                    RecapBody(
                        text = stringResource(Res.string.recap_longest_message, longest.length),
                        color = theme.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "\u201C${longest.preview}\u2026\u201D",
                        color = theme.onBackground.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        maxLines = 4
                    )
                    RecapBody(
                        text = stringResource(Res.string.recap_longest_message_to, longest.toName),
                        color = theme.onBackground.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RecapRhythmPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Rhythm
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_rhythm_title), color = theme.accent)
        }
        Spacer(Modifier.height(28.dp))
        MonthBarChart(
            months = recap.perMonth,
            peakMonth = recap.mostActiveMonth?.month,
            visible = visible,
            barColor = theme.onBackground.copy(alpha = 0.35f),
            peakColor = theme.accent,
            labelColor = theme.onBackground.copy(alpha = 0.6f)
        )
        recap.mostActiveMonth?.let { peak ->
            Spacer(Modifier.height(14.dp))
            RevealItem(visible, 1) {
                RecapBody(
                    text = stringResource(Res.string.recap_most_active_month, peak.monthName.asString()),
                    color = theme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        RevealItem(visible, 2) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recap.busiestDayFormatted?.let { day ->
                    RecapBody(
                        text = stringResource(Res.string.recap_busiest_day, day),
                        color = theme.secondary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    RecapBody(
                        text = stringResource(Res.string.recap_busiest_day_messages, recap.busiestDayCount),
                        color = theme.onBackground.copy(alpha = 0.75f),
                        fontSize = 16.sp
                    )
                }
                recap.busiestHourOfDay?.let { hour ->
                    RecapBody(
                        text = stringResource(Res.string.recap_busiest_hour, hour),
                        color = theme.onBackground.copy(alpha = 0.75f),
                        fontSize = 16.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        RevealItem(visible, 3) {
            Column {
                RecapBigText(
                    text = stringResource(Res.string.recap_streak_days, recap.longestStreakDays),
                    color = theme.secondary,
                    maxFontSize = 48.sp
                )
                RecapBody(
                    text = stringResource(Res.string.recap_streak_label),
                    color = theme.onBackground.copy(alpha = 0.75f),
                    fontSize = 16.sp
                )
            }
        }
        recap.firstMessageEverFormatted?.let { first ->
            Spacer(Modifier.height(20.dp))
            RevealItem(visible, 4) {
                RecapBody(
                    text = stringResource(Res.string.recap_first_message_ever, first),
                    color = theme.onBackground.copy(alpha = 0.55f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun RecapMessagesReceivedPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Received
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_others_sent_you), color = theme.onBackground, maxFontSize = 34.sp)
        }
        RecapBigNumber(target = recap.messagesReceived, running = visible, color = theme.accent)
        RevealItem(visible, 1) {
            RecapHeadline(text = stringResource(Res.string.recap_others_sent_you_suffix), color = theme.onBackground, maxFontSize = 34.sp)
        }
        Spacer(Modifier.height(28.dp))
        RevealItem(visible, 2) {
            RecapBody(
                text = stringResource(Res.string.recap_popular),
                color = theme.secondary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))
        RevealItem(visible, 3) {
            RecapBody(
                text = stringResource(Res.string.recap_all_time_messages, formatCount(recap.messagesReceivedAllTime)),
                color = theme.onBackground.copy(alpha = 0.6f),
                fontSize = 15.sp
            )
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.EmojiCountUi
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapUi
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_all_time_messages
import schneaggchatv3mp.composeapp.generated.resources.recap_avg_message_length
import schneaggchatv3mp.composeapp.generated.resources.recap_busiest_day
import schneaggchatv3mp.composeapp.generated.resources.recap_busiest_day_messages
import schneaggchatv3mp.composeapp.generated.resources.recap_busiest_hour
import schneaggchatv3mp.composeapp.generated.resources.recap_characters_typed
import schneaggchatv3mp.composeapp.generated.resources.recap_first_message_ever
import schneaggchatv3mp.composeapp.generated.resources.recap_friend_requests_sent
import schneaggchatv3mp.composeapp.generated.resources.recap_friends_label
import schneaggchatv3mp.composeapp.generated.resources.recap_from_you_from_them
import schneaggchatv3mp.composeapp.generated.resources.recap_games_rank
import schneaggchatv3mp.composeapp.generated.resources.recap_games_score
import schneaggchatv3mp.composeapp.generated.resources.recap_games_title
import schneaggchatv3mp.composeapp.generated.resources.recap_group_message_count
import schneaggchatv3mp.composeapp.generated.resources.recap_groups_created
import schneaggchatv3mp.composeapp.generated.resources.recap_groups_member_of
import schneaggchatv3mp.composeapp.generated.resources.recap_groups_title
import schneaggchatv3mp.composeapp.generated.resources.recap_hello
import schneaggchatv3mp.composeapp.generated.resources.recap_in_year_you_had_a_lot_to_say
import schneaggchatv3mp.composeapp.generated.resources.recap_inner_circle
import schneaggchatv3mp.composeapp.generated.resources.recap_leaderboard_messages
import schneaggchatv3mp.composeapp.generated.resources.recap_leaderboard_title
import schneaggchatv3mp.composeapp.generated.resources.recap_logins_this_year
import schneaggchatv3mp.composeapp.generated.resources.recap_longest_message
import schneaggchatv3mp.composeapp.generated.resources.recap_longest_message_to
import schneaggchatv3mp.composeapp.generated.resources.recap_loyal_friend
import schneaggchatv3mp.composeapp.generated.resources.recap_map_all_time
import schneaggchatv3mp.composeapp.generated.resources.recap_map_created
import schneaggchatv3mp.composeapp.generated.resources.recap_map_edited
import schneaggchatv3mp.composeapp.generated.resources.recap_map_title
import schneaggchatv3mp.composeapp.generated.resources.recap_member_since
import schneaggchatv3mp.composeapp.generated.resources.recap_messages
import schneaggchatv3mp.composeapp.generated.resources.recap_messages_to_favorite_humans
import schneaggchatv3mp.composeapp.generated.resources.recap_most_active_group
import schneaggchatv3mp.composeapp.generated.resources.recap_most_active_month
import schneaggchatv3mp.composeapp.generated.resources.recap_most_reacted_message
import schneaggchatv3mp.composeapp.generated.resources.recap_new_friends_this_year
import schneaggchatv3mp.composeapp.generated.resources.recap_others_sent_you
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_friends
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_messages
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_reactions
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_streak
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_subtitle
import schneaggchatv3mp.composeapp.generated.resources.recap_outro_title
import schneaggchatv3mp.composeapp.generated.resources.recap_poll_votes_cast
import schneaggchatv3mp.composeapp.generated.resources.recap_polls_created
import schneaggchatv3mp.composeapp.generated.resources.recap_popular
import schneaggchatv3mp.composeapp.generated.resources.recap_rank_number
import schneaggchatv3mp.composeapp.generated.resources.recap_reactions_given
import schneaggchatv3mp.composeapp.generated.resources.recap_reactions_received
import schneaggchatv3mp.composeapp.generated.resources.recap_reactions_title
import schneaggchatv3mp.composeapp.generated.resources.recap_rhythm_title
import schneaggchatv3mp.composeapp.generated.resources.recap_social_title
import schneaggchatv3mp.composeapp.generated.resources.recap_streak_days
import schneaggchatv3mp.composeapp.generated.resources.recap_streak_label
import schneaggchatv3mp.composeapp.generated.resources.recap_texts_number
import schneaggchatv3mp.composeapp.generated.resources.recap_top_emoji_given
import schneaggchatv3mp.composeapp.generated.resources.recap_top_emoji_received
import schneaggchatv3mp.composeapp.generated.resources.recap_typing_title
import schneaggchatv3mp.composeapp.generated.resources.recap_using_schneaggchat_in_numbers
import schneaggchatv3mp.composeapp.generated.resources.recap_words_typed
import schneaggchatv3mp.composeapp.generated.resources.recap_you_sent
import schneaggchatv3mp.composeapp.generated.resources.recap_your_rank
import schneaggchatv3mp.composeapp.generated.resources.recap_your_year

// One composable per recap story page. Every page receives the full RecapUi and a
// `visible` flag (true while it is the current pager page) that drives its animations.

@Composable
fun RecapIntroPage(recap: RecapUi, visible: Boolean) {
    RecapPageBackground(topColor = Color(0xFF0A1ED3), bottomColor = Color(0xFF040A4F)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            RevealItem(visible, 0) {
                Text(
                    text = stringResource(Res.string.recap_hello, recap.username),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(16.dp))
            RevealItem(visible, 1) {
                Text(
                    text = stringResource(Res.string.recap_your_year),
                    color = Color(0xFF1DB954),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
            }
            RevealItem(visible, 2) {
                Text(
                    text = recap.year.toString(),
                    color = Color(0xFFD63CFF),
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Black
                )
            }
            RevealItem(visible, 3) {
                Text(
                    text = stringResource(Res.string.recap_using_schneaggchat_in_numbers),
                    color = Color(0xFF1DB954),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 36.sp
                )
            }
            Spacer(Modifier.height(32.dp))
            RevealItem(visible, 4) {
                Text(
                    text = stringResource(Res.string.recap_member_since, recap.memberSinceFormatted, recap.accountAgeDays),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (recap.loginCountThisYear > 0) {
                RevealItem(visible, 5) {
                    Text(
                        text = stringResource(Res.string.recap_logins_this_year, formatCount(recap.loginCountThisYear)),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun RecapMessagesSentPage(recap: RecapUi, visible: Boolean) {
    RecapPageBackground(topColor = Color(0xFF0C1033), bottomColor = Color(0xFF2B0A4E)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            RevealItem(visible, 0) {
                Text(
                    text = stringResource(Res.string.recap_in_year_you_had_a_lot_to_say, recap.year),
                    color = Color(0xFF1DB954),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(20.dp))
            RevealItem(visible, 1) {
                Text(
                    text = stringResource(Res.string.recap_you_sent),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            CountUpText(target = recap.messagesSent, running = visible, color = Color(0xFFFF007F), fontSize = 76.sp)
            RevealItem(visible, 2) {
                Text(
                    text = stringResource(Res.string.recap_messages_to_favorite_humans),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(28.dp))
            RevealItem(visible, 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recap.sentByType.forEach { entry ->
                        StatPill(
                            text = "${formatCount(entry.count)} ${entry.label.asString()}",
                            textColor = Color.White,
                            backgroundColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            RevealItem(visible, 4) {
                Text(
                    text = stringResource(Res.string.recap_all_time_messages, formatCount(recap.messagesSentAllTime)),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun RecapTypingPage(recap: RecapUi, visible: Boolean) {
    RecapPageBackground(topColor = Color(0xFF00343A), bottomColor = Color(0xFF001B1F)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            RevealItem(visible, 0) {
                Text(
                    text = stringResource(Res.string.recap_typing_title),
                    color = Color(0xFF00E5C7),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 42.sp
                )
            }
            Spacer(Modifier.height(24.dp))
            CountUpText(target = recap.charactersTyped, running = visible, color = Color.White, fontSize = 64.sp)
            RevealItem(visible, 1) {
                Text(
                    text = stringResource(Res.string.recap_characters_typed),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(20.dp))
            RevealItem(visible, 2) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(Res.string.recap_words_typed, formatCount(recap.wordsTyped)),
                        color = Color(0xFFFFDF00),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.recap_avg_message_length, recap.averageMessageLength),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            recap.longestMessage?.let { longest ->
                Spacer(Modifier.height(28.dp))
                RevealItem(visible, 3) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.recap_longest_message, longest.length),
                            color = Color(0xFF00E5C7),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "“${longest.preview}…”",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 15.sp,
                            fontStyle = FontStyle.Italic,
                            maxLines = 4
                        )
                        Text(
                            text = stringResource(Res.string.recap_longest_message_to, longest.toName),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecapRhythmPage(recap: RecapUi, visible: Boolean) {
    RecapPageBackground(topColor = Color(0xFF3A0057), bottomColor = Color(0xFF1B0029)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            RevealItem(visible, 0) {
                Text(
                    text = stringResource(Res.string.recap_rhythm_title),
                    color = Color(0xFFFF9E00),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(28.dp))
            MonthBarChart(
                months = recap.perMonth,
                peakMonth = recap.mostActiveMonth?.month,
                visible = visible,
                barColor = Color.White.copy(alpha = 0.35f),
                peakColor = Color(0xFFFF9E00),
                labelColor = Color.White.copy(alpha = 0.6f)
            )
            recap.mostActiveMonth?.let { peak ->
                Spacer(Modifier.height(12.dp))
                RevealItem(visible, 1) {
                    Text(
                        text = stringResource(Res.string.recap_most_active_month, peak.monthName.asString()),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            RevealItem(visible, 2) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recap.busiestDayFormatted?.let { day ->
                        Text(
                            text = stringResource(Res.string.recap_busiest_day, day),
                            color = Color(0xFFFFDF00),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(Res.string.recap_busiest_day_messages, recap.busiestDayCount),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 16.sp
                        )
                    }
                    recap.busiestHourOfDay?.let { hour ->
                        Text(
                            text = stringResource(Res.string.recap_busiest_hour, hour),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 16.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            RevealItem(visible, 3) {
                Column {
                    Text(
                        text = stringResource(Res.string.recap_streak_days, recap.longestStreakDays),
                        color = Color(0xFFFF007F),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = stringResource(Res.string.recap_streak_label),
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 16.sp
                    )
                }
            }
            recap.firstMessageEverFormatted?.let { first ->
                Spacer(Modifier.height(20.dp))
                RevealItem(visible, 4) {
                    Text(
                        text = stringResource(Res.string.recap_first_message_ever, first),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RecapMessagesReceivedPage(recap: RecapUi, visible: Boolean) {
    RecapPageBackground(topColor = Color(0xFF6B0F1A), bottomColor = Color(0xFF2E060B)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            RevealItem(visible, 0) {
                Text(
                    text = stringResource(Res.string.recap_others_sent_you),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 40.sp
                )
            }
            CountUpText(target = recap.messagesReceived, running = visible, color = Color(0xFFFFDF00), fontSize = 76.sp)
            RevealItem(visible, 1) {
                Text(
                    text = stringResource(Res.string.recap_messages),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(24.dp))
            RevealItem(visible, 2) {
                Text(
                    text = stringResource(Res.string.recap_popular),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(12.dp))
            RevealItem(visible, 3) {
                Text(
                    text = stringResource(Res.string.recap_all_time_messages, formatCount(recap.messagesReceivedAllTime)),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun RecapTopContactsPage(recap: RecapUi, visible: Boolean) {
    val rowColors = listOf(
        Color(0xFF4D00FF),
        Color(0xFF0066FF),
        Color(0xFF9E00FF),
        Color(0xFF007A5E),
        Color(0xFFD00056)
    )

    RecapPageBackground(topColor = Color(0xFFFFDF00), bottomColor = Color(0xFFFFB300)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            RevealItem(visible, 0) {
                Text(
                    text = stringResource(Res.string.recap_inner_circle),
                    color = Color.Black,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 46.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                recap.topPartners.take(5).forEachIndexed { index, partner ->
                    RevealItem(visible, index + 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(rowColors[index % rowColors.size])
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "#${index + 1} ",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                ProfilePictureView(
                                    filepath = partner.profilePictureFilePath,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .padding(end = 8.dp)
                                        .clip(CircleShape)
                                )
                                Column {
                                    Text(
                                        text = partner.name,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = stringResource(
                                            Res.string.recap_from_you_from_them,
                                            formatCount(partner.messagesFromMe),
                                            formatCount(partner.messagesFromThem)
                                        ),
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(
                                text = stringResource(Res.string.recap_texts_number, partner.messagesExchanged.toInt()),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            RevealItem(visible, recap.topPartners.size + 1) {
                Text(
                    text = stringResource(Res.string.recap_loyal_friend),
                    color = Color.Black.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RecapReactionsPage(recap: RecapUi, visible: Boolean) {
    RecapPageBackground(topColor = Color(0xFF7A0BC0), bottomColor = Color(0xFF3B0764)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            RevealItem(visible, 0) {
                Text(
                    text = stringResource(Res.string.recap_reactions_title),
                    color = Color(0xFFFFB6F9),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(24.dp))
            CountUpText(target = recap.reactionsGiven, running = visible, color = Color.White, fontSize = 64.sp)
            RevealItem(visible, 1) {
                Text(
                    text = stringResource(Res.string.recap_reactions_given),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(12.dp))
            RevealItem(visible, 2) {
                Text(
                    text = stringResource(Res.string.recap_reactions_received, formatCount(recap.reactionsReceived)),
                    color = Color(0xFFFFDF00),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (recap.topEmojiGiven.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                RevealItem(visible, 3) {
                    EmojiRow(
                        title = stringResource(Res.string.recap_top_emoji_given),
                        emojis = recap.topEmojiGiven
                    )
                }
            }
            if (recap.topEmojiReceived.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                RevealItem(visible, 4) {
                    EmojiRow(
                        title = stringResource(Res.string.recap_top_emoji_received),
                        emojis = recap.topEmojiReceived
                    )
                }
            }
            recap.mostReactedMessage?.let { mostReacted ->
                Spacer(Modifier.height(24.dp))
                RevealItem(visible, 5) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.recap_most_reacted_message, mostReacted.reactionCount),
                            color = Color(0xFFFFB6F9),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "“${mostReacted.preview}”",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 15.sp,
                            fontStyle = FontStyle.Italic,
                            maxLines = 3
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiRow(title: String, emojis: List<EmojiCountUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            emojis.forEach { entry ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = entry.emoji, fontSize = 32.sp)
                    Text(
                        text = formatCount(entry.count),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RecapSocialPage(recap: RecapUi, visible: Boolean) {
    RecapPageBackground(topColor = Color(0xFF005F2E), bottomColor = Color(0xFF00230F)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            RevealItem(visible, 0) {
                Text(
                    text = stringResource(Res.string.recap_social_title),
                    color = Color(0xFF1DB954),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(24.dp))
            CountUpText(target = recap.friendsCount.toLong(), running = visible, color = Color.White, fontSize = 76.sp)
            RevealItem(visible, 1) {
                Text(
                    text = stringResource(Res.string.recap_friends_label),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(28.dp))
            RevealItem(visible, 2) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(Res.string.recap_new_friends_this_year, recap.newFriendsThisYear),
                        color = Color(0xFFFFDF00),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.recap_friend_requests_sent, recap.friendRequestsSentThisYear.toInt()),
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 16.sp
                    )
                }
            }
            if (recap.pollsCreated > 0 || recap.pollVotesCast > 0) {
                Spacer(Modifier.height(20.dp))
                RevealItem(visible, 3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPill(
                            text = stringResource(Res.string.recap_polls_created, recap.pollsCreated.toInt()),
                            textColor = Color.White,
                            backgroundColor = Color.White.copy(alpha = 0.15f)
                        )
                        StatPill(
                            text = stringResource(Res.string.recap_poll_votes_cast, recap.pollVotesCast.toInt()),
                            textColor = Color.White,
                            backgroundColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecapGroupsPage(recap: RecapUi, visible: Boolean) {
    RecapPageBackground(topColor = Color(0xFF00456B), bottomColor = Color(0xFF001D2E)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            RevealItem(visible, 0) {
                Text(
                    text = stringResource(Res.string.recap_groups_title),
                    color = Color(0xFF35C4FF),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 42.sp
                )
            }
            Spacer(Modifier.height(24.dp))
            CountUpText(target = recap.groupsMemberOf.toLong(), running = visible, color = Color.White, fontSize = 76.sp)
            RevealItem(visible, 1) {
                Text(
                    text = stringResource(Res.string.recap_groups_member_of),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(12.dp))
            RevealItem(visible, 2) {
                Text(
                    text = stringResource(Res.string.recap_groups_created, recap.groupsCreated),
                    color = Color(0xFFFFDF00),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            recap.mostActiveGroup?.let { group ->
                Spacer(Modifier.height(28.dp))
                RevealItem(visible, 3) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.recap_most_active_group),
                            color = Color(0xFF35C4FF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = group.name,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = stringResource(Res.string.recap_group_message_count, formatCount(group.messageCount)),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecapLeaderboardPage(recap: RecapUi, visible: Boolean) {
    RecapPageBackground(topColor = Color(0xFF1A1A1A), bottomColor = Color(0xFF000000)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            RevealItem(visible, 0) {
                Text(
                    text = stringResource(Res.string.recap_leaderboard_title),
                    color = Color(0xFFFFD700),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(16.dp))
            recap.myRank?.let { rank ->
                RevealItem(visible, 1) {
                    Text(
                        text = stringResource(Res.string.recap_your_rank),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                RevealItem(visible, 2) {
                    Text(
                        text = stringResource(Res.string.recap_rank_number, rank),
                        color = Color(0xFFFFD700),
                        fontSize = 88.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            RevealItem(visible, 3) {
                Text(
                    text = stringResource(Res.string.recap_leaderboard_messages, formatCount(recap.myLeaderboardMessageCount)),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 17.sp
                )
            }
            Spacer(Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recap.leaderboardTop.forEachIndexed { index, row ->
                    RevealItem(visible, index + 4) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (row.isMe) Color(0xFFFFD700).copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${row.rank}  ${row.username}",
                                color = if (row.isMe) Color(0xFFFFD700) else Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatCount(row.messageCount),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecapMapPage(recap: RecapUi, visible: Boolean) {
    RecapPageBackground(topColor = Color(0xFF4A3200), bottomColor = Color(0xFF1F1500)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            RevealItem(visible, 0) {
                Text(
                    text = stringResource(Res.string.recap_map_title),
                    color = Color(0xFFFF9E00),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(24.dp))
            CountUpText(target = recap.mapEntriesCreated, running = visible, color = Color.White, fontSize = 76.sp)
            RevealItem(visible, 1) {
                Text(
                    text = stringResource(Res.string.recap_map_created),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(20.dp))
            RevealItem(visible, 2) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(Res.string.recap_map_edited, recap.mapEntriesEdited.toInt()),
                        color = Color(0xFFFFDF00),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.recap_map_all_time, recap.mapEntriesCreatedAllTime.toInt()),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RecapGamesPage(recap: RecapUi, visible: Boolean) {
    RecapPageBackground(topColor = Color(0xFF10002B), bottomColor = Color(0xFF240090)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            RevealItem(visible, 0) {
                Text(
                    text = stringResource(Res.string.recap_games_title),
                    color = Color(0xFF00E5C7),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                recap.games.forEachIndexed { index, game ->
                    RevealItem(visible, index + 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${game.gameName} · ${game.difficulty}",
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = stringResource(Res.string.recap_games_score, formatCount(game.bestScore)),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = stringResource(Res.string.recap_games_rank, game.rank),
                                color = if (game.rank <= 3) Color(0xFFFFD700) else Color(0xFF00E5C7),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecapOutroPage(recap: RecapUi, visible: Boolean) {
    RecapPageBackground(topColor = Color(0xFF0A1ED3), bottomColor = Color(0xFFD63CFF)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            RevealItem(visible, 0) {
                Text(
                    text = stringResource(Res.string.recap_outro_title, recap.username),
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 46.sp
                )
            }
            Spacer(Modifier.height(32.dp))
            RevealItem(visible, 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutroStat(formatCount(recap.messagesSent), stringResource(Res.string.recap_outro_messages), Modifier.weight(1f))
                    OutroStat(formatCount(recap.friendsCount.toLong()), stringResource(Res.string.recap_outro_friends), Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
            RevealItem(visible, 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutroStat(formatCount(recap.reactionsGiven), stringResource(Res.string.recap_outro_reactions), Modifier.weight(1f))
                    OutroStat(recap.longestStreakDays.toString(), stringResource(Res.string.recap_outro_streak), Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(40.dp))
            RevealItem(visible, 3) {
                Text(
                    text = stringResource(Res.string.recap_outro_subtitle),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OutroStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(16.dp)
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

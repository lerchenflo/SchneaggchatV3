package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapUi
import org.lerchenflo.schneaggchatv3mp.games.domain.StarterPartnerUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageTheme
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageThemes
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_duration_dh
import schneaggchatv3mp.composeapp.generated.resources.recap_duration_s
import schneaggchatv3mp.composeapp.generated.resources.recap_emoji_more_label
import schneaggchatv3mp.composeapp.generated.resources.recap_emoji_title
import schneaggchatv3mp.composeapp.generated.resources.recap_emoji_typed
import schneaggchatv3mp.composeapp.generated.resources.recap_percent
import schneaggchatv3mp.composeapp.generated.resources.recap_quote_count
import schneaggchatv3mp.composeapp.generated.resources.recap_speed_fastest_reader
import schneaggchatv3mp.composeapp.generated.resources.recap_speed_fastest_reader_label
import schneaggchatv3mp.composeapp.generated.resources.recap_speed_monologue
import schneaggchatv3mp.composeapp.generated.resources.recap_speed_monologue_label
import schneaggchatv3mp.composeapp.generated.resources.recap_speed_read
import schneaggchatv3mp.composeapp.generated.resources.recap_speed_read_intro
import schneaggchatv3mp.composeapp.generated.resources.recap_speed_reply_intro
import schneaggchatv3mp.composeapp.generated.resources.recap_speed_title
import schneaggchatv3mp.composeapp.generated.resources.recap_starter_intro
import schneaggchatv3mp.composeapp.generated.resources.recap_starter_label
import schneaggchatv3mp.composeapp.generated.resources.recap_starter_opener_label
import schneaggchatv3mp.composeapp.generated.resources.recap_starter_partner
import schneaggchatv3mp.composeapp.generated.resources.recap_starter_partners_label
import schneaggchatv3mp.composeapp.generated.resources.recap_starter_title
import schneaggchatv3mp.composeapp.generated.resources.recap_usage_duration_hm
import schneaggchatv3mp.composeapp.generated.resources.recap_usage_duration_m

// The three chat habits pages: how fast you read and reply, who starts the chats, your emoji of the year.

private const val SECOND_MILLIS = 1_000L
private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS

/** How fast you read and reply in direct chats, who reads you fastest, your double texting record. */
@Composable
fun RecapChatSpeedPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.ChatSpeed
    val habits = recap.chatHabits
    val reply = habits.medianReplyMillis
    val read = habits.medianReadMillis

    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_speed_title), color = theme.accent)
        }
        Spacer(Modifier.height(24.dp))

        // The reply time leads; without replies the read time takes its place
        val heroMillis = reply ?: read
        if (heroMillis != null) {
            RevealItem(visible, 1) {
                RecapBody(
                    text = stringResource(if (reply != null) Res.string.recap_speed_reply_intro else Res.string.recap_speed_read_intro),
                    color = theme.onBackground,
                    fontSize = 19.sp
                )
            }
            RevealItem(visible, 2) {
                RecapBigText(text = formatWaitDuration(heroMillis), color = theme.accent)
            }
        }
        if (reply != null && read != null) {
            RevealItem(visible, 3) {
                RecapBody(
                    text = stringResource(Res.string.recap_speed_read, formatWaitDuration(read)),
                    color = theme.onBackground.copy(alpha = 0.8f),
                    fontSize = 17.sp
                )
            }
        }

        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            habits.fastestReader?.let { reader ->
                RevealItem(visible, 4) {
                    RecapLabeledCard(
                        label = stringResource(Res.string.recap_speed_fastest_reader_label),
                        text = stringResource(Res.string.recap_speed_fastest_reader, reader.name, formatWaitDuration(reader.medianMillis)),
                        theme = theme
                    )
                }
            }
            habits.longestMonologue?.let { monologue ->
                RevealItem(visible, 5) {
                    RecapLabeledCard(
                        label = stringResource(Res.string.recap_speed_monologue_label),
                        text = stringResource(
                            Res.string.recap_speed_monologue,
                            monologue.messageCount,
                            monologue.partnerName,
                            monologue.startedAtFormatted
                        ),
                        theme = theme
                    )
                }
            }
        }
    }
}

/** Who opens the direct chats: your share overall, per closest partner, and your go-to opener. */
@Composable
fun RecapChatStarterPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.ChatStarter
    val habits = recap.chatHabits
    val percent = habits.startedByMePercent ?: return

    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_starter_title), color = theme.accent)
        }
        Spacer(Modifier.height(24.dp))
        RevealItem(visible, 1) {
            RecapBody(text = stringResource(Res.string.recap_starter_intro), color = theme.onBackground, fontSize = 19.sp)
        }
        RevealItem(visible, 2) {
            RecapBigText(text = stringResource(Res.string.recap_percent, percent), color = theme.accent)
        }
        RevealItem(visible, 3) {
            RecapBody(
                text = stringResource(Res.string.recap_starter_label, formatCount(habits.conversationsStartedByMe)),
                color = theme.onBackground.copy(alpha = 0.8f),
                fontSize = 18.sp
            )
        }

        if (habits.starterPerPartner.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            RevealItem(visible, 4) {
                RecapEyebrow(text = stringResource(Res.string.recap_starter_partners_label), color = theme.accent.copy(alpha = 0.8f))
            }
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                habits.starterPerPartner.forEachIndexed { index, partner ->
                    RevealItem(visible, 5 + index) {
                        StarterPartnerRow(partner = partner, theme = theme)
                    }
                }
            }
        }

        habits.topOpener?.let { opener ->
            Spacer(Modifier.height(20.dp))
            RevealItem(visible, 5 + habits.starterPerPartner.size) {
                RecapLabeledCard(
                    label = stringResource(Res.string.recap_starter_opener_label),
                    text = stringResource(Res.string.recap_quote_count, opener.text, formatCount(opener.count)),
                    theme = theme
                )
            }
        }
    }
}

/** Partner name with "8 of 12 by you" and a bar filled to your share. */
@Composable
private fun StarterPartnerRow(partner: StarterPartnerUi, theme: RecapPageTheme) {
    val share = (partner.startedByMe.toFloat() / partner.conversations).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.onBackground.copy(alpha = 0.09f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = partner.name,
                color = theme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(Res.string.recap_starter_partner, formatCount(partner.startedByMe), formatCount(partner.conversations)),
                color = theme.onBackground.copy(alpha = 0.75f),
                fontSize = 14.sp,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(theme.onBackground.copy(alpha = 0.16f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(share)
                    .height(4.dp)
                    .background(theme.accent)
            )
        }
    }
}

/** Your most typed emoji (in message text, all chats) big, the runners-up below. */
@Composable
fun RecapChatEmojiPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.ChatEmoji
    val emojis = recap.chatHabits.topEmojiTyped
    val top = emojis.firstOrNull() ?: return

    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_emoji_title), color = theme.accent)
        }
        Spacer(Modifier.height(24.dp))
        RevealItem(visible, 1) {
            RecapBigText(text = top.emoji, color = theme.onBackground, maxFontSize = 120.sp)
        }
        RevealItem(visible, 2) {
            RecapBody(
                text = stringResource(Res.string.recap_emoji_typed, formatCount(top.count)),
                color = theme.onBackground.copy(alpha = 0.8f),
                fontSize = 20.sp
            )
        }
        if (emojis.size > 1) {
            Spacer(Modifier.height(28.dp))
            RevealItem(visible, 3) {
                EmojiRow(
                    title = stringResource(Res.string.recap_emoji_more_label),
                    emojis = emojis.drop(1),
                    theme = theme
                )
            }
        }
    }
}

/** A wait like a read or reply time: seconds below a minute, days above a day. */
@Composable
private fun formatWaitDuration(millis: Long): String = when {
    millis < MINUTE_MILLIS -> stringResource(Res.string.recap_duration_s, (millis / SECOND_MILLIS).toInt())
    millis < HOUR_MILLIS -> stringResource(Res.string.recap_usage_duration_m, (millis / MINUTE_MILLIS).toInt())
    millis < DAY_MILLIS -> stringResource(
        Res.string.recap_usage_duration_hm,
        (millis / HOUR_MILLIS).toInt(),
        ((millis % HOUR_MILLIS) / MINUTE_MILLIS).toInt()
    )
    else -> stringResource(
        Res.string.recap_duration_dh,
        (millis / DAY_MILLIS).toInt(),
        ((millis % DAY_MILLIS) / HOUR_MILLIS).toInt()
    )
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapPartnerUi
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageTheme
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageThemes
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.recap_friend_requests_sent
import schneaggchatv3mp.composeapp.generated.resources.recap_friends_label
import schneaggchatv3mp.composeapp.generated.resources.recap_from_you_from_them
import schneaggchatv3mp.composeapp.generated.resources.recap_group_message_count
import schneaggchatv3mp.composeapp.generated.resources.recap_groups_created
import schneaggchatv3mp.composeapp.generated.resources.recap_groups_member_of
import schneaggchatv3mp.composeapp.generated.resources.recap_groups_title
import schneaggchatv3mp.composeapp.generated.resources.recap_inner_circle
import schneaggchatv3mp.composeapp.generated.resources.recap_most_active_group
import schneaggchatv3mp.composeapp.generated.resources.recap_most_reacted_message
import schneaggchatv3mp.composeapp.generated.resources.recap_new_friends_this_year
import schneaggchatv3mp.composeapp.generated.resources.recap_poll_votes_cast
import schneaggchatv3mp.composeapp.generated.resources.recap_polls_created
import schneaggchatv3mp.composeapp.generated.resources.recap_reactions_given
import schneaggchatv3mp.composeapp.generated.resources.recap_reactions_received
import schneaggchatv3mp.composeapp.generated.resources.recap_reactions_title
import schneaggchatv3mp.composeapp.generated.resources.recap_social_title
import schneaggchatv3mp.composeapp.generated.resources.recap_texts_number
import schneaggchatv3mp.composeapp.generated.resources.recap_top_emoji_given
import schneaggchatv3mp.composeapp.generated.resources.recap_top_emoji_received
import schneaggchatv3mp.composeapp.generated.resources.recap_top_partner_intro

// Story pages about the people around the user: top contacts, reactions, friends, groups.

@Composable
fun RecapTopContactsPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.TopContacts
    val partners = recap.topPartners.take(5)
    val first = partners.firstOrNull()

    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_inner_circle), color = theme.accent, maxFontSize = 44.sp)
        }
        if (first != null) {
            Spacer(Modifier.height(24.dp))
            RevealItem(visible, 1) {
                RecapEyebrow(text = stringResource(Res.string.recap_top_partner_intro), color = theme.accent.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(12.dp))
            RevealItem(visible, 2) {
                TopPartnerHero(partner = first, theme = theme)
            }
        }
        if (partners.size > 1) {
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                partners.drop(1).forEachIndexed { index, partner ->
                    RevealItem(visible, index + 3) {
                        PartnerRow(rank = index + 2, partner = partner, theme = theme)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopPartnerHero(partner: RecapPartnerUi, theme: RecapPageTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(theme.accent)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfilePictureView(
            filepath = partner.profilePictureFilePath,
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = partner.name,
                color = theme.backgroundTop,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.recap_texts_number, partner.messagesExchanged.toInt()),
                color = theme.backgroundTop.copy(alpha = 0.9f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    Res.string.recap_from_you_from_them,
                    formatCount(partner.messagesFromMe),
                    formatCount(partner.messagesFromThem)
                ),
                color = theme.backgroundTop.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PartnerRow(rank: Int, partner: RecapPartnerUi, theme: RecapPageTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.accent.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            color = theme.accent.copy(alpha = 0.7f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(24.dp)
        )
        ProfilePictureView(
            filepath = partner.profilePictureFilePath,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = partner.name,
                color = theme.accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    Res.string.recap_from_you_from_them,
                    formatCount(partner.messagesFromMe),
                    formatCount(partner.messagesFromThem)
                ),
                color = theme.accent.copy(alpha = 0.65f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.recap_texts_number, partner.messagesExchanged.toInt()),
            color = theme.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun RecapReactionsPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Reactions
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_reactions_title), color = theme.accent)
        }
        Spacer(Modifier.height(24.dp))
        RecapBigNumber(target = recap.reactionsGiven, running = visible, color = theme.onBackground, maxFontSize = 84.sp)
        RevealItem(visible, 1) {
            RecapBody(
                text = stringResource(Res.string.recap_reactions_given),
                color = theme.onBackground.copy(alpha = 0.8f),
                fontSize = 20.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        RevealItem(visible, 2) {
            RecapBody(
                text = stringResource(Res.string.recap_reactions_received, formatCount(recap.reactionsReceived)),
                color = theme.secondary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (recap.topEmojiGiven.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            RevealItem(visible, 3) {
                EmojiRow(
                    title = stringResource(Res.string.recap_top_emoji_given),
                    emojis = recap.topEmojiGiven,
                    theme = theme
                )
            }
        }
        if (recap.topEmojiReceived.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            RevealItem(visible, 4) {
                EmojiRow(
                    title = stringResource(Res.string.recap_top_emoji_received),
                    emojis = recap.topEmojiReceived,
                    theme = theme
                )
            }
        }
        recap.mostReactedMessage?.let { mostReacted ->
            Spacer(Modifier.height(24.dp))
            RevealItem(visible, 5) {
                RecapCard(theme = theme) {
                    RecapBody(
                        text = stringResource(Res.string.recap_most_reacted_message, mostReacted.reactionCount),
                        color = theme.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "“${mostReacted.preview}”",
                        color = theme.onBackground.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun RecapSocialPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Social
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_social_title), color = theme.accent)
        }
        Spacer(Modifier.height(24.dp))
        RecapBigNumber(target = recap.friendsCount.toLong(), running = visible, color = theme.onBackground)
        RevealItem(visible, 1) {
            RecapBody(
                text = stringResource(Res.string.recap_friends_label),
                color = theme.onBackground.copy(alpha = 0.8f),
                fontSize = 22.sp
            )
        }
        Spacer(Modifier.height(28.dp))
        RevealItem(visible, 2) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecapBody(
                    text = stringResource(Res.string.recap_new_friends_this_year, recap.newFriendsThisYear),
                    color = theme.secondary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                RecapBody(
                    text = stringResource(Res.string.recap_friend_requests_sent, recap.friendRequestsSentThisYear.toInt()),
                    color = theme.onBackground.copy(alpha = 0.75f),
                    fontSize = 16.sp
                )
            }
        }
        if (recap.pollsCreated > 0 || recap.pollVotesCast > 0) {
            Spacer(Modifier.height(24.dp))
            RevealItem(visible, 3) {
                PillFlow(
                    pills = listOf(
                        stringResource(Res.string.recap_polls_created, recap.pollsCreated.toInt()),
                        stringResource(Res.string.recap_poll_votes_cast, recap.pollVotesCast.toInt())
                    ),
                    theme = theme
                )
            }
        }
    }
}

@Composable
fun RecapGroupsPage(recap: RecapUi, visible: Boolean) {
    val theme = RecapPageThemes.Groups
    RecapPage(theme = theme, visible = visible) {
        RevealItem(visible, 0) {
            RecapHeadline(text = stringResource(Res.string.recap_groups_title), color = theme.accent)
        }
        Spacer(Modifier.height(24.dp))
        RecapBigNumber(target = recap.groupsMemberOf.toLong(), running = visible, color = theme.onBackground)
        RevealItem(visible, 1) {
            RecapBody(
                text = stringResource(Res.string.recap_groups_member_of),
                color = theme.onBackground.copy(alpha = 0.8f),
                fontSize = 22.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        RevealItem(visible, 2) {
            RecapBody(
                text = stringResource(Res.string.recap_groups_created, recap.groupsCreated),
                color = theme.secondary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        recap.mostActiveGroup?.let { group ->
            Spacer(Modifier.height(28.dp))
            RevealItem(visible, 3) {
                RecapCard(theme = theme) {
                    RecapEyebrow(text = stringResource(Res.string.recap_most_active_group), color = theme.accent)
                    Text(
                        text = group.name,
                        color = theme.onBackground,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    RecapBody(
                        text = stringResource(Res.string.recap_group_message_count, formatCount(group.messageCount)),
                        color = theme.onBackground.copy(alpha = 0.7f),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

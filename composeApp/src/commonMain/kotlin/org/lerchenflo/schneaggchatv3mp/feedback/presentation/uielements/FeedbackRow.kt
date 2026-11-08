package org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements

import androidx.compose.foundation.background
import schneaggchatv3mp.composeapp.generated.resources.feedback_updated_badge
import schneaggchatv3mp.composeapp.generated.resources.feedback_premium
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackMark
import org.lerchenflo.schneaggchatv3mp.sharedUi.modifiers.AnimatedGradientOutlineDefaults
import org.lerchenflo.schneaggchatv3mp.sharedUi.modifiers.animatedGradientOutline
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NotInterested
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTag
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.colors
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.labelRes
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.feedback_dev_comment
import schneaggchatv3mp.composeapp.generated.resources.feedback_new_badge
import schneaggchatv3mp.composeapp.generated.resources.feedback_new_comments
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_cant_repro
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_didnt_know
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_down
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_me_too
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_not_needed
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_up

/**
 * One entry of the board as a compact, GitHub-like row: title with its New / Updated label, status
 * and tags, then creator, date, the vote buttons and the comment count. Description and comments
 * are in the detail dialog. Premium features get an animated outline.
 */
@Composable
fun FeedbackRow(
    entry: FeedbackEntry,
    mark: FeedbackMark?,
    onClick: () -> Unit,
    onVoteClick: (FeedbackVote) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val premiumModifier = if (entry.isPremium) {
        Modifier.animatedGradientOutline(colors = AnimatedGradientOutlineDefaults.themeColors(), shape = shape)
    } else Modifier

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(premiumModifier)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            when (mark) {
                FeedbackMark.NEW -> FeedbackPill(
                    text = stringResource(Res.string.feedback_new_badge),
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary,
                )
                FeedbackMark.UPDATED -> FeedbackPill(
                    text = stringResource(Res.string.feedback_updated_badge),
                    container = MaterialTheme.colorScheme.tertiary,
                    content = MaterialTheme.colorScheme.onTertiary,
                )
                null -> Unit
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FeedbackStatusPill(entry.status)
            if (entry.newCommentCount > 0) {
                FeedbackPill(
                    text = pluralStringResource(Res.plurals.feedback_new_comments, entry.newCommentCount, entry.newCommentCount),
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (entry.isPremium) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = stringResource(Res.string.feedback_premium),
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            val tagLabels = entry.tags.map { stringResource(it.labelRes()) }
            if (tagLabels.isNotEmpty()) {
                Text(
                    text = tagLabels.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${authorName(entry)} · ${millisToString(entry.createdAt, DATE_FORMAT)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            CompactVoteButtons(entry = entry, onVoteClick = onVoteClick)
            CommentCount(entry.commentCount)
            if (entry.latestDevComment != null) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = stringResource(Res.string.feedback_dev_comment),
                    modifier = Modifier.padding(start = 4.dp).size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** The same votes as [FeedbackVoteRow], as small icon + count buttons for the list row. */
@Composable
private fun CompactVoteButtons(
    entry: FeedbackEntry,
    onVoteClick: (FeedbackVote) -> Unit,
) {
    val isBug = entry.type == FeedbackType.BUG
    CompactVoteButton(
        selected = entry.myVote == FeedbackVote.UP,
        count = entry.upvotes,
        contentDescription = stringResource(if (isBug) Res.string.feedback_vote_me_too else Res.string.feedback_vote_up),
        selectedIcon = Icons.Filled.ThumbUp,
        icon = Icons.Outlined.ThumbUp,
        onClick = { onVoteClick(FeedbackVote.UP) },
    )
    CompactVoteButton(
        selected = entry.myVote == FeedbackVote.DOWN,
        count = entry.downvotes,
        contentDescription = stringResource(if (isBug) Res.string.feedback_vote_cant_repro else Res.string.feedback_vote_down),
        selectedIcon = Icons.Filled.ThumbDown,
        icon = Icons.Outlined.ThumbDown,
        onClick = { onVoteClick(FeedbackVote.DOWN) },
    )
    if (entry.status == FeedbackStatus.IMPLEMENTED) {
        CompactVoteButton(
            selected = entry.myVote == FeedbackVote.DIDNT_KNOW,
            count = entry.didntKnowCount,
            contentDescription = stringResource(Res.string.feedback_vote_didnt_know),
            selectedIcon = Icons.Filled.Lightbulb,
            icon = Icons.Outlined.Lightbulb,
            onClick = { onVoteClick(FeedbackVote.DIDNT_KNOW) },
        )
    }
    if (!isBug) {
        CompactVoteButton(
            selected = entry.myVote == FeedbackVote.NOT_NEEDED,
            count = entry.notNeededCount,
            contentDescription = stringResource(Res.string.feedback_vote_not_needed),
            selectedIcon = Icons.Filled.NotInterested,
            icon = Icons.Outlined.NotInterested,
            onClick = { onVoteClick(FeedbackVote.NOT_NEEDED) },
        )
    }
}

@Composable
private fun CompactVoteButton(
    selected: Boolean,
    count: Int,
    contentDescription: String,
    selectedIcon: ImageVector,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val content = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .then(if (selected) Modifier.background(MaterialTheme.colorScheme.secondaryContainer) else Modifier)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .heightIn(min = 32.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = content,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}

@Composable
fun FeedbackPill(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
fun FeedbackStatusPill(status: FeedbackStatus, modifier: Modifier = Modifier) {
    val colors = status.colors()
    FeedbackPill(
        text = stringResource(status.labelRes()),
        container = colors.container,
        content = colors.content,
        modifier = modifier,
    )
}

/** Tags as neutral pills; [isPremium] puts a highlighted "Premium" pill first. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackTagRow(tags: List<FeedbackTag>, modifier: Modifier = Modifier, isPremium: Boolean = false) {
    if (tags.isEmpty() && !isPremium) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isPremium) {
            FeedbackPill(
                text = stringResource(Res.string.feedback_premium),
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        tags.forEach { tag ->
            val colors = tag.colors()
            FeedbackPill(
                text = stringResource(tag.labelRes()),
                container = colors.container,
                content = colors.content,
            )
        }
    }
}

/** Highlighted block for a comment written by a developer. */
@Composable
fun DevCommentBlock(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    footer: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        //Plain label on the container instead of a second filled pill - the block is tinted already
        Text(
            text = stringResource(Res.string.feedback_dev_comment),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
        footer?.invoke()
    }
}

@Composable
private fun CommentCount(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Comment,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Features: thumbs up / down and "I don't need this", plus "didn't know" once implemented.
 * Bugs: "me too" / "can't repro".
 * Tapping the active button again removes the vote.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackVoteRow(
    entry: FeedbackEntry,
    onVoteClick: (FeedbackVote) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBug = entry.type == FeedbackType.BUG

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VoteChip(
            selected = entry.myVote == FeedbackVote.UP,
            count = entry.upvotes,
            label = if (isBug) stringResource(Res.string.feedback_vote_me_too) else null,
            contentDescription = stringResource(if (isBug) Res.string.feedback_vote_me_too else Res.string.feedback_vote_up),
            selectedIcon = Icons.Filled.ThumbUp,
            icon = Icons.Outlined.ThumbUp,
            onClick = { onVoteClick(FeedbackVote.UP) },
        )
        VoteChip(
            selected = entry.myVote == FeedbackVote.DOWN,
            count = entry.downvotes,
            label = if (isBug) stringResource(Res.string.feedback_vote_cant_repro) else null,
            contentDescription = stringResource(if (isBug) Res.string.feedback_vote_cant_repro else Res.string.feedback_vote_down),
            selectedIcon = Icons.Filled.ThumbDown,
            icon = Icons.Outlined.ThumbDown,
            onClick = { onVoteClick(FeedbackVote.DOWN) },
        )
        if (entry.status == FeedbackStatus.IMPLEMENTED) {
            VoteChip(
                selected = entry.myVote == FeedbackVote.DIDNT_KNOW,
                count = entry.didntKnowCount,
                label = stringResource(Res.string.feedback_vote_didnt_know),
                contentDescription = stringResource(Res.string.feedback_vote_didnt_know),
                selectedIcon = Icons.Filled.Lightbulb,
                icon = Icons.Outlined.Lightbulb,
                onClick = { onVoteClick(FeedbackVote.DIDNT_KNOW) },
            )
        }
        // Neutral: seen it, no use for it, but not against it - doesn't change the score
        if (!isBug) {
            VoteChip(
                selected = entry.myVote == FeedbackVote.NOT_NEEDED,
                count = entry.notNeededCount,
                label = stringResource(Res.string.feedback_vote_not_needed),
                contentDescription = stringResource(Res.string.feedback_vote_not_needed),
                selectedIcon = Icons.Filled.NotInterested,
                icon = Icons.Outlined.NotInterested,
                onClick = { onVoteClick(FeedbackVote.NOT_NEEDED) },
            )
        }
    }
}

@Composable
private fun VoteChip(
    selected: Boolean,
    count: Int,
    label: String?,
    contentDescription: String,
    selectedIcon: ImageVector,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(if (label != null) "$label · $count" else count.toString()) },
        leadingIcon = {
            Icon(
                imageVector = if (selected) selectedIcon else icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
    )
}

@Preview
@Composable
private fun FeedbackRowPreview() {
    SchneaggchatTheme {
        Column {
            FeedbackRow(
                entry = previewFeedbackEntry.copy(isOwn = true, newCommentCount = 2),
                mark = FeedbackMark.UPDATED,
                onClick = {},
                onVoteClick = {},
            )
            FeedbackRow(
                entry = previewFeedbackEntry.copy(id = "2", isPremium = true, latestDevComment = null),
                mark = FeedbackMark.NEW,
                onClick = {},
                onVoteClick = {},
            )
        }
    }
}

internal val previewFeedbackEntry = FeedbackEntry(
    id = "1",
    type = FeedbackType.FEATURE,
    title = "Dark mode for the map",
    description = "The map is very bright at night. A dark map style would be great.",
    tags = listOf(FeedbackTag.MAP, FeedbackTag.UI),
    status = FeedbackStatus.PLANNED,
    upvotes = 12,
    downvotes = 2,
    didntKnowCount = 0,
    notNeededCount = 0,
    myVote = FeedbackVote.UP,
    commentCount = 4,
    latestDevComment = "Good idea, coming with the next map update.",
    creatorName = "anna",
    isOwn = false,
    canDelete = false,
    isSeeded = false,
    isPremium = false,
    createdAt = 0L,
    updatedAt = 0L,
    activityAt = 0L,
)

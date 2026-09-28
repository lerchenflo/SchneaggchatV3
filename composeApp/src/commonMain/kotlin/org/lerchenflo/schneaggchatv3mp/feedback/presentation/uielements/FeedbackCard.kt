package org.lerchenflo.schneaggchatv3mp.feedback.presentation.uielements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_cant_repro
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_didnt_know
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_down
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_me_too
import schneaggchatv3mp.composeapp.generated.resources.feedback_vote_up

/**
 * One entry of the board: status, title, short description, tags, the newest dev comment and the
 * vote buttons. Done entries (implemented / fixed) get a tinted card so they stand apart from
 * requests; entries younger than two days get an outline and a "new" badge.
 */
@Composable
fun FeedbackCard(
    entry: FeedbackEntry,
    isPinned: Boolean,
    onClick: () -> Unit,
    onVoteClick: (FeedbackVote) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isDone) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        border = if (isPinned) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FeedbackStatusPill(entry.status)
                if (isPinned) {
                    FeedbackPill(
                        text = stringResource(Res.string.feedback_new_badge),
                        container = MaterialTheme.colorScheme.primary,
                        content = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Spacer(Modifier.weight(1f))
                CommentCount(entry.commentCount)
            }

            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (entry.descriptionPreview.isNotBlank()) {
                Text(
                    text = entry.descriptionPreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            FeedbackTagRow(entry.tags)

            entry.latestDevComment?.let { comment ->
                DevCommentBlock(text = comment, maxLines = 3)
            }

            FeedbackVoteRow(entry = entry, onVoteClick = onVoteClick)
        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackTagRow(tags: List<FeedbackTag>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
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
        FeedbackPill(
            text = stringResource(Res.string.feedback_dev_comment),
            container = MaterialTheme.colorScheme.primary,
            content = MaterialTheme.colorScheme.onPrimary,
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
 * Features: thumbs up / down, plus "didn't know" once implemented. Bugs: "me too" / "can't repro".
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
private fun FeedbackCardPreview() {
    SchneaggchatTheme {
        FeedbackCard(
            entry = previewFeedbackEntry,
            isPinned = true,
            onClick = {},
            onVoteClick = {},
        )
    }
}

internal val previewFeedbackEntry = FeedbackEntry(
    id = "1",
    type = FeedbackType.FEATURE,
    title = "Dark mode for the map",
    descriptionPreview = "The map is very bright at night. A dark map style would be great.",
    tags = listOf(FeedbackTag.MAP, FeedbackTag.UI),
    status = FeedbackStatus.PLANNED,
    upvotes = 12,
    downvotes = 2,
    didntKnowCount = 0,
    myVote = FeedbackVote.UP,
    commentCount = 4,
    latestDevComment = "Good idea, coming with the next map update.",
    creatorName = "anna",
    isOwn = false,
    canDelete = false,
    isSeeded = false,
    createdAt = 0L,
)

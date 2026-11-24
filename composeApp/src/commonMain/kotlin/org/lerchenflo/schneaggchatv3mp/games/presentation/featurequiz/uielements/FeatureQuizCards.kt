package org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.ThumbsUpDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz.FeatureQuizAnswerResult
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_correct
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_didnt_know
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wish_title
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wrong
import schneaggchatv3mp.composeapp.generated.resources.featurevoting_feature_title

private val CardShape = RoundedCornerShape(20.dp)

/** Right / wrong / didn't know after an answer, with the question's explanation. */
@Composable
fun FeatureQuizVerdictCard(
    result: FeatureQuizAnswerResult,
    correctAnswer: String,
    explanation: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (result) {
        FeatureQuizAnswerResult.CORRECT -> colors.primaryContainer to colors.onPrimaryContainer
        FeatureQuizAnswerResult.WRONG_DETAIL -> colors.errorContainer to colors.onErrorContainer
        FeatureQuizAnswerResult.DIDNT_KNOW -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    val icon = when (result) {
        FeatureQuizAnswerResult.CORRECT -> Icons.Filled.CheckCircle
        FeatureQuizAnswerResult.WRONG_DETAIL -> Icons.Filled.Cancel
        FeatureQuizAnswerResult.DIDNT_KNOW -> Icons.Filled.Lightbulb
    }
    val verdict = when (result) {
        FeatureQuizAnswerResult.CORRECT -> stringResource(Res.string.featurequiz_correct)
        FeatureQuizAnswerResult.WRONG_DETAIL -> stringResource(Res.string.featurequiz_wrong, correctAnswer)
        FeatureQuizAnswerResult.DIDNT_KNOW -> stringResource(Res.string.featurequiz_didnt_know, correctAnswer)
    }

    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = verdict, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = explanation, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** An open wish from the feedback board, shown instead of a question. */
@Composable
fun FeatureQuizWishCard(entry: FeedbackEntry, modifier: Modifier = Modifier) {
    EntryCard(
        entry = entry,
        icon = Icons.Outlined.Lightbulb,
        label = stringResource(Res.string.featurequiz_wish_title),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier,
    )
}

/** A feature the app has, shown in the vote-only mode. */
@Composable
fun FeatureQuizFeatureCard(entry: FeedbackEntry, modifier: Modifier = Modifier) {
    EntryCard(
        entry = entry,
        icon = Icons.Outlined.CheckCircle,
        label = stringResource(Res.string.featurevoting_feature_title),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
    )
}

/** A board entry's title and description under a small label. */
@Composable
private fun EntryCard(
    entry: FeedbackEntry,
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }
            Text(text = entry.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (entry.description.isNotBlank()) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The vote prompt around the feedback board's vote buttons. */
@Composable
fun FeatureQuizVoteCard(
    prompt: String,
    modifier: Modifier = Modifier,
    voteButtons: @Composable () -> Unit,
) {
    OutlinedCard(shape = CardShape, modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ThumbsUpDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(text = prompt, style = MaterialTheme.typography.titleSmall)
            }
            voteButtons()
        }
    }
}

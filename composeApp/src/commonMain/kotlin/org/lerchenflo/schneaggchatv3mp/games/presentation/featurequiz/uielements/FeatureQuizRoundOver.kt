package org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz.FeatureQuizResultTier
import org.lerchenflo.schneaggchatv3mp.utilities.formatMillis
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_done
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_play_again
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_progress
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_result_good
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_result_great
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_result_low
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_round_over

private const val RING_ANIMATION_MILLIS = 900

/** Result screen: trophy, a score ring that fills up, a message for the score, and the time. */
@Composable
fun FeatureQuizRoundOver(
    points: Int,
    questionCount: Int,
    elapsedMillis: Long,
    onPlayAgainClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val share = if (questionCount == 0) 0f else points.toFloat() / questionCount
    val ringProgress = remember { Animatable(0f) }
    LaunchedEffect(share) {
        ringProgress.animateTo(share, tween(RING_ANIMATION_MILLIS, easing = FastOutSlowInEasing))
    }
    val message = when (FeatureQuizResultTier.of(points, questionCount)) {
        FeatureQuizResultTier.GREAT -> Res.string.featurequiz_result_great
        FeatureQuizResultTier.GOOD -> Res.string.featurequiz_result_good
        FeatureQuizResultTier.LOW -> Res.string.featurequiz_result_low
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = stringResource(Res.string.featurequiz_round_over),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(168.dp)) {
            CircularProgressIndicator(
                progress = { ringProgress.value },
                strokeWidth = 12.dp,
                strokeCap = StrokeCap.Round,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                text = stringResource(Res.string.featurequiz_progress, points, questionCount),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        FeatureQuizStatChip(icon = Icons.Outlined.Timer, text = formatMillis(elapsedMillis))

        Spacer(Modifier.size(4.dp))
        Button(
            onClick = onPlayAgainClick,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(Res.string.featurequiz_play_again))
        }
        OutlinedButton(
            onClick = onDoneClick,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.featurequiz_done))
        }
    }
}

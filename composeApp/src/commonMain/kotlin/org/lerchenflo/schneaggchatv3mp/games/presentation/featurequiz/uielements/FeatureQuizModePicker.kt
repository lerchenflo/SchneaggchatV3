package org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.ThumbsUpDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_mode_quiz_description
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_mode_quiz_title
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_mode_title
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_mode_vote_description
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_mode_vote_title

/** Start page of the quiz: the scored quiz, or only voting on every feature without a vote. */
@Composable
fun FeatureQuizModePicker(
    onQuizClick: () -> Unit,
    onVoteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(Res.string.featurequiz_mode_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        ModeCard(
            icon = Icons.Filled.Psychology,
            title = stringResource(Res.string.featurequiz_mode_quiz_title),
            description = stringResource(Res.string.featurequiz_mode_quiz_description),
            onClick = onQuizClick,
        )
        ModeCard(
            icon = Icons.Outlined.ThumbsUpDown,
            title = stringResource(Res.string.featurequiz_mode_vote_title),
            description = stringResource(Res.string.featurequiz_mode_vote_description),
            onClick = onVoteClick,
        )
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

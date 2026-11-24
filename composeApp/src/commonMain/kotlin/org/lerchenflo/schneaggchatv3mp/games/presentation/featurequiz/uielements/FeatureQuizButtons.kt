package org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private val ButtonShape = RoundedCornerShape(16.dp)
private val ButtonPadding = PaddingValues(vertical = 14.dp)

/** Goes on to the next card, its icon after the text. */
@Composable
fun FeatureQuizNextButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(text)
        Spacer(Modifier.size(8.dp))
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

/** Leaves a card without an answer, next to [FeatureQuizNextButton]. */
@Composable
fun FeatureQuizSkipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(text)
    }
}

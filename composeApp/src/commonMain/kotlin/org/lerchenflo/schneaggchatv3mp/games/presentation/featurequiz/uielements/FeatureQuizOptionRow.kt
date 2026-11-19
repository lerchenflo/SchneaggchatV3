package org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz.uielements

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** How an answer option looks: open before the answer, then right, the wrong pick, or out of play. */
enum class FeatureQuizOptionLook { OPEN, CORRECT, WRONG_PICK, DIMMED }

/** One answer option: a letter badge, the text, and after the answer a check or cross. */
@Composable
fun FeatureQuizOptionRow(
    letter: Char,
    text: String,
    look: FeatureQuizOptionLook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(colors.containerFor(look), label = "optionContainer")
    val content by animateColorAsState(colors.contentFor(look), label = "optionContent")
    val border by animateColorAsState(colors.borderFor(look), label = "optionBorder")
    val highlighted = look == FeatureQuizOptionLook.CORRECT || look == FeatureQuizOptionLook.WRONG_PICK

    Surface(
        onClick = onClick,
        enabled = look == FeatureQuizOptionLook.OPEN,
        shape = RoundedCornerShape(16.dp),
        color = container,
        contentColor = content,
        border = BorderStroke(if (highlighted) 2.dp else 1.dp, border),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (look == FeatureQuizOptionLook.DIMMED) 0.6f else 1f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            LetterBadge(letter = letter, look = look)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            when (look) {
                FeatureQuizOptionLook.CORRECT -> Icon(Icons.Filled.CheckCircle, contentDescription = null)
                FeatureQuizOptionLook.WRONG_PICK -> Icon(Icons.Filled.Cancel, contentDescription = null)
                else -> Unit
            }
        }
    }
}

@Composable
private fun LetterBadge(letter: Char, look: FeatureQuizOptionLook) {
    val colors = MaterialTheme.colorScheme
    val (background, content) = when (look) {
        FeatureQuizOptionLook.OPEN -> colors.secondaryContainer to colors.onSecondaryContainer
        FeatureQuizOptionLook.CORRECT -> colors.primary to colors.onPrimary
        FeatureQuizOptionLook.WRONG_PICK -> colors.error to colors.onError
        FeatureQuizOptionLook.DIMMED -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .background(background, CircleShape),
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = content,
        )
    }
}

private fun ColorScheme.containerFor(look: FeatureQuizOptionLook): Color = when (look) {
    FeatureQuizOptionLook.OPEN -> surfaceContainerHigh
    FeatureQuizOptionLook.CORRECT -> primaryContainer
    FeatureQuizOptionLook.WRONG_PICK -> errorContainer
    FeatureQuizOptionLook.DIMMED -> surfaceContainerLow
}

private fun ColorScheme.contentFor(look: FeatureQuizOptionLook): Color = when (look) {
    FeatureQuizOptionLook.OPEN -> onSurface
    FeatureQuizOptionLook.CORRECT -> onPrimaryContainer
    FeatureQuizOptionLook.WRONG_PICK -> onErrorContainer
    FeatureQuizOptionLook.DIMMED -> onSurfaceVariant
}

private fun ColorScheme.borderFor(look: FeatureQuizOptionLook): Color = when (look) {
    FeatureQuizOptionLook.OPEN, FeatureQuizOptionLook.DIMMED -> outlineVariant
    FeatureQuizOptionLook.CORRECT -> primary
    FeatureQuizOptionLook.WRONG_PICK -> error
}

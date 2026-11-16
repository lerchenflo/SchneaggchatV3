package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Round icon of a split entry: a transfer icon, the emoji picked for an expense, or the default expense icon. */
@Composable
fun SplitEntryIcon(
    isTransfer: Boolean,
    emoji: String?,
) {
    when {
        isTransfer -> RoundIcon(icon = Icons.Default.SwapHoriz)
        emoji != null -> RoundEmoji(emoji = emoji)
        else -> RoundIcon(icon = Icons.AutoMirrored.Filled.ReceiptLong)
    }
}

@Composable
fun RoundIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/** Same circle as [RoundIcon], showing the emoji the user picked for an expense. */
@Composable
private fun RoundEmoji(emoji: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1
        )
    }
}

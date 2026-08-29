package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString

const val DATE_CHIP_FORMAT = "dd.MM.yyyy"

/**
 * Small centered date pill, used as the floating date indicator in the chat list and as the
 * sticky day header in the events list.
 */
@Composable
fun DateChip(
    millis: Long,
    modifier: Modifier = Modifier
) {
    DateChip(
        text = millisToString(millis, DATE_CHIP_FORMAT),
        modifier = modifier
    )
}

@Composable
fun DateChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium
    )
}

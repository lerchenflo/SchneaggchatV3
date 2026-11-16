package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.settings.domain.EventsViewMode
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.events_view_day
import schneaggchatv3mp.composeapp.generated.resources.events_view_list
import schneaggchatv3mp.composeapp.generated.resources.events_view_month
import schneaggchatv3mp.composeapp.generated.resources.events_view_upcoming

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsViewModeSwitch(
    selected: EventsViewMode,
    onSelect: (EventsViewMode) -> Unit,
    unseenEventCount: Int,
    modifier: Modifier = Modifier
) {
    val options = EventsViewMode.entries

    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                ViewModeLabel(
                    text = stringResource(mode.labelRes()),
                    badgeCount = if (mode == EventsViewMode.LIST) unseenEventCount else 0
                )
            }
        }
    }
}

/**
 * Segment label with an optional count badge placed after the text. Not a BadgedBox: that one
 * overlaps its anchor's top end by 12dp, which is meant for icons and covers the end of a text
 * label, and its badge is not measured, so the segment's shape clips it in a narrow segment. The
 * label shrinks with an ellipsis first so the badge always stays fully visible.
 */
@Composable
private fun ViewModeLabel(
    text: String,
    badgeCount: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (badgeCount > 0) {
            Badge { Text(if (badgeCount > 99) "99+" else badgeCount.toString()) }
        }
    }
}

private fun EventsViewMode.labelRes(): StringResource = when (this) {
    EventsViewMode.LIST -> Res.string.events_view_list
    EventsViewMode.DAY -> Res.string.events_view_day
    EventsViewMode.UPCOMING -> Res.string.events_view_upcoming
    EventsViewMode.MONTH -> Res.string.events_view_month
}

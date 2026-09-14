package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.events.domain.EventsViewMode
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
    hasUnseenEvents: Boolean,
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
                Box {
                    Text(
                        text = stringResource(mode.labelRes()),
                        maxLines = 1
                    )
                    if (mode == EventsViewMode.LIST && hasUnseenEvents) {
                        Badge()
                    }
                }
            }
        }
    }
}

private fun EventsViewMode.labelRes(): StringResource = when (this) {
    EventsViewMode.LIST -> Res.string.events_view_list
    EventsViewMode.DAY -> Res.string.events_view_day
    EventsViewMode.UPCOMING -> Res.string.events_view_upcoming
    EventsViewMode.MONTH -> Res.string.events_view_month
}

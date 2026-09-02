package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.calendar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.events.presentation.EventsViewMode
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.events_view_list
import schneaggchatv3mp.composeapp.generated.resources.events_view_month
import schneaggchatv3mp.composeapp.generated.resources.events_view_week

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsViewModeSwitch(
    selected: EventsViewMode,
    onSelect: (EventsViewMode) -> Unit,
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
                Text(text = stringResource(mode.labelRes()))
            }
        }
    }
}

private fun EventsViewMode.labelRes(): StringResource = when (this) {
    EventsViewMode.LIST -> Res.string.events_view_list
    EventsViewMode.WEEK -> Res.string.events_view_week
    EventsViewMode.MONTH -> Res.string.events_view_month
}

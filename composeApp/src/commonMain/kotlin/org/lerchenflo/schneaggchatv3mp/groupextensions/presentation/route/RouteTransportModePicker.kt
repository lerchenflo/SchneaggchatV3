package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.RouteTransportMode

/** Labeled chips for a transport mode: the route's default when it is created, a point's way to it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RouteTransportModePicker(
    label: String,
    selected: RouteTransportMode,
    onSelect: (RouteTransportMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RouteTransportMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    label = { Text(stringResource(mode.labelRes())) },
                    leadingIcon = {
                        Icon(imageVector = mode.icon(), contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }
        }
    }
}

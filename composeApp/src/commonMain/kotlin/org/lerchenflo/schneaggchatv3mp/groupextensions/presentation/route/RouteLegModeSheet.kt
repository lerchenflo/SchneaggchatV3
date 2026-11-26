package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.route_arrival_mode

/** How a leg is travelled, opened from its badge on the map; a pick saves at once. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteLegModeSheet(
    edit: RouteLegModeEdit,
    onAction: (RouteAction) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(RouteAction.OnLegModeDismiss) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // The point the leg leads to, or the path for its way back
            Text(text = edit.title, style = MaterialTheme.typography.titleLarge)
            RouteTransportModePicker(
                label = stringResource(Res.string.route_arrival_mode),
                selected = edit.mode,
                onSelect = { onAction(RouteAction.OnLegModeSelect(it)) },
            )
        }
    }
}

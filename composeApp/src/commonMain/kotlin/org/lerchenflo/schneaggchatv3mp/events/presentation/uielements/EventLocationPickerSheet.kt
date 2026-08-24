package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.CoordinateView
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.LocationPickerMap
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.event_pick_location_title
import schneaggchatv3mp.composeapp.generated.resources.ok

// Popup (not a bottom sheet) wrapping the reusable LocationPickerMap (schneaggmap feature) for
// picking an event's location.
@Composable
fun EventLocationPickerSheet(
    initialLocation: LatLong?,
    mapStyleUrl: String,
    onConfirm: (LatLong) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by remember { mutableStateOf(initialLocation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.event_pick_location_title))
        },
        text = {
            Column {
                LocationPickerMap(
                    selectedLocation = picked,
                    mapStyleUrl = mapStyleUrl,
                    onLocationSelected = { picked = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                )

                picked?.let { location ->
                    Spacer(modifier = Modifier.height(12.dp))
                    CoordinateView(coordinates = location)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { picked?.let(onConfirm) },
                enabled = picked != null
            ) {
                Text(text = stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.cancel))
            }
        }
    )
}

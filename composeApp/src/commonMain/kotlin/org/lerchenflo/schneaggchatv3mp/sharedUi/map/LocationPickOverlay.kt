package org.lerchenflo.schneaggchatv3mp.sharedUi.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.event_pick_location_hint
import schneaggchatv3mp.composeapp.generated.resources.event_pick_location_title
import schneaggchatv3mp.composeapp.generated.resources.event_use_this_location

/**
 * Focused "pick a coordinate" chrome over a map, shown instead of the screen's normal controls.
 * The pin stays fixed at the screen center; the user places it by panning the map underneath
 * (like a typical address picker), so there's no tap/drag handling here - [cameraTarget] is just
 * the live camera center converted to [LatLong]. Used by the Schneaggmap (event location) and the
 * route planner (move a point).
 */
@Composable
fun LocationPickOverlay(
    cameraTarget: LatLong,
    onConfirm: (LatLong) -> Unit,
    onCancel: () -> Unit,
    title: String = stringResource(Res.string.event_pick_location_title),
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onCancel,
                colors = IconButtonDefaults.iconButtonColors().copy(
                    containerColor = MaterialTheme.colorScheme.background
                )
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(Res.string.cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.background,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
                .offset(y = (-24).dp),
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.event_pick_location_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                CoordinateView(coordinates = cameraTarget)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onConfirm(cameraTarget) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.event_use_this_location))
                }
            }
        }
    }
}

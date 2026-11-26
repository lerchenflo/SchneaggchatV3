package org.lerchenflo.schneaggchatv3mp.sharedUi.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.utilities.copyToClipboard
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.latlong

/** Read-only coordinates, centered; a tap copies them. [trailing] sits right after the text. */
@Composable
fun CoordinateView(
    coordinates: LatLong,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val latitude = coordinates.lat.toString().take(8)
    val longitude = coordinates.long.toString().take(8)
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.latlong, latitude, longitude),
                modifier = Modifier.clickable { copyToClipboard(text = "$latitude, $longitude") }
            )
            trailing?.invoke()
        }
    }
}

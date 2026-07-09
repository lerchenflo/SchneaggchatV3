@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import org.lerchenflo.schneaggchatv3mp.utilities.distanceMeters
import org.lerchenflo.schneaggchatv3mp.utilities.formatDistance
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.open_chat
import schneaggchatv3mp.composeapp.generated.resources.open_location_in_maps
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_altitude_label
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_battery_label
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_distance_24h_label
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_distance_label
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_last_online_label
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_online
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_speed_label
import kotlin.math.roundToInt

/** One stat tile's content - an icon (with its own tint), a caption, and the formatted value. */
private data class StatTileData(
    val icon: ImageVector,
    val tint: Color,
    val caption: String,
    val value: String,
)

@Composable
fun UserInfoCard(
    user: User,
    isOnline: Boolean,
    ownLocation: LatLong?,
    onDismiss: () -> Unit,
    onOpenChat: (User) -> Unit,
    modifier: Modifier = Modifier,
    isOwnUser: Boolean = false,
) {

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp)) {

            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            val location = user.location
            val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

            val userDistanceText = remember(ownLocation, location) {
                if (ownLocation != null && location != null) {
                    formatDistance(distanceMeters(ownLocation, LatLong(lat = location.lat, long = location.long)))
                } else {
                    "--"
                }
            }

            //TODO: heading is available on user.location?.heading but intentionally not shown
            // here - there's currently no good way to render it usefully (no compass/rotation
            // display), so it's left out of the popup for now.
            val onlineColor = MaterialTheme.colorScheme.primary

            val tiles = buildList {
                add(
                    StatTileData(
                        icon = Icons.Default.Schedule,
                        tint = if (isOnline) onlineColor else onSurfaceVariant,
                        caption = stringResource(Res.string.schneaggmap_user_last_online_label),
                        value = if (isOnline) {
                            stringResource(Res.string.schneaggmap_user_online)
                        } else {
                            user.lastSeen?.let { millisToTimeDateOrYesterday(it) } ?: "-"
                        }
                    )
                )
                if (!isOwnUser) {
                    add(
                        StatTileData(
                            icon = Icons.Default.Straighten,
                            tint = onSurfaceVariant,
                            caption = stringResource(Res.string.schneaggmap_user_distance_label),
                            value = userDistanceText
                        )
                    )
                }
                location?.speed?.let { speed ->
                    add(
                        StatTileData(
                            icon = Icons.Default.Speed,
                            tint = onSurfaceVariant,
                            caption = stringResource(Res.string.schneaggmap_user_speed_label),
                            value = "${(speed * 3.6).roundToInt()} km/h"
                        )
                    )
                }
                location?.altitude?.let { altitude ->
                    add(
                        StatTileData(
                            icon = Icons.Default.Terrain,
                            tint = onSurfaceVariant,
                            caption = stringResource(Res.string.schneaggmap_user_altitude_label),
                            value = "${altitude.roundToInt()} m"
                        )
                    )
                }
                location?.batteryLevel?.let { battery ->
                    val (icon, tint) = batteryIconAndTint(battery, onSurfaceVariant, MaterialTheme.colorScheme.error)
                    add(
                        StatTileData(
                            icon = icon,
                            tint = tint,
                            caption = stringResource(Res.string.schneaggmap_user_battery_label),
                            value = "$battery%"
                        )
                    )
                }
                location?.distanceTraveled24h?.let { distance24h ->
                    add(
                        StatTileData(
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
                            tint = onSurfaceVariant,
                            caption = stringResource(Res.string.schneaggmap_user_distance_24h_label),
                            value = formatDistance(distance24h)
                        )
                    )
                }
            }

            tiles.chunked(2).forEach { rowTiles ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowTiles.forEach { tile ->
                        StatTile(tile, modifier = Modifier.weight(1f))
                    }
                    if (rowTiles.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(thickness = 4.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                NormalButton(
                    text = stringResource(Res.string.cancel),
                    onClick = onDismiss,
                    primary = false
                )

                location?.let { userLocation ->
                    val shareUtils = koinInject<ShareUtils>()
                    NormalButton(
                        text = stringResource(Res.string.open_location_in_maps),
                        onClick = { shareUtils.openLocationInMaps(userLocation.lat, userLocation.long, user.displayName) },
                        primary = false
                    )
                }

                if (!isOwnUser) {
                    NormalButton(
                        text = stringResource(Res.string.open_chat),
                        onClick = { onOpenChat(user) },
                        primary = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatTile(data: StatTileData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = data.icon,
            contentDescription = data.caption,
            tint = data.tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = data.caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
        Text(
            text = data.value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/** Maps a battery percentage to a level-appropriate icon, with a warning tint when low. */
private fun batteryIconAndTint(level: Int, defaultTint: Color, lowTint: Color): Pair<ImageVector, Color> {
    if (level <= 15) return Icons.Default.BatteryAlert to lowTint
    val icon = when {
        level <= 30 -> Icons.Default.Battery1Bar
        level <= 45 -> Icons.Default.Battery2Bar
        level <= 60 -> Icons.Default.Battery3Bar
        level <= 75 -> Icons.Default.Battery4Bar
        level <= 90 -> Icons.Default.Battery5Bar
        level < 100 -> Icons.Default.Battery6Bar
        else -> Icons.Default.BatteryFull
    }
    return icon to defaultTint
}

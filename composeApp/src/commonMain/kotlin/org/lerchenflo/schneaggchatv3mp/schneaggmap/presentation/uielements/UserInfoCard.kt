@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.utilities.distanceMeters
import org.lerchenflo.schneaggchatv3mp.utilities.formatDistance
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.open_chat
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_altitude
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_battery
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_distance
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_distance_24h
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_heading
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_last_online
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_speed
import kotlin.math.roundToInt

@Composable
fun UserInfoCard(
    user: User,
    ownLocation: LatLong?,
    onDismiss: () -> Unit,
    onOpenChat: (User) -> Unit,
    modifier: Modifier = Modifier,
) {

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp)) {

            Text(
                text = user.nickName?.takeIf { it.isNotBlank() } ?: user.name,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(
                    Res.string.schneaggmap_user_last_online,
                    user.location?.date?.let { millisToTimeDateOrYesterday(it) } ?: "-"
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            val userDistanceText = remember(ownLocation, user.location) {
                val location = user.location
                if (ownLocation != null && location != null) {
                    formatDistance(distanceMeters(ownLocation, LatLong(lat = location.lat, long = location.long)))
                } else {
                    "--"
                }
            }

            Text(
                text = stringResource(Res.string.schneaggmap_user_distance, userDistanceText),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Advanced telemetry - only present when the user has "Advanced location sharing"
            // enabled towards us, so these rows simply don't appear otherwise.
            user.location?.speed?.let { speed ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.schneaggmap_user_speed, (speed * 3.6).roundToInt().toString()),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            user.location?.heading?.let { heading ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.schneaggmap_user_heading, heading.roundToInt().toString()),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            user.location?.altitude?.let { altitude ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.schneaggmap_user_altitude, altitude.roundToInt().toString()),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            user.location?.batteryLevel?.let { battery ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.schneaggmap_user_battery, battery.toString()),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            user.location?.distanceTraveled24h?.let { distance24h ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.schneaggmap_user_distance_24h, (distance24h / 1000.0).roundToInt().toString()),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
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

                NormalButton(
                    text = stringResource(Res.string.open_chat),
                    onClick = { onOpenChat(user) },
                    primary = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

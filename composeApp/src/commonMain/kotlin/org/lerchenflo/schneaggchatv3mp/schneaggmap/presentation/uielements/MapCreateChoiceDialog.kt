package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.map_create_choice_entry
import schneaggchatv3mp.composeapp.generated.resources.map_create_choice_event
import schneaggchatv3mp.composeapp.generated.resources.map_create_choice_open_in_maps
import schneaggchatv3mp.composeapp.generated.resources.map_create_choice_title

/**
 * Shown after a long-press on the map to ask what should be created at that coordinate: a public
 * [org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry] or an
 * [org.lerchenflo.schneaggchatv3mp.events.domain.Event].
 */
@Composable
fun MapCreateChoiceDialog(
    location: LatLong,
    onDismiss: () -> Unit,
    onCreateMapEntry: (LatLong) -> Unit,
    onCreateEvent: (LatLong) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)) {
                Text(
                    text = stringResource(Res.string.map_create_choice_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                Spacer(modifier = Modifier.size(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    ChoiceRow(
                        icon = Icons.Default.Place,
                        label = stringResource(Res.string.map_create_choice_entry),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            onCreateMapEntry(location)
                        },
                    )
                    ChoiceRow(
                        icon = Icons.Default.Event,
                        label = stringResource(Res.string.map_create_choice_event),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            onCreateEvent(location)
                        },
                    )

                    val shareUtils = koinInject<ShareUtils>()
                    ChoiceRow(
                        icon = Icons.Default.Map,
                        label = stringResource(Res.string.map_create_choice_open_in_maps),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = {

                            shareUtils.openLocationInMaps(
                                lat = location.lat,
                                long = location.long,
                                label = "Marker"
                            )
                        }
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(contentColor.copy(alpha = 0.12f), CircleShape),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f),
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
            )
        }
    }
}

@Preview
@Composable
private fun MapCreateChoiceDialogPreview() {
    SchneaggchatTheme {
        MapCreateChoiceDialog(
            onDismiss = {},
            onCreateMapEntry = {},
            onCreateEvent = {},
            location = LatLong(24.2,0.2)
            )
    }
}

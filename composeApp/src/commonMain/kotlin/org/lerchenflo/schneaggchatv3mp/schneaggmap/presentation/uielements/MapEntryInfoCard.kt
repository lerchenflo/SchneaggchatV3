@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stevdza_san.swipeable.Swipeable
import com.stevdza_san.swipeable.domain.SwipeBehavior
import com.stevdza_san.swipeable.domain.SwipeDirection
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.copyToClipboard
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.locationDataStringResFromKey
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.stringRes
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.toSimpleLocationData
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.latlong
import schneaggchatv3mp.composeapp.generated.resources.location_belongs_to_type
import schneaggchatv3mp.composeapp.generated.resources.save
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_entry_description
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_entry_title

@Composable
fun MapEntryInfoCard(
    entry: MapEntry,
    onDismiss: () -> Unit,
    onSave: (MapEntry) -> Unit,
    modifier: Modifier = Modifier,
) {

    var currentEntry by remember {
        mutableStateOf(entry)
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp)) {

            EntryTitleView(
                entry = currentEntry,
                onChange = {
                    currentEntry = it
                },
                onDismiss = onDismiss
            )


            Spacer(modifier = Modifier.height(12.dp))

            CoordinateView(currentEntry.coordinates)

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(thickness = 4.dp)

            Spacer(modifier = Modifier.height(4.dp))

            LocationAttributeView(
                entry = currentEntry,
                onChange = {
                    currentEntry = it
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            //Close / cancel row
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val changed = entry != currentEntry

                //Move cancel button to the right, if something changed to the left (primary action always on the right)
                if (!changed) {
                    Spacer(modifier = Modifier.weight(1f))
                }

                NormalButton(
                    text = stringResource(Res.string.cancel),
                    onClick = onDismiss,
                    primary = false
                )

                //Save button on the right side, but only if something changed
                if (entry != currentEntry) {
                    NormalButton(
                        text = stringResource(Res.string.save),
                        onClick = {
                            onSave(currentEntry)
                        },
                        primary = false,
                        showOutline = true
                    )
                }

            }

        }
    }


}


@Composable
fun EntryTitleView(entry: MapEntry, onChange: (MapEntry) -> Unit, onDismiss: () -> Unit) {

    Column {

        val combinedEntries = entry.locationData.map {
            stringResource(it.stringRes())
        }
        Text(
            text = stringResource(Res.string.location_belongs_to_type, combinedEntries.joinToString(", ").orEmpty()),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        //TODO: Small row with ratings

        Spacer(modifier = Modifier.height(4.dp))

        //Title
        TextField(
            value = entry.name,
            onValueChange = {
                onChange(
                    entry.copy(name = it)
                )
            },
            label = {
                Text(
                    text = stringResource(Res.string.schneaggmap_entry_title)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = entry.description,
            onValueChange = {
                onChange(
                    entry.copy(description = it)
                )
            },
            label = {
                Text(
                    text = stringResource(Res.string.schneaggmap_entry_description)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CoordinateView(coordinates: LatLong) {

    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {

        val clipboard = LocalClipboard.current.nativeClipboard

        Text(
            text = stringResource(Res.string.latlong, coordinates.lat.toString().take(8), coordinates.long.toString().take(8)),
            modifier = Modifier.clickable {
                copyToClipboard(
                    text = coordinates.lat.toString().take(8) + ", " + coordinates.long.toString().take(8),
                    clipboard = clipboard
                )
            }
        )
    }

    /*

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {


        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.longitude),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = coordinates.long.toString().take(8),
                onValueChange = {},
                enabled = false,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(2f)
            )
        }
    }

     */
}

@Composable
fun LocationAttributeView(entry: MapEntry, onChange: (MapEntry) -> Unit) {

    Column {
        entry.locationData.forEach { locationData ->
            Swipeable(
                behavior = SwipeBehavior.REVEAL,
                direction = SwipeDirection.LEFT,
                rightRevealActions = listOf(
                    /*
                    SwipeAction(
                        customization = ActionCustomization(
                            icon = Icons.Default.Delete,
                            iconColor = Color.White,
                            containerColor = Color.Red
                        ),
                        onAction = { /* Delete item */ }
                    )

                     */
                    //TODO: Delete entry (Update library first to accept drawablevectors)
                )
            ) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(locationData.stringRes()),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            locationData.schema().forEach { definition ->
                                val value = locationData.getValueByKey(definition.key)

                                KeyValueView(
                                    value = value,
                                    definition = definition,
                                    onValueChange = { newValue ->
                                        onChange(entry.copy(
                                            locationData = entry.locationData.map {
                                                if (it === locationData) locationData.withValueForKey(definition.key, newValue)
                                                else it
                                            }
                                        ))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }




    var showLocationAddDropdown by remember { mutableStateOf(false) }

    //Box for alignment of popup
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        NormalButton(
            text = "+",
            primary = false,
            onClick = {
                showLocationAddDropdown = true
            },
            modifier = Modifier.fillMaxWidth()
                .padding(8.dp)
        )

        DropdownMenu(
            expanded = showLocationAddDropdown,
            onDismissRequest = {
                showLocationAddDropdown = false
            },
            modifier = Modifier.align(Alignment.Center)
        ) {
            LocationType.entries.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(locationDataStringResFromKey(type))
                        )
                    },
                    onClick = {
                        onChange(entry.copy(
                            locationData = entry.locationData + type.toSimpleLocationData()
                        ))
                        showLocationAddDropdown = false
                    },
                )
            }
        }
    }

}

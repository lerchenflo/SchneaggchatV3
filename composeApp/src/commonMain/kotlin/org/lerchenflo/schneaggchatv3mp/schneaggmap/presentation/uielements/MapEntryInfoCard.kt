@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stevdza_san.swipeable.Swipeable
import com.stevdza_san.swipeable.domain.ActionCustomization
import com.stevdza_san.swipeable.domain.SwipeAction
import com.stevdza_san.swipeable.domain.SwipeBehavior
import com.stevdza_san.swipeable.domain.SwipeDirection
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.copyToClipboard
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationGroup
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.stringRes
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.toSimpleLocationData
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.delete
import schneaggchatv3mp.composeapp.generated.resources.latlong
import schneaggchatv3mp.composeapp.generated.resources.location_belongs_to_type
import schneaggchatv3mp.composeapp.generated.resources.open_location_in_maps
import schneaggchatv3mp.composeapp.generated.resources.save
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_entry_delete
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_entry_description
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_entry_title

@Composable
fun MapEntryInfoCard(
    entry: MapEntry,
    onDismiss: () -> Unit,
    onSave: (MapEntry) -> Unit,
    onDelete: (String) -> Unit,
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

                val shareUtils = koinInject<ShareUtils>()
                IconButton(
                    onClick = {
                        shareUtils.openLocationInMaps(
                            currentEntry.coordinates.lat,
                            currentEntry.coordinates.long,
                            currentEntry.name
                        )
                    }
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = stringResource(Res.string.open_location_in_maps)
                    )
                }

                val changed = entry != currentEntry

                //Move cancel button to the right, if something changed to the left (primary action always on the right)
                if (!changed) {

                    var showDeleteConfirmationPopup by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {showDeleteConfirmationPopup = true}
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    if (showDeleteConfirmationPopup) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirmationPopup = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    onDelete(entry.id)
                                    showDeleteConfirmationPopup = false

                                }) {
                                    Text(stringResource(Res.string.delete))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    // Handle confirm action
                                    showDeleteConfirmationPopup = false
                                }) {
                                    Text(stringResource(Res.string.cancel))
                                }
                            },
                            text = {
                                Text(
                                    text = stringResource(Res.string.schneaggmap_entry_delete)
                                )
                            }
                        )
                    }


                    Spacer(modifier = Modifier.weight(1f))
                }

                NormalButton(
                    text = stringResource(Res.string.cancel),
                    onClick = onDismiss,
                    primary = false
                )

                if (changed) {
                    Spacer(modifier = Modifier.weight(1f))
                }

                //Save button on the right side, but only if something changed
                if (changed) {
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
fun EntryTitleView(entry: MapEntry, onChange: (MapEntry) -> Unit) {

    Column {

        val combinedEntries = entry.locationData.map {
            stringResource(it.locationtype.stringRes())
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
                leftRevealActions = if (entry.locationData.size > 1) { //Only allow deletion if the entry has more than one locationdata
                    listOf(

                        SwipeAction(
                            customization = ActionCustomization(
                                icon = Icons.Default.Delete,
                                iconColor = MaterialTheme.colorScheme.onError,
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            onAction = {
                                onChange(entry.copy(
                                    locationData = entry.locationData.mapNotNull {
                                        if (it.locationtype == locationData.locationtype) {
                                            null //Return null for this entry (gets deleted)
                                        } else it
                                    }
                                ))
                            }
                        )

                    )
                } else emptyList()
            ) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(locationData.locationtype.stringRes()),
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
    var expandedAddGroups by remember { mutableStateOf(emptySet<LocationGroup>()) }

    //Box for alignment of popup
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        NormalButton(
            text = "+",
            primary = false,
            onClick = {
                expandedAddGroups = emptySet()
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
            LocationGroup.entries.forEach { group ->
                val expanded = group in expandedAddGroups

                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(group.stringRes())
                        )
                    },
                    trailingIcon = {
                        val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f)
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.rotate(arrowRotation)
                        )
                    },
                    onClick = {
                        expandedAddGroups = if (expanded) expandedAddGroups - group else expandedAddGroups + group
                    },
                )

                AnimatedVisibility(visible = expanded) {
                    Column {
                        group.types.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(type.stringRes())
                                    )
                                },
                                onClick = {
                                    onChange(entry.copy(
                                        locationData = entry.locationData + type.toSimpleLocationData()
                                    ))
                                    showLocationAddDropdown = false
                                },
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

}

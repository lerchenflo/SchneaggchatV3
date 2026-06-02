package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.asString
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.latitude
import schneaggchatv3mp.composeapp.generated.resources.longitude
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false
        ),
    ) {

        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp)

        ) {
            Column(modifier = modifier.padding(12.dp)) {


                //Title
                OutlinedTextField(
                    value = currentEntry.name,
                    onValueChange = {
                        currentEntry = currentEntry.copy(
                            name = it
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(Res.string.schneaggmap_entry_title)
                        )
                    },
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = currentEntry.description,
                    onValueChange = {
                        currentEntry = currentEntry.copy(
                            description = it
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(Res.string.schneaggmap_entry_description)
                        )
                    },
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                CoordinateView(currentEntry.coordinates)

                Spacer(modifier = Modifier.height(12.dp))

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
}



@Composable
fun LocationAttributeView(entry: MapEntry, onChange: (MapEntry) -> Unit) {
    entry.locationData.forEach { locationData ->
        Text(locationData.asString())

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


@Composable
fun CoordinateView(coordinates: LatLong) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.latitude),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = coordinates.lat.toString().take(8),
                onValueChange = {},
                enabled = false,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(2f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

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
}


@Preview(
    apiLevel = 36,
    showSystemUi = true,
    showBackground = true
)
@Composable
private fun MapEntryInfoCardPreview() {
    SchneaggchatTheme {
        Box(
            contentAlignment = Alignment.Center
        ) {
            MapEntryInfoCard(
                entry = MapEntry(
                    id = "test",
                    coordinates = LatLong(2.222342342342,2.223433322222332),
                    name = "Test title entry",
                    description = "This is a default test entry for debugging how to show a popup. there is no use in this much text other than showing if the line breaks and the padding works correctly.",
                    locationData = listOf(
                        LocationData.Street(
                            mautFee = null,
                            heightLimit = AttributeValue.DoubleValue(22.222),
                            closedInWinter = AttributeValue.BoolValue(false),
                            wheeliesAllowed = AttributeValue.BoolValue(true)
                        ),

                        LocationData.Radar(
                            speedLimit = AttributeValue.IntValue(25),
                            radarType = LocationData.RadarType.REDLIGHT
                        )
                    ),
                    createdBy = "awd",
                    createdAt = 23232L,
                    updatedBy = "awwad",
                    updatedAt = 223234223L
                ),
                onDismiss = {},
                onSave = {}
            )
        }
    }
}
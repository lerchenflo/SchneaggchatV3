package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
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
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.InputTextField
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.description
import schneaggchatv3mp.composeapp.generated.resources.save

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
                    modifier = Modifier.padding(top = 16.dp),
                )


                InputTextField(
                    text = currentEntry.description,
                    onValueChange = {
                        currentEntry = currentEntry.copy(
                            description = it
                        )
                    },
                    label = stringResource(Res.string.description),
                    modifier = Modifier.padding(top = 16.dp),
                    hint = "",
                )




                //Close / cancel row
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {


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















@Preview(
    apiLevel = 36,
    showSystemUi = true,
    showBackground = true
)
@Composable
private fun MapEntryInfoCardPreview() {
    SchneaggchatTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            MapEntryInfoCard(
                entry = MapEntry(
                    id = "test",
                    coordinates = LatLong(2.22,2.22),
                    name = "Test title entry",
                    description = "This is a default test entry for debugging how to show a popup. there is no use in this much text other than showing if the line breaks and the padding works correctly.",
                    locationData = listOf(
                        LocationData.Street(
                            mautFee = null,
                            heightLimit = AttributeValue.DoubleValue(22.222),
                            closedInWinter = AttributeValue.BoolValue(false),
                            wheeliesAllowed = AttributeValue.BoolValue(true)
                        ),
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
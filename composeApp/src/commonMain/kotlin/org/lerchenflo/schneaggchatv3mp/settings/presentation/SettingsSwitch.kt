package org.lerchenflo.schneaggchatv3mp.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import schneaggchatv3mp.composeapp.generated.resources.Res


@Composable
fun SettingsSwitch(
    modifier : Modifier = Modifier
        .fillMaxWidth()
        .padding(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = 16.dp
        ),
    titletext: String,
    infotext: String,
    switchchecked: Boolean,
    onSwitchChange : (Boolean) -> Unit
){
    //Quer umme a row
    Row(
        modifier = modifier
    ){
        //In da row an titel und drunta a info
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = titletext,
                maxLines = 1,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = infotext,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Switch(
            checked = switchchecked,
            onCheckedChange = {
                onSwitchChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.Gray
            )
        )
    }
}

@Preview(
    showBackground = true
)
@Composable
private fun SettingsSwitchPreview(

){
    SettingsSwitch(
        titletext = "Use markdown",
        infotext = "Blabla es wird din gerät w3ru2p3rnbpfis7dhfciubfpasubiusdbzvouszbevuowszrbvoueszrbfguepzrbgvoaeuzrbg0e8zrghaueozrbgtöta und es crasht oft",
        switchchecked = false,
        onSwitchChange = {}
    )
}
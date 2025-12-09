package org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.UrlChangeDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.change_server_url
import schneaggchatv3mp.composeapp.generated.resources.developer_setting_info
import schneaggchatv3mp.composeapp.generated.resources.developer_settings

@Composable
fun DeveloperSettings(
    modifier: Modifier = Modifier
        .fillMaxWidth(),
    devSettingsViewModel: DevSettingsViewModel,
    sharedSettingsViewmodel: SharedSettingsViewmodel,
    onBackClick: () -> Unit
) {

    var showChangeServerUrlPopup by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        ActivityTitle(
            title = stringResource(Res.string.developer_settings),
            onBackClick = onBackClick
        )

        Spacer(modifier = Modifier.size(10.dp))

        SettingsSwitch(
            titletext = stringResource(Res.string.developer_settings),
            infotext = stringResource(Res.string.developer_setting_info),
            switchchecked = sharedSettingsViewmodel.devSettingsEnabled,
            onSwitchChange = { sharedSettingsViewmodel.updateDevSettings(it) },
            icon = Icons.Default.Code
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        SettingsOption(
            icon = Icons.Default.Link,
            text = stringResource(Res.string.change_server_url),
            subtext = null,
            onClick = {
                showChangeServerUrlPopup = true
            }
        )



        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        SettingsOption(
            icon = Icons.Default.Lightbulb,
            text = "mehr Settings",
            subtext = "es gibt ned mehr dev settings",
            onClick = {
                SnackbarManager.showMessage("joo muasch da was usdenka")
            }
        )

    }

    if(showChangeServerUrlPopup){

        UrlChangeDialog(
            onDismiss = { showChangeServerUrlPopup = false },
            onConfirm = {
                sharedSettingsViewmodel.updateServerUrl(it)
                showChangeServerUrlPopup = false
                        },
            serverUrl = sharedSettingsViewmodel.serverUrl
        )


    }


}
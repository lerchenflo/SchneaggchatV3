package org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsDivider
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.UrlChangeDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.change_server_url
import schneaggchatv3mp.composeapp.generated.resources.dev_group_general
import schneaggchatv3mp.composeapp.generated.resources.dev_group_network
import schneaggchatv3mp.composeapp.generated.resources.dev_group_tools
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

    Column {


        ActivityTitle(
            title = stringResource(Res.string.developer_settings),
            onBackClick = onBackClick
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
        ) {

            SettingsDivider(
                title = stringResource(Res.string.dev_group_general),
                showTopDivider = false
            )

            SettingsSwitch(
                titletext = stringResource(Res.string.developer_settings),
                infotext = stringResource(Res.string.developer_setting_info),
                switchchecked = sharedSettingsViewmodel.devSettingsEnabled,
                onSwitchChange = { sharedSettingsViewmodel.updateDevSettings(it) },
                icon = Icons.Default.Code
            )

            SettingsDivider(
                title = stringResource(Res.string.dev_group_network)
            )

            SettingsOption(
                icon = Icons.Default.Link,
                text = stringResource(Res.string.change_server_url),
                subtext = null,
                onClick = {
                    showChangeServerUrlPopup = true
                }
            )

            SettingsDivider(
                title = stringResource(Res.string.dev_group_tools)
            )

            SettingsOption(
                icon = Icons.Default.Replay,
                text = "Recap",
                subtext = null,
                onClick = {
                    devSettingsViewModel.navigateRecap()
                }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        }

        if (showChangeServerUrlPopup) {
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




}
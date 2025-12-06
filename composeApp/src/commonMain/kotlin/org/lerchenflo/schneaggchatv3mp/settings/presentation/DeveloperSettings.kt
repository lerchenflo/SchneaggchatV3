package org.lerchenflo.schneaggchatv3mp.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.change_server_url
import schneaggchatv3mp.composeapp.generated.resources.developer_setting_info
import schneaggchatv3mp.composeapp.generated.resources.developer_settings

@Composable
fun DeveloperSettings(
    onBackClick: () -> Unit = {}, //TODO: Not used, better navigation inside settings
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .safeContentPadding()
) {
    val settingsViewModel = koinViewModel<SettingsViewModel>()

    var showChangeServerUrlPopup by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
    ) {
        ActivityTitle(
            title = stringResource(Res.string.developer_settings),
            onBackClick = onBackClick
        )

        Spacer(modifier = Modifier.size(10.dp))

        SettingsSwitch(
            titletext = stringResource(Res.string.developer_settings),
            infotext = stringResource(Res.string.developer_setting_info),
            switchchecked = settingsViewModel.devSettingsEnabeled,
            onSwitchChange = { settingsViewModel.updateDevSettings(it) },
            icon = Icons.Default.Code
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        SettingsOption(
            icon = Icons.Default.Link,
            text = stringResource(Res.string.change_server_url),
            subtext = null,
            onClick = {
                println("change server url clicked")
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
            onDismiss = {showChangeServerUrlPopup = false},
            onConfirm = {showChangeServerUrlPopup = false}
        )
    }


}
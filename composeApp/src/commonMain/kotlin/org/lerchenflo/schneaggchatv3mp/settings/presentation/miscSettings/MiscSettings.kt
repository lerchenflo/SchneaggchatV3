package org.lerchenflo.schneaggchatv3mp.settings.presentation.miscSettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.app_broken
import schneaggchatv3mp.composeapp.generated.resources.app_broken_are_you_sure
import schneaggchatv3mp.composeapp.generated.resources.app_broken_desc
import schneaggchatv3mp.composeapp.generated.resources.misc_settings
import schneaggchatv3mp.composeapp.generated.resources.no
import schneaggchatv3mp.composeapp.generated.resources.settings
import schneaggchatv3mp.composeapp.generated.resources.show_logs
import schneaggchatv3mp.composeapp.generated.resources.yes

@Composable
fun MiscSettings(
    modifier: Modifier = Modifier
        .fillMaxWidth(),
    miscSettingsViewModel: MiscSettingsViewModel,
    sharedSettingsViewmodel: SharedSettingsViewmodel,
    onBackClick : () -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        ActivityTitle(
            title = stringResource(Res.string.misc_settings),
            onBackClick = onBackClick
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        var showLogsDialog by remember { mutableStateOf(false) }
        SettingsOption(
            icon = Icons.AutoMirrored.Filled.List,
            text = "Logs",
            subtext = stringResource(Res.string.show_logs),
            onClick = {
                showLogsDialog = true
            }
        )
        if (showLogsDialog) {
            LogsDialog(
                logs = miscSettingsViewModel.logs,
                onDismiss = {
                    showLogsDialog = false
                },
                onClearLogs = {
                    miscSettingsViewModel.onClearLogs()
                }
            )
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        var showAppBrokenDialog by rememberSaveable { mutableStateOf(false) }
        SettingsOption(
            Icons.Default.Delete,
            stringResource(Res.string.app_broken),
            stringResource(Res.string.app_broken_desc),
            onClick = { showAppBrokenDialog = true }
        )
        // app kaputt dialog
        if (showAppBrokenDialog) {
            AlertDialog(
                onDismissRequest = { showAppBrokenDialog = false },
                title = { Text(text = stringResource(Res.string.app_broken)) },
                text = { Text(text = stringResource(Res.string.app_broken_are_you_sure)) },
                confirmButton = {
                    TextButton(onClick = {
                        showAppBrokenDialog = false
                        miscSettingsViewModel.deleteAllAppData()

                    }) {
                        Text(text = stringResource(Res.string.yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAppBrokenDialog = false }) {
                        Text(text = stringResource(Res.string.no))
                    }
                }
            )
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


    }

}
package org.lerchenflo.schneaggchatv3mp.settings.presentation.miscSettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.PLAYSTORE_TESTER_URI
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.ConfirmationDialog
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.app_broken
import schneaggchatv3mp.composeapp.generated.resources.app_broken_are_you_sure
import schneaggchatv3mp.composeapp.generated.resources.app_broken_desc
import schneaggchatv3mp.composeapp.generated.resources.become_beta_tester
import schneaggchatv3mp.composeapp.generated.resources.become_beta_tester_desc
import schneaggchatv3mp.composeapp.generated.resources.bugreport_request
import schneaggchatv3mp.composeapp.generated.resources.bugreport_request_info
import schneaggchatv3mp.composeapp.generated.resources.misc_settings
import schneaggchatv3mp.composeapp.generated.resources.show_logs

@Composable
fun MiscSettings(
    modifier: Modifier = Modifier
        .fillMaxWidth(),
    miscSettingsViewModel: MiscSettingsViewModel,
    sharedSettingsViewmodel: SharedSettingsViewmodel,
    onBackClick : () -> Unit
) {

    val currentAppVersion = koinInject<AppVersion>()

    Column {

        ActivityTitle(
            title = stringResource(Res.string.misc_settings),
            onBackClick = onBackClick
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
        ) {




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

                val clipboard = LocalClipboard.current.nativeClipboard

                LogsDialog(
                    logs = miscSettingsViewModel.logs,
                    onDismiss = {
                        showLogsDialog = false
                    },
                    onClearLogs = {
                        miscSettingsViewModel.onClearLogs()
                    },
                    onCopyAllLogs = { filteredLogs ->
                        val formattedLogs = miscSettingsViewModel.formatAllLogs(filteredLogs)
                        val shareUtils = KoinPlatform.getKoin().get<ShareUtils>()
                        shareUtils.copyToClipboard(formattedLogs, clipboard)
                    }
                )
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            var showBugFeaturePopup by rememberSaveable { mutableStateOf(false) }
            SettingsOption(
                Icons.Default.BugReport,
                text = stringResource(Res.string.bugreport_request),
                subtext = stringResource(Res.string.bugreport_request_info),
                onClick = { showBugFeaturePopup = true }
            )
            if (showBugFeaturePopup) {
                BugReportDialog(
                    onDismiss = { showBugFeaturePopup = false },
                    onSubmit = {
                        miscSettingsViewModel.onSendBugReportEmail(it)
                        showBugFeaturePopup = false
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
                ConfirmationDialog(
                    message = stringResource(Res.string.app_broken_are_you_sure),
                    onConfirm = {
                        miscSettingsViewModel.deleteAllAppData()
                        showAppBrokenDialog = false
                    },
                    onDismiss = {
                        showAppBrokenDialog = false
                    }
                )
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


            //Show beta test uri only on android
            if (currentAppVersion.isAndroid()) {
                val localUriHandler = LocalUriHandler.current

                SettingsOption(
                    icon = Icons.Default.Science,
                    text = stringResource(Res.string.become_beta_tester),
                    subtext = stringResource(Res.string.become_beta_tester_desc),
                    onClick = { localUriHandler.openUri(PLAYSTORE_TESTER_URI) }
                )

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }


        }
    }

}
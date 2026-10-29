package org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import io.github.lerchenflo.taptarget.LocalTapTargetController
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.announcements.AnnouncementAdminAction
import org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.announcements.AnnouncementAdminPopup
import org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.announcements.AnnouncementAdminViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsDivider
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.UrlChangeDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.change_server_url
import schneaggchatv3mp.composeapp.generated.resources.dev_announcements
import schneaggchatv3mp.composeapp.generated.resources.dev_announcements_desc
import schneaggchatv3mp.composeapp.generated.resources.dev_group_general
import schneaggchatv3mp.composeapp.generated.resources.dev_group_network
import schneaggchatv3mp.composeapp.generated.resources.dev_group_tools
import schneaggchatv3mp.composeapp.generated.resources.dev_settings_skip_tour
import schneaggchatv3mp.composeapp.generated.resources.dev_settings_skip_tour_desc
import schneaggchatv3mp.composeapp.generated.resources.developer_setting_info
import schneaggchatv3mp.composeapp.generated.resources.developer_settings
import schneaggchatv3mp.composeapp.generated.resources.dev_settings_demo_mode
import schneaggchatv3mp.composeapp.generated.resources.dev_settings_demo_mode_desc

@Composable
fun DeveloperSettings(
    modifier: Modifier = Modifier
        .fillMaxWidth(),
    devSettingsViewModel: DevSettingsViewModel,
    sharedSettingsViewmodel: SharedSettingsViewmodel,
    onBackClick: () -> Unit,
    announcementAdminViewModel: AnnouncementAdminViewModel = koinViewModel(),
) {

    var showChangeServerUrlPopup by remember { mutableStateOf(false) }
    val tapTargetController = LocalTapTargetController.current
    val demoMode by devSettingsViewModel.demoMode.collectAsStateWithLifecycle()
    val serverCheckStatus by devSettingsViewModel.serverCheckStatus.collectAsStateWithLifecycle()
    val announcementAdminState by announcementAdminViewModel.state.collectAsStateWithLifecycle()

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

            SettingsSwitch(
                titletext = stringResource(Res.string.dev_settings_demo_mode),
                infotext = stringResource(Res.string.dev_settings_demo_mode_desc),
                switchchecked = demoMode,
                onSwitchChange = { devSettingsViewModel.setDemoMode(it) },
                icon = Icons.Default.Slideshow
            )

            SettingsDivider(
                title = stringResource(Res.string.dev_group_network)
            )

            SettingsOption(
                icon = Icons.Default.Link,
                text = stringResource(Res.string.change_server_url),
                subtext = null,
                onClick = {
                    devSettingsViewModel.resetServerCheck()
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

            SettingsOption(
                icon = Icons.Default.DateRange,
                text = "Events",
                subtext = null,
                onClick = {
                    devSettingsViewModel.navigateEvents()
                }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // Shown to every developer - the server rejects everyone who is not an admin
            SettingsOption(
                icon = Icons.Default.Campaign,
                text = stringResource(Res.string.dev_announcements),
                subtext = stringResource(Res.string.dev_announcements_desc),
                onClick = {
                    announcementAdminViewModel.onAction(AnnouncementAdminAction.Open)
                }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            SettingsOption(
                icon = Icons.AutoMirrored.Filled.Forward,
                text = stringResource(Res.string.dev_settings_skip_tour),
                subtext = stringResource(Res.string.dev_settings_skip_tour_desc),
                onClick = {
                    tapTargetController?.skip()
                    devSettingsViewModel.skipOnboardingTour()
                }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        }

        if (announcementAdminState.isOpen) {
            AnnouncementAdminPopup(
                state = announcementAdminState,
                onAction = announcementAdminViewModel::onAction,
            )
        }

        if (showChangeServerUrlPopup) {
            UrlChangeDialog(
                onDismiss = {
                    devSettingsViewModel.resetServerCheck()
                    showChangeServerUrlPopup = false
                },
                onConfirm = {
                    sharedSettingsViewmodel.updateServerUrl(it)
                    devSettingsViewModel.resetServerCheck()
                    showChangeServerUrlPopup = false
                },
                serverUrl = sharedSettingsViewmodel.serverUrl,
                checkStatus = serverCheckStatus,
                onTestServer = { devSettingsViewModel.testServer(it) },
                onUrlEdited = { devSettingsViewModel.onServerUrlEdited() }
            )
        }
    }




}
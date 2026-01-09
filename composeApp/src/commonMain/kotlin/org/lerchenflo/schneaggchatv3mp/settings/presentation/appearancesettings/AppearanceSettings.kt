package org.lerchenflo.schneaggchatv3mp.settings.presentation.appearancesettings

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SsidChart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.DevSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.ThemeSelector
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.UrlChangeDialog
import org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings.UserSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.appearance_settings
import schneaggchatv3mp.composeapp.generated.resources.change_server_url
import schneaggchatv3mp.composeapp.generated.resources.developer_setting_info
import schneaggchatv3mp.composeapp.generated.resources.developer_settings
import schneaggchatv3mp.composeapp.generated.resources.email
import schneaggchatv3mp.composeapp.generated.resources.emailinfo
import schneaggchatv3mp.composeapp.generated.resources.emailinfo_unverified
import schneaggchatv3mp.composeapp.generated.resources.markdownInfo
import schneaggchatv3mp.composeapp.generated.resources.markdown_24px
import schneaggchatv3mp.composeapp.generated.resources.theme
import schneaggchatv3mp.composeapp.generated.resources.theme_sel_desc
import schneaggchatv3mp.composeapp.generated.resources.useMarkdown
import schneaggchatv3mp.composeapp.generated.resources.user_settings

@Composable
fun AppearanceSettings(
    modifier: Modifier = Modifier
        .fillMaxWidth(),
    appearanceSettingsViewModel: AppearanceSettingsViewModel,
    sharedSettingsViewmodel: SharedSettingsViewmodel,
    onBackClick : () -> Unit
) {

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        ActivityTitle(
            title = stringResource(Res.string.appearance_settings),
            onBackClick = onBackClick
        )
        Spacer(modifier = Modifier.size(10.dp))
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        // Markdown Formatting
        SettingsSwitch(
            titletext = stringResource(Res.string.useMarkdown),
            infotext = stringResource(Res.string.markdownInfo),
            switchchecked = appearanceSettingsViewModel.markdownEnabeled,
            onSwitchChange = { appearanceSettingsViewModel.updateMarkdownSwitch(it) },
            icon = vectorResource(Res.drawable.markdown_24px) //Gibts uf da icons no ned aber uf da website scho
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        var themeSelDialog by remember{mutableStateOf(false)}
        // Theme selector
        SettingsOption(
            Icons.Default.Palette,
            stringResource(Res.string.theme),
            stringResource(Res.string.theme_sel_desc),
            onClick = { themeSelDialog = true }
        )
        if(themeSelDialog){
            ThemeSelector(
                onDismiss = { themeSelDialog = false },
                onConfirm = {
                    themeSelDialog = false
                    appearanceSettingsViewModel.saveThemeSetting(it)
                },
                selectedTheme = appearanceSettingsViewModel.selectedTheme,
            )
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


    }




}
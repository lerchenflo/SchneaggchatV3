package org.lerchenflo.schneaggchatv3mp.settings.presentation.appearancesettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
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
import org.jetbrains.compose.resources.vectorResource
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.ThemeSelector
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.LanguageSelector
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.utilities.ThemeSetting
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageSetting
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.appearance_settings
import schneaggchatv3mp.composeapp.generated.resources.language
import schneaggchatv3mp.composeapp.generated.resources.language_sel_desc
import schneaggchatv3mp.composeapp.generated.resources.markdownInfo
import schneaggchatv3mp.composeapp.generated.resources.markdown_24px
import schneaggchatv3mp.composeapp.generated.resources.theme
import schneaggchatv3mp.composeapp.generated.resources.theme_sel_desc
import schneaggchatv3mp.composeapp.generated.resources.useMarkdown

@Composable
fun AppearanceSettings(
    modifier: Modifier = Modifier
        .fillMaxWidth(),
    appearanceSettingsViewModel: AppearanceSettingsViewModel,
    sharedSettingsViewmodel: SharedSettingsViewmodel,
    onBackClick : () -> Unit
) {
    Column {
        ActivityTitle(
            title = stringResource(Res.string.appearance_settings),
            onBackClick = onBackClick
        )
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
        ) {


            // Markdown Formatting
            SettingsSwitch(
                titletext = stringResource(Res.string.useMarkdown),
                infotext = stringResource(Res.string.markdownInfo),
                switchchecked = appearanceSettingsViewModel.markdownEnabeled,
                onSwitchChange = { appearanceSettingsViewModel.updateMarkdownSwitch(it) },
                icon = vectorResource(Res.drawable.markdown_24px) //Gibts uf da icons no ned aber uf da website scho
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            var themeSelDialog by remember { mutableStateOf(false) }
            var previousTheme by remember { mutableStateOf(ThemeSetting.SYSTEM) }
            // Theme selector
            SettingsOption(
                Icons.Default.Palette,
                stringResource(Res.string.theme),
                stringResource(Res.string.theme_sel_desc),
                onClick = {
                    previousTheme = appearanceSettingsViewModel.selectedTheme
                    themeSelDialog = true
                }
            )
            if (themeSelDialog) {
                ThemeSelector(
                    onDismiss = {
                        themeSelDialog = false
                        appearanceSettingsViewModel.saveThemeSetting(previousTheme)
                    },
                    onConfirm = {
                        previousTheme = it
                        themeSelDialog = false
                    },
                    selectedTheme = appearanceSettingsViewModel.selectedTheme,
                    onThemeSelected = { appearanceSettingsViewModel.saveThemeSetting(it) }
                )
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            var languageSelDialog by remember { mutableStateOf(false) }
            var previousLanguage by remember { mutableStateOf(LanguageSetting.SYSTEM) }
            // Language selector
            SettingsOption(
                Icons.Default.Language,
                stringResource(Res.string.language),
                stringResource(Res.string.language_sel_desc),
                onClick = {
                    previousLanguage = appearanceSettingsViewModel.selectedLanguage
                    languageSelDialog = true
                }
            )
            if (languageSelDialog) {
                LanguageSelector(
                    onDismiss = {
                        languageSelDialog = false
                        appearanceSettingsViewModel.saveLanguageSetting(previousLanguage)
                    },
                    onConfirm = {
                        previousLanguage = it
                        languageSelDialog = false
                    },
                    selectedLanguage = appearanceSettingsViewModel.selectedLanguage,
                    onLanguageSelected = { appearanceSettingsViewModel.saveLanguageSetting(it) }
                )
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        }
    }




}
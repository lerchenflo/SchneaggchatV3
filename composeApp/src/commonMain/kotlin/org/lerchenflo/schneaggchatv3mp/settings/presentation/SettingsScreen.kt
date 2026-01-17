package org.lerchenflo.schneaggchatv3mp.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Boy
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Hexagon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureBigDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.app_broken
import schneaggchatv3mp.composeapp.generated.resources.app_broken_are_you_sure
import schneaggchatv3mp.composeapp.generated.resources.app_broken_desc
import schneaggchatv3mp.composeapp.generated.resources.appearance_settings
import schneaggchatv3mp.composeapp.generated.resources.appearance_settings_info
import schneaggchatv3mp.composeapp.generated.resources.developer_setting_info
import schneaggchatv3mp.composeapp.generated.resources.developer_settings
import schneaggchatv3mp.composeapp.generated.resources.misc_setting_info
import schneaggchatv3mp.composeapp.generated.resources.misc_settings
import schneaggchatv3mp.composeapp.generated.resources.no
import schneaggchatv3mp.composeapp.generated.resources.settings
import schneaggchatv3mp.composeapp.generated.resources.user_settings
import schneaggchatv3mp.composeapp.generated.resources.user_settingsinfo
import schneaggchatv3mp.composeapp.generated.resources.user_since
import schneaggchatv3mp.composeapp.generated.resources.version
import schneaggchatv3mp.composeapp.generated.resources.yes

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
        .fillMaxWidth(),
    settingsViewmodel: SettingsViewModel,
    sharedSettingsViewmodel: SharedSettingsViewmodel,
    onBackClick: () -> Unit,
    navigateUserSettings: () -> Unit,
    navigateDevSettings: () -> Unit,
    navigateMiscSettings: () -> Unit,
    navigateAppearanceSettings: () -> Unit
){
    val appRepository = koinInject<AppRepository>()


    val ownuser = sharedSettingsViewmodel.ownUser


    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ){
        ActivityTitle(
            title = stringResource(Res.string.settings),
            onBackClick = onBackClick
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        // Section Userinfo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ){

            var profilepicpopupShown by remember { mutableStateOf(false) }
            ProfilePictureView(
                filepath = ownuser?.profilePictureUrl ?: "",
                modifier = Modifier
                    .size(120.dp)
                    .clickable {
                        profilepicpopupShown = true
                    }
                    .padding(8.dp)
            )
            if (profilepicpopupShown) {
                ProfilePictureBigDialog(
                    filepath = ownuser?.profilePictureUrl ?: "",
                    onDismiss = {
                        profilepicpopupShown = false
                    }
                )
            }




            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        end = 4.dp
                    ),
                horizontalAlignment = Alignment.Start
            ){

                Text(
                    text = ownuser?.name ?: "Username",
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 10.sp,
                        maxFontSize = 30.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (ownuser != null && ownuser.createdAt != null) {
                    Text(
                        text = stringResource(Res.string.user_since,
                            millisToTimeDateOrYesterday(ownuser.createdAt)
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        SettingsOption(
            icon = Icons.Default.Boy,
            text = stringResource(Res.string.user_settings),
            subtext = stringResource(Res.string.user_settingsinfo),
            onClick = navigateUserSettings
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        SettingsOption(
            Icons.Default.Palette,
            stringResource(Res.string.appearance_settings),
            stringResource(Res.string.appearance_settings_info),
            onClick = navigateAppearanceSettings
        )



        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        if(sharedSettingsViewmodel.devSettingsEnabled){
            SettingsOption(
                Icons.Default.Code,
                stringResource(Res.string.developer_settings),
                stringResource(Res.string.developer_setting_info),
                onClick = navigateDevSettings
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        }


        SettingsOption(
            icon = Icons.Outlined.Hexagon,
            text = stringResource(Res.string.misc_settings),
            subtext = stringResource(Res.string.misc_setting_info),
            onClick = navigateMiscSettings
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        var openDevSettingsCounter by mutableIntStateOf(0)

        Text(
            text = stringResource(Res.string.version, appRepository.appVersion.getVersionName()) + " Buildnr: " + appRepository.appVersion.getVersionCode(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .padding(
                    top = 16.dp
                )
                .fillMaxWidth()
                .pointerInput(Unit) {// clickable for developer settings
                    // manual pointer handling for non consuming tap
                    awaitEachGesture {
                        // Do NOT require unconsumed → we don't consume either
                        val down = awaitFirstDown(requireUnconsumed = false)

                        val requiredClicks = 8


                        val up = waitForUpOrCancellation()
                        if (up != null) {

                            // Tap detected, but NOT consumed
                            openDevSettingsCounter++

                            when {
                                // Still counting down
                                openDevSettingsCounter < requiredClicks -> {
                                    val stepsRemaining = requiredClicks - openDevSettingsCounter

                                    //Show popup after x clicks
                                    if (stepsRemaining < requiredClicks - 4) {
                                        SnackbarManager.showMessage(
                                            if (stepsRemaining == 1) {
                                                "You are now 1 step away from being a developer"
                                            } else {
                                                "You are now $stepsRemaining steps away from being a developer"
                                            }
                                        )
                                    }
                                }

                                // Exactly at the threshold - activate dev mode
                                openDevSettingsCounter == requiredClicks -> {
                                    SnackbarManager.showMessage("You are now a developer!")
                                    sharedSettingsViewmodel.updateDevSettings(true) // save in preferences
                                    openDevSettingsCounter = 0
                                    navigateDevSettings()
                                }

                                // Already a developer (shouldn't normally happen, but just in case)
                                else -> {
                                    SnackbarManager.showMessage("You are already a developer")
                                    openDevSettingsCounter = 0
                                }
                            }
                        }
                    }
                }
        )
    }
}
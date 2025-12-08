package org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mail
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
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.DevSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.UrlChangeDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.change_server_url
import schneaggchatv3mp.composeapp.generated.resources.developer_setting_info
import schneaggchatv3mp.composeapp.generated.resources.developer_settings
import schneaggchatv3mp.composeapp.generated.resources.email
import schneaggchatv3mp.composeapp.generated.resources.emailinfo
import schneaggchatv3mp.composeapp.generated.resources.emailinfo_unverified
import schneaggchatv3mp.composeapp.generated.resources.user_settings

@Composable
fun UserSettings(
    modifier: Modifier = Modifier
        .fillMaxWidth(),
    userSettingsViewModel: UserSettingsViewModel,
    sharedSettingsViewmodel: SharedSettingsViewmodel,
    onBackClick : () -> Unit
) {

    val ownuser = sharedSettingsViewmodel.ownUser

    var showChangeUsernamePopup by remember { mutableStateOf(false) }


    //TODO: Wittabuggla

    Column(
        modifier = modifier
    ) {
        ActivityTitle(
            title = stringResource(Res.string.user_settings),
            onBackClick = onBackClick
        )
        Spacer(modifier = Modifier.size(10.dp))
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        //Username?
        SettingsOption(
            icon = Icons.Default.Accessibility,
            text = "mehr Settings",
            subtext = "es gibt ned mehr dev settings",
            onClick = {
                SnackbarManager.showMessage("joo muasch da was usdenka")
            }
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        //passwort
        SettingsOption(
            icon = Icons.Default.Key,
            text = "mehr Settings",
            subtext = "es gibt ned mehr dev settings",
            onClick = {
                SnackbarManager.showMessage("joo muasch da was usdenka")
            }
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        SettingsOption(
            icon = Icons.Default.Mail,
            text = stringResource(Res.string.email),
            subtext = if (ownuser?.isEmailVerified() == true) stringResource(Res.string.emailinfo) else stringResource(Res.string.emailinfo_unverified),
            onClick = {
                SnackbarManager.showMessage("joo muasch da was usdenka")
            },
            rightSideIcon = {
                if (ownuser != null){
                    if (ownuser.isEmailVerified()) {
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = "Email is verified",
                            modifier = Modifier.size(30.dp),
                            tint = Color(red = 0, green = 255, blue = 0)
                        )
                    }else {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Email not verified",
                            modifier = Modifier.size(30.dp),
                            tint = Color(red = 255, green = 165, blue = 0) //orange

                        )
                    }
                }
            },
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


    }

    if(showChangeUsernamePopup){

    }


}
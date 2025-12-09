package org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.ismoy.imagepickerkmp.domain.config.CameraCaptureConfig
import io.github.ismoy.imagepickerkmp.domain.config.CropConfig
import io.github.ismoy.imagepickerkmp.domain.config.GalleryConfig
import io.github.ismoy.imagepickerkmp.domain.models.CapturePhotoPreference
import io.github.ismoy.imagepickerkmp.domain.models.CompressionLevel
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.SignupAction
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.DevSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.UrlChangeDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.are_you_sure_you_want_to_logout
import schneaggchatv3mp.composeapp.generated.resources.change_server_url
import schneaggchatv3mp.composeapp.generated.resources.developer_setting_info
import schneaggchatv3mp.composeapp.generated.resources.developer_settings
import schneaggchatv3mp.composeapp.generated.resources.email
import schneaggchatv3mp.composeapp.generated.resources.emailinfo
import schneaggchatv3mp.composeapp.generated.resources.emailinfo_unverified
import schneaggchatv3mp.composeapp.generated.resources.logout
import schneaggchatv3mp.composeapp.generated.resources.no
import schneaggchatv3mp.composeapp.generated.resources.user_settings
import schneaggchatv3mp.composeapp.generated.resources.yes

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

    var showImagePickerDialog by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        if (showImagePickerDialog ) {

            //TODO: Fix all strings
            GalleryPickerLauncher(
                onPhotosSelected = {
                    userSettingsViewModel.changeProfilePicture(it.first())
                    showImagePickerDialog = false
                },
                onError = {
                    showImagePickerDialog = false
                },
                onDismiss = {
                    showImagePickerDialog = false
                },
                selectionLimit = 1,
                enableCrop = true,
                cameraCaptureConfig = CameraCaptureConfig(
                    compressionLevel = CompressionLevel.HIGH,
                    preference = CapturePhotoPreference.FAST, //No flash
                    cropConfig = CropConfig(
                        enabled = true,
                        aspectRatioLocked = true,
                        circularCrop = true,
                        squareCrop = false,
                        freeformCrop = false
                    ),
                    galleryConfig = GalleryConfig(
                        allowMultiple = false,
                        selectionLimit = 1,
                    )
                )
            )

        }
    }
    //TODO: Wittabuggla

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        ActivityTitle(
            title = stringResource(Res.string.user_settings),
            onBackClick = onBackClick
        )
        Spacer(modifier = Modifier.size(10.dp))
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        ProfilePictureView(
            filepath = ownuser?.profilePictureUrl ?: "",
            modifier = Modifier
                .padding(horizontal = 60.dp)
                .widthIn(max = 250.dp) //Max width for desktop
                .fillMaxWidth()
                .clickable {
                    showImagePickerDialog = true
                }
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )


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
            subtext = if (ownuser?.isEmailVerified() == true) stringResource(Res.string.emailinfo) else stringResource(Res.string.emailinfo_unverified) + "\n" + ownuser?.email,
            onClick = {
                if (ownuser?.isEmailVerified() == true){
                    //TODO: Email Change popup
                }else {
                    userSettingsViewModel.sendEmailVerify()
                }
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


        var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
        SettingsOption(
            icon = Icons.AutoMirrored.Default.ExitToApp,
            text = stringResource(Res.string.logout),
            onClick = {
                showLogoutDialog = true
            }
        )

        //Logoutdialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text(text = stringResource(Res.string.logout)) },
                text = { Text(text = stringResource(Res.string.are_you_sure_you_want_to_logout)) },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        userSettingsViewModel.logout()
                    }) {
                        Text(text = stringResource(Res.string.yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(text = stringResource(Res.string.no))
                    }
                }
            )
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


    }

    if(showChangeUsernamePopup){

    }


}
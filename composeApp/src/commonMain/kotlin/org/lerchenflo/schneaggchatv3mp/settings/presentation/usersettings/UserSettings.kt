package org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.ismoy.imagepickerkmp.domain.config.CameraCaptureConfig
import io.github.ismoy.imagepickerkmp.domain.config.CropConfig
import io.github.ismoy.imagepickerkmp.domain.config.GalleryConfig
import io.github.ismoy.imagepickerkmp.domain.models.CapturePhotoPreference
import io.github.ismoy.imagepickerkmp.domain.models.CompressionLevel
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.lerchenflo.schneaggchatv3mp.URL_DEL_ACC
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.QuotedText
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.ChangeDialog
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.ConfirmationDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.DeleteButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.isEmailValid
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.are_you_sure_you_want_to_logout
import schneaggchatv3mp.composeapp.generated.resources.change
import schneaggchatv3mp.composeapp.generated.resources.change_email
import schneaggchatv3mp.composeapp.generated.resources.change_status
import schneaggchatv3mp.composeapp.generated.resources.change_username
import schneaggchatv3mp.composeapp.generated.resources.change_username_description
import schneaggchatv3mp.composeapp.generated.resources.change_username_placeholder
import schneaggchatv3mp.composeapp.generated.resources.currentstatus
import schneaggchatv3mp.composeapp.generated.resources.delete_account
import schneaggchatv3mp.composeapp.generated.resources.edit_profile_picture
import schneaggchatv3mp.composeapp.generated.resources.email
import schneaggchatv3mp.composeapp.generated.resources.emailinfo
import schneaggchatv3mp.composeapp.generated.resources.emailinfo_unverified
import schneaggchatv3mp.composeapp.generated.resources.error_cannot_be_the_same_username
import schneaggchatv3mp.composeapp.generated.resources.invalid_email
import schneaggchatv3mp.composeapp.generated.resources.logout
import schneaggchatv3mp.composeapp.generated.resources.status_infotext
import schneaggchatv3mp.composeapp.generated.resources.status_nosemicolon
import schneaggchatv3mp.composeapp.generated.resources.user_attributes_24px
import schneaggchatv3mp.composeapp.generated.resources.user_settings
import schneaggchatv3mp.composeapp.generated.resources.your_friends_wrote_this

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

    var showChangeStatusPopup by remember { mutableStateOf(false) }
    var showChangeEmailPopup by remember { mutableStateOf(false) }


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


    if (!showImagePickerDialog){
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


            Box(
                modifier = Modifier
                    .padding(horizontal = 60.dp)
                    .widthIn(max = 250.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally) // Ensure the Box itself is centered
                    .clickable { showImagePickerDialog = true }

            ) {
                // The Main Profile Picture
                ProfilePictureView(
                    filepath = ownuser?.profilePictureUrl ?: "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                // The Edit Icon Overlay
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.BottomEnd) // Positions icon at bottom right
                        .offset(x = (-8).dp, y = (-8).dp) // Adjusts spacing from the edges
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(Res.string.edit_profile_picture),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                    )
                }
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


            if (ownuser != null && ownuser.description != null && ownuser.description.isNotEmpty()) {
                //Userdescription
                QuotedText(
                    text = ownuser.description,
                    author = stringResource(Res.string.your_friends_wrote_this)
                )

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }

            //Username
            SettingsOption(
                icon = Icons.Default.EditNote,
                text = stringResource(Res.string.change_username),
                subtext = stringResource(Res.string.change_username_description),
                onClick = {
                    showChangeUsernamePopup = true
                }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


            //Status
            SettingsOption(
                icon = vectorResource(Res.drawable.user_attributes_24px),
                text = stringResource(Res.string.status_nosemicolon),
                subtext = if (ownuser?.status == null || ownuser.status.isEmpty()) {
                    stringResource(Res.string.status_infotext)
                } else {
                    stringResource(Res.string.currentstatus, ownuser.status)

                },
                onClick = {
                    showChangeStatusPopup = true
                }
            )


            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            /*
            //TODO: Change passwort
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

             */

            SettingsOption(
                icon = Icons.Default.Mail,
                text = stringResource(Res.string.email),
                subtext = (if (ownuser?.isEmailVerified() == true) stringResource(Res.string.emailinfo) else stringResource(Res.string.emailinfo_unverified)) + "\n" + ownuser?.email,
                onClick = {
                    showChangeEmailPopup = true
                    /*
                    if (ownuser?.isEmailVerified() == true){
                    }else {
                        userSettingsViewModel.sendEmailVerify()
                    }

                     */
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
                                tint = Color(red = 255, green = 165, blue = 0), //orange
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable{
                                        userSettingsViewModel.sendEmailVerify()
                                    }
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
                ConfirmationDialog(
                    message = stringResource(Res.string.are_you_sure_you_want_to_logout),
                    onConfirm = {
                        userSettingsViewModel.logout()
                    },
                    onDismiss = {
                        showLogoutDialog = false
                    }
                )
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            val uriHandler = LocalUriHandler.current

            DeleteButton(
                text = stringResource(Res.string.delete_account),
                onClick = {
                    uriHandler.openUri(URL_DEL_ACC)
                },
                modifier = Modifier
                    .padding(top = 8.dp,
                        bottom = 4.dp,
                        start = 4.dp,
                        end = 4.dp)

                    .fillMaxWidth(),
            )

        }
    }


    if(showChangeUsernamePopup){
        val cannot_be_same_username_text = stringResource(Res.string.error_cannot_be_the_same_username)

        ChangeDialog(
            title = stringResource(Res.string.change_username),
            initialValue = sharedSettingsViewmodel.ownUser?.name ?: "",
            placeholder = stringResource(Res.string.change_username_placeholder),
            onDismiss = { showChangeUsernamePopup = false },
            onConfirm = {
                userSettingsViewModel.updateUsernameOnServer(it)
            },
            confirmButtonText = stringResource(Res.string.change),
            validator = { newValue ->
                if (newValue == (sharedSettingsViewmodel.ownUser?.name ?: "")) {
                    cannot_be_same_username_text
                } else null
            }
        )
    }

    if (showChangeStatusPopup) {
        ChangeDialog(
            title = stringResource(Res.string.change_status),
            initialValue = ownuser?.status ?: "",
            onDismiss = { showChangeStatusPopup = false },
            onConfirm = {
                userSettingsViewModel.changeStatus(it)
            },
            confirmButtonText = stringResource(Res.string.change)
        )
    }

    if (showChangeEmailPopup) {
        val invalidEmailString = stringResource(Res.string.invalid_email)
        ChangeDialog(
            title = stringResource(Res.string.change_email),
            initialValue = ownuser?.email ?: "",
            onDismiss = { showChangeEmailPopup = false },
            onConfirm = {
                userSettingsViewModel.changeEmail(it)
            },
            keyboardType = KeyboardType.Email,
            confirmButtonText = stringResource(Res.string.change),
            validator = { newValue ->
                if (!isEmailValid(newValue)) {
                    invalidEmailString
                } else null
            }
        )
    }


}
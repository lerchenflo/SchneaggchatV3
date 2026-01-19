package org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ismoy.imagepickerkmp.domain.config.CameraCaptureConfig
import io.github.ismoy.imagepickerkmp.domain.config.CropConfig
import io.github.ismoy.imagepickerkmp.domain.config.GalleryConfig
import io.github.ismoy.imagepickerkmp.domain.models.CapturePhotoPreference
import io.github.ismoy.imagepickerkmp.domain.models.CompressionLevel
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.lerchenflo.schneaggchatv3mp.URL_DEL_ACC
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.QuotedText
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.DeleteButton
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.are_you_sure_you_want_to_logout
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.change
import schneaggchatv3mp.composeapp.generated.resources.change_status
import schneaggchatv3mp.composeapp.generated.resources.change_username
import schneaggchatv3mp.composeapp.generated.resources.change_username_description
import schneaggchatv3mp.composeapp.generated.resources.change_username_placeholder
import schneaggchatv3mp.composeapp.generated.resources.delete_account
import schneaggchatv3mp.composeapp.generated.resources.email
import schneaggchatv3mp.composeapp.generated.resources.emailinfo
import schneaggchatv3mp.composeapp.generated.resources.emailinfo_unverified
import schneaggchatv3mp.composeapp.generated.resources.error_cannot_be_the_same_username
import schneaggchatv3mp.composeapp.generated.resources.logout
import schneaggchatv3mp.composeapp.generated.resources.no
import schneaggchatv3mp.composeapp.generated.resources.no_status
import schneaggchatv3mp.composeapp.generated.resources.status
import schneaggchatv3mp.composeapp.generated.resources.status_infotext
import schneaggchatv3mp.composeapp.generated.resources.status_nosemicolon
import schneaggchatv3mp.composeapp.generated.resources.user_attributes_24px
import schneaggchatv3mp.composeapp.generated.resources.user_settings
import schneaggchatv3mp.composeapp.generated.resources.yes
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
                    ownuser.status
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
        ChangeUsernameAlert(
            onDismiss = { showChangeUsernamePopup = false },
            onSave = {
                userSettingsViewModel.updateUsernameOnServer(it)
            },
            oldUsername = sharedSettingsViewmodel.ownUser?.name ?: "",
        )
    }

    if (showChangeStatusPopup) {
        ChangeStatusAlert(
            onDismiss = {showChangeStatusPopup = false},
            onSave = {
                userSettingsViewModel.changeStatus(it)
            },
            oldStatus = ownuser?.status ?: ""
        )
    }


}

@Composable
fun ChangeUsernameAlert(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    oldUsername: String

){
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current // Also helpful to hide keyboard

    var newUsername by remember {
        mutableStateOf(oldUsername)
    }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (newUsername == oldUsername) {
                        scope.launch {
                            SnackbarManager.showMessage(getString(Res.string.error_cannot_be_the_same_username))
                        }
                    } else {
                        onSave(newUsername)
                        onDismiss()
                    }

                },
            ) {
                Text(
                    text = stringResource(Res.string.change)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = stringResource(Res.string.cancel)
                )
            }
        },
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        })
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.change_username),
                    )
                    OutlinedTextField(
                        value = newUsername,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 14.sp // You can adjust this value as needed
                        ),
                        singleLine = true,
                        onValueChange = { newValue ->
                            newUsername = newValue
                        },
                        modifier = Modifier
                            .onPreviewKeyEvent { event ->
                                // Check if the key is 'Escape' and it's a 'KeyDown' event
                                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                                    onDismiss()
                                }
                                false // Pass all other events (letters, backspace, etc.) to the TextField
                            },
                        placeholder = { Text(stringResource(Res.string.change_username_placeholder)) }
                    )

                }
            }

        },
    )
}


@Composable
fun ChangeStatusAlert(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    oldStatus: String

){
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current // Also helpful to hide keyboard

    var newStatus by remember {
        mutableStateOf(oldStatus)
    }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (newStatus == oldStatus) {
                        onDismiss()
                    } else {
                        onSave(newStatus)
                        onDismiss()
                    }

                },
            ) {
                Text(
                    text = stringResource(Res.string.change)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = stringResource(Res.string.cancel)
                )
            }
        },
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        })
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.change_status),
                    )
                    OutlinedTextField(
                        value = newStatus,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 14.sp // You can adjust this value as needed
                        ),
                        singleLine = true,
                        onValueChange = { newValue ->
                            newStatus = newValue
                        },
                        modifier = Modifier
                            .onPreviewKeyEvent { event ->
                                // Check if the key is 'Escape' and it's a 'KeyDown' event
                                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                                    onDismiss()
                                }
                                false // Pass all other events (letters, backspace, etc.) to the TextField
                            },
                    )

                }
            }

        },
    )
}
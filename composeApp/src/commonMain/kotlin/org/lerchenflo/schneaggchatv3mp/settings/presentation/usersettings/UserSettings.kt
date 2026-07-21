@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import io.github.ismoy.imagepickerkmp.domain.config.CropConfig
import io.github.ismoy.imagepickerkmp.domain.config.GalleryConfig
import io.github.ismoy.imagepickerkmp.domain.models.MimeType
import io.github.ismoy.imagepickerkmp.features.imagepicker.config.ImagePickerKMPConfig
import io.github.ismoy.imagepickerkmp.features.imagepicker.model.ImagePickerResult
import io.github.ismoy.imagepickerkmp.features.imagepicker.ui.rememberImagePickerKMP
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.ConfirmationDialog
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.getDeleteAccountUrl
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.BirthdatePickerPopup
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.ChangeDialog
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.QuotedText
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.DeleteButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.emailProviderWarning
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.isEmailValid
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.are_you_sure_you_want_to_logout
import schneaggchatv3mp.composeapp.generated.resources.camera
import schneaggchatv3mp.composeapp.generated.resources.change
import schneaggchatv3mp.composeapp.generated.resources.change_email
import schneaggchatv3mp.composeapp.generated.resources.change_gebi_date
import schneaggchatv3mp.composeapp.generated.resources.change_status
import schneaggchatv3mp.composeapp.generated.resources.change_username
import schneaggchatv3mp.composeapp.generated.resources.change_username_description
import schneaggchatv3mp.composeapp.generated.resources.change_username_placeholder
import schneaggchatv3mp.composeapp.generated.resources.choose_image_source
import schneaggchatv3mp.composeapp.generated.resources.currentstatus
import schneaggchatv3mp.composeapp.generated.resources.delete_account
import schneaggchatv3mp.composeapp.generated.resources.edit_profile_picture
import schneaggchatv3mp.composeapp.generated.resources.email
import schneaggchatv3mp.composeapp.generated.resources.email_provider_warning
import schneaggchatv3mp.composeapp.generated.resources.emailinfo
import schneaggchatv3mp.composeapp.generated.resources.emailinfo_unverified
import schneaggchatv3mp.composeapp.generated.resources.error_cannot_be_the_same_username
import schneaggchatv3mp.composeapp.generated.resources.gallery
import schneaggchatv3mp.composeapp.generated.resources.invalid_email
import schneaggchatv3mp.composeapp.generated.resources.logout
import schneaggchatv3mp.composeapp.generated.resources.status_infotext
import schneaggchatv3mp.composeapp.generated.resources.status_nosemicolon
import schneaggchatv3mp.composeapp.generated.resources.user_attributes_24px
import schneaggchatv3mp.composeapp.generated.resources.user_settings
import schneaggchatv3mp.composeapp.generated.resources.wake_settings
import schneaggchatv3mp.composeapp.generated.resources.wake_settings_info
import schneaggchatv3mp.composeapp.generated.resources.your_friends_wrote_this

@Composable
fun UserSettings(
    modifier: Modifier = Modifier
        .fillMaxWidth(),
    userSettingsViewModel: UserSettingsViewModel,
    sharedSettingsViewmodel: SharedSettingsViewmodel,
    onBackClick : () -> Unit
) {
    val preferencemanager = koinInject<Preferencemanager>()

    val ownuser = sharedSettingsViewmodel.ownUser

    val dev = SessionCache.requireLoggedIn()?.developer ?: return

    var showChangeUsernamePopup by remember { mutableStateOf(false) }

    var showChangeStatusPopup by remember { mutableStateOf(false) }
    var showChangeBirthDatePopup by remember { mutableStateOf(false) }
    var showChangeEmailPopup by remember { mutableStateOf(false) }



    var showImagePickerDialog by remember { mutableStateOf(false) }

    val picker = rememberImagePickerKMP(
        config = ImagePickerKMPConfig(
            cropConfig = CropConfig(
                enabled = true,
                aspectRatioLocked = true,
                circularCrop = true,
                squareCrop = false,
                freeformCrop = false
            ),
            galleryConfig = GalleryConfig(
                allowMultiple = false,
                mimeTypes = listOf(MimeType.IMAGE_ALL),
                includeExif = false
            )
        )
    )
    val result = picker.result


    if (showImagePickerDialog) {

        // Handle side effects safely
        LaunchedEffect(result) {
            when (result) {
                is ImagePickerResult.Success -> {
                    userSettingsViewModel.changeProfilePicture(result.photos.first())

                    picker.reset()
                    showImagePickerDialog = false
                }

                is ImagePickerResult.Dismissed -> {
                    picker.reset()
                    showImagePickerDialog = false
                }

                else -> Unit
            }
        }

        BasicAlertDialog(
            onDismissRequest = {
                showImagePickerDialog = false
            }
        ) {

            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {

                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .width(IntrinsicSize.Min),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    when (result) {

                        is ImagePickerResult.Loading -> {
                            CircularProgressIndicator()
                        }

                        is ImagePickerResult.Error -> {
                            Text(
                                text = "Error: ${result.exception.message}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        is ImagePickerResult.Idle -> {

                            Text(
                                stringResource(Res.string.choose_image_source)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                Button(
                                    onClick = {
                                        picker.launchCamera()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(Res.string.camera))
                                }

                                Button(
                                    onClick = {
                                        picker.launchGallery()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(Res.string.gallery))
                                }
                            }
                        }

                        is ImagePickerResult.Success,
                        is ImagePickerResult.Dismissed -> {
                            // handled in LaunchedEffect
                        }
                    }
                }
            }
        }
    }



    Column {

        ActivityTitle(
            title = stringResource(Res.string.user_settings),
            onBackClick = onBackClick
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
        ) {

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


            if (ownuser != null && !ownuser.description.isNullOrEmpty()) {
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

            // Gebidate
            SettingsOption(
                icon = Icons.Default.Cake,
                text = stringResource(Res.string.change_gebi_date),
                subtext = ownuser?.birthDate?.let { dateString ->
                    try {
                        val date = LocalDate.parse(dateString)
                        "${date.day.toString().padStart(2, '0')}.${date.month.number.toString().padStart(2, '0')}.${date.year}"
                    } catch (e: Exception) {
                        dateString
                    }
                },
                onClick = {
                    showChangeBirthDatePopup = true
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

            if (dev) {

                SettingsOption(
                    icon = Icons.Default.Mail,
                    text = stringResource(Res.string.email),
                    subtext = (if (ownuser?.isEmailVerified() == true) stringResource(Res.string.emailinfo) else stringResource(
                        Res.string.emailinfo_unverified
                    )) + "\n" + ownuser?.email,
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
                        if (ownuser != null) {
                            if (ownuser.isEmailVerified()) {
                                Icon(
                                    imageVector = Icons.Outlined.Verified,
                                    contentDescription = "Email is verified",
                                    modifier = Modifier.size(30.dp),
                                    tint = Color(red = 0, green = 255, blue = 0)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = "Email not verified",
                                    tint = Color(red = 255, green = 165, blue = 0), //orange
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clickable {
                                            userSettingsViewModel.sendEmailVerify()
                                        }
                                )
                            }
                        }
                    },
                )

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }

            //Waking is Android only - the alarm service has no counterpart on iOS/Desktop.
            if (koinInject<AppVersion>().isAndroid()) {
                var showWakeSettingsDialog by rememberSaveable { mutableStateOf(false) }

                SettingsOption(
                    icon = Icons.Default.Alarm,
                    text = stringResource(Res.string.wake_settings),
                    subtext = stringResource(Res.string.wake_settings_info),
                    onClick = { showWakeSettingsDialog = true }
                )

                if (showWakeSettingsDialog) {
                    WakeSettingsDialog(
                        wakeEnabledGlobal = userSettingsViewModel.wakeEnabledGlobal,
                        friends = userSettingsViewModel.friends,
                        onSave = { global, friendDrafts ->
                            userSettingsViewModel.saveWakeSettings(global, friendDrafts)
                        },
                        onDismiss = { showWakeSettingsDialog = false }
                    )
                }

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }

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
                    val serverUrl = runBlocking { preferencemanager.getServerUrl() }
                    uriHandler.openUri(getDeleteAccountUrl(serverUrl))
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
        val providerWarningString = stringResource(Res.string.email_provider_warning)
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
                if (!isEmailValid(newValue)) invalidEmailString else null
            },
            warningValidator = { newValue ->
                emailProviderWarning(newValue, providerWarningString)
            }
        )
    }

    if(showChangeBirthDatePopup){
        BirthdatePickerPopup(
            onDateSelected = {
                userSettingsViewModel.changeBirthDate(
                    it.toString()
                )
                showChangeBirthDatePopup = false
            },
            onDismiss = { showChangeBirthDatePopup = false },
            defaultDate = ownuser?.birthDate?.let { dateString ->
                try {
                    LocalDate.parse(dateString)
                } catch (e: Exception) {
                    null
                }
            }
        )
    }


}
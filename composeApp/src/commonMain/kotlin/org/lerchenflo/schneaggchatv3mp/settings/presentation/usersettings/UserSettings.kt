@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings

import schneaggchatv3mp.composeapp.generated.resources.user_settings_friends_hint
import schneaggchatv3mp.composeapp.generated.resources.user_settings_friends_empty
import schneaggchatv3mp.composeapp.generated.resources.about_friends_say
import schneaggchatv3mp.composeapp.generated.resources.user_settings_status_set
import schneaggchatv3mp.composeapp.generated.resources.user_settings_status_empty
import schneaggchatv3mp.composeapp.generated.resources.user_settings_you_say
import schneaggchatv3mp.composeapp.generated.resources.user_settings_about_title
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Groups
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.AboutCardPart
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.AboutCardDivider
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.AboutCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.utilities.NameError
import org.lerchenflo.schneaggchatv3mp.utilities.toStringResource
import org.lerchenflo.schneaggchatv3mp.utilities.validateUsername
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import io.github.lerchenflo.taptarget.tapTarget
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.BirthdatePickerPopup
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.ChangeDialog
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsDivider
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.isPhoneNumberValid
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.change
import schneaggchatv3mp.composeapp.generated.resources.change_gebi_date
import schneaggchatv3mp.composeapp.generated.resources.change_status
import schneaggchatv3mp.composeapp.generated.resources.change_username
import schneaggchatv3mp.composeapp.generated.resources.change_username_description
import schneaggchatv3mp.composeapp.generated.resources.change_username_placeholder
import schneaggchatv3mp.composeapp.generated.resources.current_phone_number
import schneaggchatv3mp.composeapp.generated.resources.change_phone_number
import schneaggchatv3mp.composeapp.generated.resources.edit_profile_picture
import schneaggchatv3mp.composeapp.generated.resources.error_cannot_be_the_same_username
import schneaggchatv3mp.composeapp.generated.resources.invalid_phone_number
import schneaggchatv3mp.composeapp.generated.resources.phone_number_infotext
import schneaggchatv3mp.composeapp.generated.resources.phone_number_placeholder
import schneaggchatv3mp.composeapp.generated.resources.user_settings
import schneaggchatv3mp.composeapp.generated.resources.user_settings_group_profile
import org.lerchenflo.schneaggchatv3mp.sharedUi.pickers.ImagePickPurpose
import org.lerchenflo.schneaggchatv3mp.sharedUi.pickers.ImageSourcePickerDialog

@Composable
fun UserSettings(
    modifier: Modifier = Modifier
        .fillMaxWidth(),
    userSettingsViewModel: UserSettingsViewModel,
    sharedSettingsViewmodel: SharedSettingsViewmodel,
    onBackClick : () -> Unit
) {

    val ownuser = sharedSettingsViewmodel.ownUser

    SessionCache.authStateValue // reactive read: recompose once autologin finishes instead of staying blank
    val dev = SessionCache.requireLoggedIn()?.developer ?: return

    var showChangeUsernamePopup by remember { mutableStateOf(false) }
    var showChangeStatusPopup by remember { mutableStateOf(false) }
    var showChangeBirthDatePopup by remember { mutableStateOf(false) }
    var showChangePhoneNumberPopup by remember { mutableStateOf(false) }



    var showImagePickerDialog by remember { mutableStateOf(false) }

    ImageSourcePickerDialog(
        visible = showImagePickerDialog,
        purpose = ImagePickPurpose.ROUND_PICTURE,
        onImagesPicked = { photos, _ -> photos.firstOrNull()?.let { userSettingsViewModel.changeProfilePicture(it) } },
        onDismiss = { showImagePickerDialog = false }
    )



    Column {

        ActivityTitle(
            title = stringResource(Res.string.user_settings),
            onBackClick = onBackClick,
            backButtonModifier = Modifier.tapTarget("user_settings_back_button")
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


            // What I say about myself (my status, I edit it) and what my friends write about me
            // (only they can change it): the same card as in the chat details
            AboutCard(
                title = stringResource(Res.string.user_settings_about_title),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                AboutCardPart(
                    label = stringResource(Res.string.user_settings_you_say),
                    text = ownuser?.status,
                    emptyText = stringResource(Res.string.user_settings_status_empty),
                    isQuote = true,
                    leading = {
                        ProfilePictureView(
                            filepath = ownuser?.profilePictureUrl ?: "",
                            modifier = Modifier.size(22.dp).clip(CircleShape),
                            contentDescription = null
                        )
                    },
                    onEditClick = { showChangeStatusPopup = true },
                    emptyActionText = stringResource(Res.string.user_settings_status_set)
                )
                AboutCardDivider()
                AboutCardPart(
                    label = stringResource(Res.string.about_friends_say),
                    text = ownuser?.description,
                    emptyText = stringResource(Res.string.user_settings_friends_empty),
                    leading = {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    hint = stringResource(Res.string.user_settings_friends_hint)
                )
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            SettingsDivider(
                title = stringResource(Res.string.user_settings_group_profile),
                showTopDivider = false
            )

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

            //Phone number
            SettingsOption(
                icon = Icons.Default.Phone,
                text = stringResource(Res.string.change_phone_number),
                subtext = if (ownuser?.phoneNumber == null || ownuser.phoneNumber.isEmpty()) {
                    stringResource(Res.string.phone_number_infotext)
                } else {
                    stringResource(Res.string.current_phone_number, ownuser.phoneNumber)
                },
                onClick = {
                    showChangePhoneNumberPopup = true
                },
                modifier = Modifier.tapTarget("user_settings_phone_number")
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        }
    }

    if(showChangeUsernamePopup){
        val cannot_be_same_username_text = stringResource(Res.string.error_cannot_be_the_same_username)
        val nameErrorTexts = NameError.entries.associateWith { stringResource(it.toStringResource()) }

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
                } else {
                    // The server trims the new name before checking it
                    validateUsername(newValue.trim())?.let { nameErrorTexts.getValue(it) }
                }
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

    if (showChangePhoneNumberPopup) {
        val invalidPhoneText = stringResource(Res.string.invalid_phone_number)

        ChangeDialog(
            title = stringResource(Res.string.change_phone_number),
            initialValue = ownuser?.phoneNumber ?: "",
            placeholder = stringResource(Res.string.phone_number_placeholder),
            keyboardType = KeyboardType.Phone,
            onDismiss = { showChangePhoneNumberPopup = false },
            onConfirm = {
                userSettingsViewModel.changePhoneNumber(it)
            },
            confirmButtonText = stringResource(Res.string.change),
            validator = { newValue ->
                if (newValue.isBlank() || isPhoneNumberValid(newValue)) null else invalidPhoneText
            }
        )
    }

}
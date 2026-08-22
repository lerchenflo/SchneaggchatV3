package org.lerchenflo.schneaggchatv3mp.settings.presentation.privacyandsecurity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.onboarding.tapTarget
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.ConfirmationDialog
import org.lerchenflo.schneaggchatv3mp.getDeleteAccountUrl
import org.lerchenflo.schneaggchatv3mp.getPrivacyPolicyUrl
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.schneaggmapsettings.LocationSharingDialog
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.ChangeDialog
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsDivider
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.DeleteButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.emailProviderWarning
import org.lerchenflo.schneaggchatv3mp.utilities.isEmailValid
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.are_you_sure_you_want_to_logout
import schneaggchatv3mp.composeapp.generated.resources.change
import schneaggchatv3mp.composeapp.generated.resources.change_email
import schneaggchatv3mp.composeapp.generated.resources.change_password
import schneaggchatv3mp.composeapp.generated.resources.change_password_description
import schneaggchatv3mp.composeapp.generated.resources.delete_account
import schneaggchatv3mp.composeapp.generated.resources.email
import schneaggchatv3mp.composeapp.generated.resources.email_not_verified_icon_description
import schneaggchatv3mp.composeapp.generated.resources.email_provider_warning
import schneaggchatv3mp.composeapp.generated.resources.email_verified_icon_description
import schneaggchatv3mp.composeapp.generated.resources.emailinfo
import schneaggchatv3mp.composeapp.generated.resources.emailinfo_unverified
import schneaggchatv3mp.composeapp.generated.resources.invalid_email
import schneaggchatv3mp.composeapp.generated.resources.logout
import schneaggchatv3mp.composeapp.generated.resources.privacy_and_security
import schneaggchatv3mp.composeapp.generated.resources.privacy_group_legal
import schneaggchatv3mp.composeapp.generated.resources.privacy_group_location
import schneaggchatv3mp.composeapp.generated.resources.privacy_group_security
import schneaggchatv3mp.composeapp.generated.resources.privacy_policy
import schneaggchatv3mp.composeapp.generated.resources.privacy_policy_info
import schneaggchatv3mp.composeapp.generated.resources.share_location_global
import schneaggchatv3mp.composeapp.generated.resources.share_location_global_info

@Composable
fun PrivacyAndSecuritySettings(
    modifier: Modifier = Modifier.fillMaxWidth(),
    viewModel: PrivacyAndSecurityViewModel,
    sharedSettingsViewmodel: SharedSettingsViewmodel,
    onBackClick: () -> Unit
) {
    val ownuser = sharedSettingsViewmodel.ownUser
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showChangeEmailPopup by remember { mutableStateOf(false) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var locationSharingDialogShown by remember { mutableStateOf(false) }

    Column {
        ActivityTitle(
            title = stringResource(Res.string.privacy_and_security),
            onBackClick = onBackClick,
            backButtonModifier = Modifier.tapTarget("privacy_settings_back_button")
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
        ) {
            // Group 1: Security & Authentication
            SettingsDivider(
                title = stringResource(Res.string.privacy_group_security),
                showTopDivider = false
            )

            // Change Password
            SettingsOption(
                icon = Icons.Default.Key,
                text = stringResource(Res.string.change_password),
                subtext = stringResource(Res.string.change_password_description),
                onClick = { showChangePasswordDialog = true }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // Email & Verification
            SettingsOption(
                icon = Icons.Default.Mail,
                text = stringResource(Res.string.email),
                subtext = (if (ownuser?.isEmailVerified() == true) stringResource(Res.string.emailinfo) else stringResource(
                    Res.string.emailinfo_unverified
                )) + "\n" + (ownuser?.email ?: ""),
                onClick = { showChangeEmailPopup = true },
                rightSideIcon = {
                    if (ownuser != null) {
                        if (ownuser.isEmailVerified()) {
                            Icon(
                                imageVector = Icons.Outlined.Verified,
                                contentDescription = stringResource(Res.string.email_verified_icon_description),
                                modifier = Modifier.size(30.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = stringResource(Res.string.email_not_verified_icon_description),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable {
                                        viewModel.sendEmailVerify()
                                    }
                            )
                        }
                    }
                }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // Logout
            SettingsOption(
                icon = Icons.AutoMirrored.Default.ExitToApp,
                text = stringResource(Res.string.logout),
                onClick = { showLogoutDialog = true }
            )

            // Group 2: Location & Privacy
            SettingsDivider(
                title = stringResource(Res.string.privacy_group_location)
            )

            // Location Sharing Dialog
            SettingsOption(
                icon = if (viewModel.shareLocationGlobal) Icons.Default.LocationOn else Icons.Default.LocationOff,
                text = stringResource(Res.string.share_location_global),
                subtext = stringResource(Res.string.share_location_global_info),
                onClick = { locationSharingDialogShown = true },
                rightSideIcon = {
                    if (viewModel.shareLocationGlobal) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // Group 3: Data & Legal
            SettingsDivider(
                title = stringResource(Res.string.privacy_group_legal)
            )

            // Privacy Policy
            SettingsOption(
                icon = Icons.Default.Description,
                text = stringResource(Res.string.privacy_policy),
                subtext = stringResource(Res.string.privacy_policy_info),
                onClick = {
                    scope.launch {
                        val serverUrl = viewModel.getServerUrl()
                        uriHandler.openUri(getPrivacyPolicyUrl(serverUrl))
                    }
                }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // Delete Account
            DeleteButton(
                text = stringResource(Res.string.delete_account),
                onClick = {
                    scope.launch {
                        val serverUrl = viewModel.getServerUrl()
                        uriHandler.openUri(getDeleteAccountUrl(serverUrl))
                    }
                },
                modifier = Modifier
                    .padding(
                        top = 8.dp,
                        bottom = 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    )
                    .fillMaxWidth()
            )
        }
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { oldPassword, newPassword ->
                viewModel.changePassword(oldPassword, newPassword) { success ->
                    if (success) {
                        showChangePasswordDialog = false
                    }
                }
            },
            isLoading = viewModel.isChangingPassword
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
                viewModel.changeEmail(it)
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

    if (showLogoutDialog) {
        ConfirmationDialog(
            message = stringResource(Res.string.are_you_sure_you_want_to_logout),
            onConfirm = {
                viewModel.logout()
            },
            onDismiss = {
                showLogoutDialog = false
            }
        )
    }

    if (locationSharingDialogShown) {
        LocationSharingDialog(
            shareLocationGlobal = viewModel.shareLocationGlobal,
            friends = viewModel.friends,
            onSave = { global, friendDrafts -> viewModel.saveLocationSharing(global, friendDrafts) },
            onDismiss = { locationSharingDialogShown = false }
        )
    }
}

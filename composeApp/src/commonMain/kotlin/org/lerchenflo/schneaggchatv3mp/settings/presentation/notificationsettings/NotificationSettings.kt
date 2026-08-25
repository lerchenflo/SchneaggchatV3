package org.lerchenflo.schneaggchatv3mp.settings.presentation.notificationsettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.app.onboarding.tapTarget
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsDivider
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings.WakeSettingsDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.notification_group_general
import schneaggchatv3mp.composeapp.generated.resources.notification_permission_permanently_denied_message
import schneaggchatv3mp.composeapp.generated.resources.notification_permission_required_title
import schneaggchatv3mp.composeapp.generated.resources.notifications
import schneaggchatv3mp.composeapp.generated.resources.notifications_disabled
import schneaggchatv3mp.composeapp.generated.resources.notifications_enabled
import schneaggchatv3mp.composeapp.generated.resources.open_settings
import schneaggchatv3mp.composeapp.generated.resources.wake_settings
import schneaggchatv3mp.composeapp.generated.resources.wake_settings_info

@Composable
fun NotificationSettings(
    modifier: Modifier = Modifier.fillMaxWidth(),
    viewModel: NotificationSettingsViewModel,
    onBackClick: () -> Unit
) {
    var showWakeSettingsDialog by rememberSaveable { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkNotificationPermission()
    }

    Column {
        ActivityTitle(
            title = stringResource(Res.string.notifications),
            onBackClick = onBackClick,
            backButtonModifier = Modifier.tapTarget("notification_settings_back_button")
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
        ) {
            // Group 1: General Notifications
            SettingsDivider(
                title = stringResource(Res.string.notification_group_general),
                showTopDivider = false
            )

            val isPermissionGranted = viewModel.notificationPermissionState == PermissionState.GRANTED

            // Notification Permission
            SettingsOption(
                icon = Icons.Default.Notifications,
                text = stringResource(Res.string.notifications),
                subtext = if (isPermissionGranted) {
                    stringResource(Res.string.notifications_enabled)
                } else {
                    stringResource(Res.string.notifications_disabled)
                },
                onClick = {
                    viewModel.requestNotificationPermission()
                },
                rightSideIcon = {
                    if (isPermissionGranted) {
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = "Notifications enabled",
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Notifications disabled",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(30.dp)
                                .clickable {
                                    viewModel.requestNotificationPermission()
                                }
                        )
                    }
                }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // Group 2: Wake-ups (Android only)
            if (koinInject<AppVersion>().isAndroid()) {
                SettingsDivider(
                    title = stringResource(Res.string.wake_settings)
                )

                SettingsOption(
                    icon = Icons.Default.Alarm,
                    text = stringResource(Res.string.wake_settings),
                    subtext = stringResource(Res.string.wake_settings_info),
                    onClick = { showWakeSettingsDialog = true },
                    modifier = Modifier.tapTarget("settings_notifications_wake")
                )

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }
        }
    }

    if (showWakeSettingsDialog) {
        WakeSettingsDialog(
            wakeEnabledGlobal = viewModel.wakeEnabledGlobal,
            friends = viewModel.friends,
            onSave = { global, friendDrafts ->
                viewModel.saveWakeSettings(global, friendDrafts)
            },
            onDismiss = { showWakeSettingsDialog = false }
        )
    }

    if (viewModel.showOpenSettingsDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissOpenSettingsDialog() },
            title = {
                Text(text = stringResource(Res.string.notification_permission_required_title))
            },
            text = {
                Text(text = stringResource(Res.string.notification_permission_permanently_denied_message))
            },
            confirmButton = {
                TextButton(onClick = { viewModel.openAppSettings() }) {
                    Text(text = stringResource(Res.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissOpenSettingsDialog() }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            }
        )
    }
}

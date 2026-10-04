package org.lerchenflo.schneaggchatv3mp.settings.presentation.loggedindevices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.ConfirmationDialog
import org.lerchenflo.schneaggchatv3mp.settings.data.DEVICETYPE
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.logged_in_devices
import schneaggchatv3mp.composeapp.generated.resources.logged_in_devices_last_active
import schneaggchatv3mp.composeapp.generated.resources.logged_in_devices_load_error
import schneaggchatv3mp.composeapp.generated.resources.logged_in_devices_logout_all
import schneaggchatv3mp.composeapp.generated.resources.logged_in_devices_logout_all_confirm
import schneaggchatv3mp.composeapp.generated.resources.logged_in_devices_logout_confirm
import schneaggchatv3mp.composeapp.generated.resources.logged_in_devices_logout_device
import schneaggchatv3mp.composeapp.generated.resources.logged_in_devices_retry
import schneaggchatv3mp.composeapp.generated.resources.logged_in_devices_since
import schneaggchatv3mp.composeapp.generated.resources.logged_in_devices_this_device
import schneaggchatv3mp.composeapp.generated.resources.logged_in_devices_unknown_device

@Composable
fun LoggedInDevicesScreen(
    modifier: Modifier = Modifier.fillMaxWidth(),
    viewModel: LoggedInDevicesViewModel,
    onBackClick: () -> Unit
) {
    var deviceToLogout by remember { mutableStateOf<LoggedInDevice?>(null) }
    var showLogoutAllDialog by remember { mutableStateOf(false) }

    Column {
        ActivityTitle(
            title = stringResource(Res.string.logged_in_devices),
            onBackClick = onBackClick
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        when {
            viewModel.isLoading && viewModel.devices.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            viewModel.loadFailed -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.logged_in_devices_load_error),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = viewModel::loadDevices) {
                        Text(stringResource(Res.string.logged_in_devices_retry))
                    }
                }
            }

            else -> {
                LazyColumn(modifier = modifier) {
                    items(items = viewModel.devices, key = { it.id }) { device ->
                        LoggedInDeviceRow(
                            device = device,
                            onLogoutClick = { deviceToLogout = device }
                        )

                        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                    }

                    item(key = "logout_all_devices") {
                        SettingsOption(
                            icon = Icons.AutoMirrored.Default.Logout,
                            text = stringResource(Res.string.logged_in_devices_logout_all),
                            onClick = { showLogoutAllDialog = true }
                        )
                    }
                }
            }
        }
    }

    deviceToLogout?.let { device ->
        ConfirmationDialog(
            message = stringResource(
                Res.string.logged_in_devices_logout_confirm,
                device.displayName ?: stringResource(Res.string.logged_in_devices_unknown_device)
            ),
            onConfirm = { viewModel.logoutDevice(device) },
            onDismiss = { deviceToLogout = null }
        )
    }

    if (showLogoutAllDialog) {
        ConfirmationDialog(
            message = stringResource(Res.string.logged_in_devices_logout_all_confirm),
            onConfirm = { viewModel.logoutAllDevices() },
            onDismiss = { showLogoutAllDialog = false }
        )
    }
}

@Composable
private fun LoggedInDeviceRow(
    device: LoggedInDevice,
    onLogoutClick: () -> Unit
) {
    val subtext = buildList {
        if (device.isCurrentDevice) add(stringResource(Res.string.logged_in_devices_this_device))
        add(
            listOfNotNull(
                device.deviceType?.label(),
                stringResource(Res.string.logged_in_devices_last_active, millisToTimeDateOrYesterday(device.lastUsedAt))
            ).joinToString(" · ")
        )
        add(stringResource(Res.string.logged_in_devices_since, millisToString(device.createdAt, format = "dd.MM.yyyy")))
    }.joinToString("\n")

    SettingsOption(
        icon = device.deviceType.icon(),
        text = device.displayName ?: stringResource(Res.string.logged_in_devices_unknown_device),
        subtext = subtext,
        highlighted = device.isCurrentDevice,
        // The own device logs out via the normal logout in Privacy & Security
        onClick = { if (!device.isCurrentDevice) onLogoutClick() },
        rightSideIcon = {
            if (!device.isCurrentDevice) {
                IconButton(onClick = onLogoutClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Logout,
                        contentDescription = stringResource(Res.string.logged_in_devices_logout_device),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

// Platform names are product names, not translated
private fun DEVICETYPE.label(): String = when (this) {
    DEVICETYPE.ANDROID -> "Android"
    DEVICETYPE.IOS -> "iOS"
    DEVICETYPE.DESKTOP -> "Desktop"
    DEVICETYPE.WEB -> "Web"
}

private fun DEVICETYPE?.icon(): ImageVector = when (this) {
    DEVICETYPE.ANDROID -> Icons.Default.PhoneAndroid
    DEVICETYPE.IOS -> Icons.Default.PhoneIphone
    DEVICETYPE.DESKTOP -> Icons.Default.Computer
    DEVICETYPE.WEB -> Icons.Default.Language
    null -> Icons.Default.Devices
}

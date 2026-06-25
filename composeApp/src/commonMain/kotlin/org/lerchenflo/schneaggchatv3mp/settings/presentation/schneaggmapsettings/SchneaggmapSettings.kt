package org.lerchenflo.schneaggchatv3mp.settings.presentation.schneaggmapsettings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.merge_map_locations
import schneaggchatv3mp.composeapp.generated.resources.merge_map_locations_info
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_settings
import schneaggchatv3mp.composeapp.generated.resources.share_location_global
import schneaggchatv3mp.composeapp.generated.resources.share_location_global_info

@Composable
fun SchneaggmapSettings(
    modifier: Modifier = Modifier
        .fillMaxWidth(),
    schneaggmapSettingsViewModel: SchneaggmapSettingsViewModel,
    sharedSettingsViewmodel: SharedSettingsViewmodel,
    onBackClick: () -> Unit
) {
    var locationSharingDialogShown by remember { mutableStateOf(false) }

    Column {
        ActivityTitle(
            title = stringResource(Res.string.schneaggmap_settings),
            onBackClick = onBackClick
        )
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
        ) {

            // Merge map locations when zooming (local preference only)
            SettingsSwitch(
                titletext = stringResource(Res.string.merge_map_locations),
                infotext = stringResource(Res.string.merge_map_locations_info),
                switchchecked = schneaggmapSettingsViewModel.mergeMapLocations,
                onSwitchChange = { schneaggmapSettingsViewModel.updateMergeMapLocations(it) },
                icon = null
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // Opens a popup with the global switch + a per-friend toggle for each friend,
            // instead of toggling instantly on tap.
            SettingsOption(
                icon = if (schneaggmapSettingsViewModel.shareLocationGlobal) Icons.Default.LocationOn else Icons.Default.LocationOff,
                text = stringResource(Res.string.share_location_global),
                subtext = stringResource(Res.string.share_location_global_info),
                onClick = { locationSharingDialogShown = true },
                rightSideIcon = {
                    if (schneaggmapSettingsViewModel.shareLocationGlobal) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        }
    }

    if (locationSharingDialogShown) {
        LocationSharingDialog(
            shareLocationGlobal = schneaggmapSettingsViewModel.shareLocationGlobal,
            onShareLocationGlobalChange = { schneaggmapSettingsViewModel.updateShareLocationGlobal(it) },
            advancedLocationSharing = schneaggmapSettingsViewModel.advancedLocationSharing,
            onAdvancedLocationSharingChange = { schneaggmapSettingsViewModel.updateAdvancedLocationSharing(it) },
            friends = schneaggmapSettingsViewModel.friends,
            onFriendShareChange = { friendId, share -> schneaggmapSettingsViewModel.updateFriendLocationSharing(friendId, share) },
            onFriendAdvancedShareChange = { friendId, shareSpeedHeading, snailTrailHours ->
                schneaggmapSettingsViewModel.updateFriendAdvancedSharing(friendId, shareSpeedHeading, snailTrailHours)
            },
            onDismiss = { locationSharingDialogShown = false }
        )
    }
}

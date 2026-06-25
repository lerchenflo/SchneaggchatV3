package org.lerchenflo.schneaggchatv3mp.settings.presentation.schneaggmapsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults.textContentColor
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.advanced_location_sharing
import schneaggchatv3mp.composeapp.generated.resources.advanced_location_sharing_info
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.no_friends
import schneaggchatv3mp.composeapp.generated.resources.share_location_friends_title
import schneaggchatv3mp.composeapp.generated.resources.share_location_global
import schneaggchatv3mp.composeapp.generated.resources.share_location_global_info
import schneaggchatv3mp.composeapp.generated.resources.share_snail_trail
import schneaggchatv3mp.composeapp.generated.resources.share_speed_heading

@Composable
fun LocationSharingDialog(
    shareLocationGlobal: Boolean,
    onShareLocationGlobalChange: (Boolean) -> Unit,
    advancedLocationSharing: Boolean,
    onAdvancedLocationSharingChange: (Boolean) -> Unit,
    friends: List<User>,
    onFriendShareChange: (friendId: String, share: Boolean) -> Unit,
    onFriendAdvancedShareChange: (friendId: String, shareSpeedHeading: Boolean, snailTrailHours: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.share_location_global)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Big global switch - handles the existing global share functionality
                SettingsSwitch(
                    titletext = stringResource(Res.string.share_location_global),
                    infotext = stringResource(Res.string.share_location_global_info),
                    switchchecked = shareLocationGlobal,
                    onSwitchChange = onShareLocationGlobalChange,
                    icon = null
                )

                // Sends our own speed/heading, and reveals the per-friend advanced controls
                // below. Only meaningful once basic sharing is on.
                SettingsSwitch(
                    titletext = stringResource(Res.string.advanced_location_sharing),
                    infotext = stringResource(Res.string.advanced_location_sharing_info),
                    switchchecked = advancedLocationSharing,
                    onSwitchChange = onAdvancedLocationSharingChange,
                    icon = null,
                    enabled = shareLocationGlobal
                )

                if (friends.isNotEmpty()) {
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                    Text(
                        text = stringResource(Res.string.share_location_friends_title),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )

                    friends.forEach { friend ->
                        val contentAlpha = if (shareLocationGlobal) 1f else 0.38f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = friend.nickName ?: friend.name,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyLarge,
                                color = LocalContentColor.current.copy(alpha = contentAlpha),
                                modifier = Modifier.weight(1f)
                            )

                            Switch(
                                checked = friend.locationShared,
                                enabled = shareLocationGlobal,
                                onCheckedChange = { onFriendShareChange(friend.id, it) }
                            )
                        }

                        // Per-friend advanced controls - only shown once "Advanced location
                        // sharing" is toggled on and this friend may see our location at all.
                        if (advancedLocationSharing && friend.locationShared) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(Res.string.share_speed_heading),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )

                                Switch(
                                    checked = friend.shareSpeedHeading,
                                    enabled = shareLocationGlobal,
                                    onCheckedChange = { checked ->
                                        onFriendAdvancedShareChange(friend.id, checked, friend.snailTrailHours)
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(Res.string.share_snail_trail),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )

                                Switch(
                                    checked = friend.snailTrailHours != null,
                                    enabled = shareLocationGlobal,
                                    onCheckedChange = { checked ->
                                        onFriendAdvancedShareChange(
                                            friend.id,
                                            friend.shareSpeedHeading,
                                            if (checked) 0 else null // 0 = full 24h retained history
                                        )
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.no_friends)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.close), color = textContentColor)
            }
        }
    )
}

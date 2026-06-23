package org.lerchenflo.schneaggchatv3mp.settings.presentation.schneaggmapsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults.textContentColor
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.share_location_friends_title
import schneaggchatv3mp.composeapp.generated.resources.share_location_global
import schneaggchatv3mp.composeapp.generated.resources.share_location_global_info

@Composable
fun LocationSharingDialog(
    shareLocationGlobal: Boolean,
    onShareLocationGlobalChange: (Boolean) -> Unit,
    friends: List<User>,
    onFriendShareChange: (friendId: String, share: Boolean) -> Unit,
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

                if (friends.isNotEmpty()) {
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                    Text(
                        text = stringResource(Res.string.share_location_friends_title),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )

                    friends.forEach { friend ->
                        SettingsSwitch(
                            titletext = friend.nickName ?: friend.name,
                            infotext = null,
                            switchchecked = friend.locationShared,
                            onSwitchChange = { onFriendShareChange(friend.id, it) },
                            icon = null,
                            enabled = shareLocationGlobal
                        )
                    }
                } else {
                    Text(
                        text = "no friends"
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

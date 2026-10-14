package org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.no_friends
import schneaggchatv3mp.composeapp.generated.resources.save
import schneaggchatv3mp.composeapp.generated.resources.wake_allow_friends_title
import schneaggchatv3mp.composeapp.generated.resources.wake_allow_global
import schneaggchatv3mp.composeapp.generated.resources.wake_allow_global_info

/** One friend's pending "may wake me" value - not sent to the server until Save is pressed. */
data class WakePermissionDraft(
    val friendId: String,
    val allowWake: Boolean,
)

/**
 * Lets the user pick who may wake them. Both the master switch and every friend default to off,
 * so nobody can wake this user until they deliberately opt in here.
 */
@Composable
fun WakeSettingsDialog(
    wakeEnabledGlobal: Boolean,
    friends: List<User>,
    onSave: (global: Boolean, friendDrafts: List<WakePermissionDraft>) -> Unit,
    onDismiss: () -> Unit,
) {
    var draftGlobal by remember { mutableStateOf(wakeEnabledGlobal) }

    val draftFriendPermissions = remember {
        friends.associate { friend ->
            friend.id to mutableStateOf(friend.wakeupEnabled)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.wake_allow_global)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Master kill switch. While off nobody can wake this user, whatever the
                // per-friend switches below say - they stay visible but disabled so the user
                // can see their choices are preserved.
                SettingsSwitch(
                    titletext = stringResource(Res.string.wake_allow_global),
                    infotext = stringResource(Res.string.wake_allow_global_info),
                    switchchecked = draftGlobal,
                    onSwitchChange = { draftGlobal = it },
                    icon = null
                )

                if (friends.isNotEmpty()) {
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                    Text(
                        text = stringResource(Res.string.wake_allow_friends_title),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )

                    friends.forEach { friend ->
                        val draftState = draftFriendPermissions[friend.id] ?: return@forEach
                        var allowWake by draftState
                        val contentAlpha = if (draftGlobal) 1f else 0.38f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = friend.displayName,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyLarge,
                                color = LocalContentColor.current.copy(alpha = contentAlpha),
                                modifier = Modifier.weight(1f)
                            )

                            Switch(
                                checked = allowWake,
                                enabled = draftGlobal,
                                onCheckedChange = { allowWake = it }
                            )
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
            TextButton(onClick = {
                val friendDrafts = draftFriendPermissions.map { (friendId, state) ->
                    WakePermissionDraft(friendId = friendId, allowWake = state.value)
                }
                onSave(draftGlobal, friendDrafts)
                onDismiss()
            }) {
                Text(text = stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.close))
            }
        }
    )
}

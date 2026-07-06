package org.lerchenflo.schneaggchatv3mp.settings.presentation.schneaggmapsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import schneaggchatv3mp.composeapp.generated.resources.advanced_location_sharing
import schneaggchatv3mp.composeapp.generated.resources.advanced_location_sharing_info
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.no_friends
import schneaggchatv3mp.composeapp.generated.resources.save
import schneaggchatv3mp.composeapp.generated.resources.share_location_friends_title
import schneaggchatv3mp.composeapp.generated.resources.share_location_global
import schneaggchatv3mp.composeapp.generated.resources.share_location_global_info
import schneaggchatv3mp.composeapp.generated.resources.share_snail_trail
import schneaggchatv3mp.composeapp.generated.resources.share_speed_heading

/** Local, editable draft of one friend's sharing settings - not sent to the server until Save is pressed. */
private data class FriendShareDraftState(
    var share: Boolean,
    var shareSpeedHeading: Boolean,
    var snailTrail: Boolean,
)

@Composable
fun LocationSharingDialog(
    shareLocationGlobal: Boolean,
    advancedLocationSharing: Boolean,
    onAdvancedLocationSharingChange: (Boolean) -> Unit,
    friends: List<User>,
    onSave: (globalShare: Boolean, friendDrafts: List<FriendShareDraft>) -> Unit,
    onDismiss: () -> Unit,
) {
    var draftGlobalShare by remember { mutableStateOf(shareLocationGlobal) }

    val draftFriendShares = remember {
        friends.associate { friend ->
            friend.id to mutableStateOf(
                FriendShareDraftState(
                    share = friend.locationShared,
                    shareSpeedHeading = friend.shareSpeedHeading,
                    snailTrail = friend.snailTrail,
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.share_location_global)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Big global switch - local-only, just reflects "sharing with anyone at all".
                // Committing it force-enables/disables sharing for every friend on Save.
                SettingsSwitch(
                    titletext = stringResource(Res.string.share_location_global),
                    infotext = stringResource(Res.string.share_location_global_info),
                    switchchecked = draftGlobalShare,
                    onSwitchChange = { draftGlobalShare = it },
                    icon = null
                )

                // Sends our own speed/heading, and reveals the per-friend advanced controls
                // below. Only meaningful once basic sharing is on. Saved immediately (local pref).
                SettingsSwitch(
                    titletext = stringResource(Res.string.advanced_location_sharing),
                    infotext = stringResource(Res.string.advanced_location_sharing_info),
                    switchchecked = advancedLocationSharing,
                    onSwitchChange = onAdvancedLocationSharingChange,
                    icon = null,
                    enabled = draftGlobalShare
                )

                if (friends.isNotEmpty()) {
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                    Text(
                        text = stringResource(Res.string.share_location_friends_title),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )

                    friends.forEach { friend ->
                        val draftState = draftFriendShares[friend.id] ?: return@forEach
                        var draft by draftState
                        val contentAlpha = if (draftGlobalShare) 1f else 0.38f

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
                                checked = draft.share,
                                enabled = draftGlobalShare,
                                onCheckedChange = { draft = draft.copy(share = it) }
                            )
                        }

                        if (advancedLocationSharing && draft.share) {
                            val subColor = MaterialTheme.colorScheme.secondary
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 2.dp, bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(Res.string.share_speed_heading),
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = subColor,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Checkbox(
                                        checked = draft.shareSpeedHeading,
                                        enabled = draftGlobalShare,
                                        onCheckedChange = { checked ->
                                            draft = draft.copy(shareSpeedHeading = checked)
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
                                        style = MaterialTheme.typography.bodySmall,
                                        color = subColor,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Checkbox(
                                        checked = draft.snailTrail,
                                        enabled = draftGlobalShare,
                                        onCheckedChange = { checked ->
                                            draft = draft.copy(snailTrail = checked)
                                        }
                                    )
                                }
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
            TextButton(onClick = {
                val friendDrafts = draftFriendShares.map { (friendId, state) ->
                    val draft = state.value
                    FriendShareDraft(
                        friendId = friendId,
                        share = draft.share,
                        shareSpeedHeading = draft.shareSpeedHeading,
                        snailTrail = draft.snailTrail,
                    )
                }
                onSave(draftGlobalShare, friendDrafts)
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

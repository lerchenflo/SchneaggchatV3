package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.MAX_GROUP_DESCRIPTION_LENGTH
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import org.lerchenflo.schneaggchatv3mp.sharedUi.clearFocusOnTap
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add_description_placeholder
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.group_description
import schneaggchatv3mp.composeapp.generated.resources.group_members_can_add_info
import schneaggchatv3mp.composeapp.generated.resources.group_members_can_add_label
import schneaggchatv3mp.composeapp.generated.resources.group_name
import schneaggchatv3mp.composeapp.generated.resources.group_settings_title
import schneaggchatv3mp.composeapp.generated.resources.save

/**
 * Name, description and - for admins - whether members may add people, saved together. The draft
 * lives here while the popup is open, so a rejected name keeps everything that was typed.
 */
@Composable
fun GroupSettingsDialog(
    group: Group,
    iAmAdmin: Boolean,
    nameError: UiText?,
    onConfirm: (name: String, description: String, membersCanAdd: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(group.id) { mutableStateOf(group.name) }
    var description by remember(group.id) { mutableStateOf(group.description) }
    var membersCanAdd by remember(group.id) { mutableStateOf(group.membersCanAdd) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.group_settings_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearFocusOnTap()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = stringResource(Res.string.group_name)) },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { error -> { Text(text = error.asString()) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(MAX_GROUP_DESCRIPTION_LENGTH) },
                    label = { Text(text = stringResource(Res.string.group_description)) },
                    placeholder = { Text(text = stringResource(Res.string.add_description_placeholder)) },
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                // Admins only, the server rejects anyone else
                if (iAmAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsSwitch(
                        modifier = Modifier.fillMaxWidth(),
                        titletext = stringResource(Res.string.group_members_can_add_label),
                        infotext = stringResource(Res.string.group_members_can_add_info),
                        switchchecked = membersCanAdd,
                        onSwitchChange = { membersCanAdd = it },
                        icon = null,
                        titleMaxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), description, membersCanAdd) }) {
                Text(text = stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.cancel))
            }
        },
    )
}

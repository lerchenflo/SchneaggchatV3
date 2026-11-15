package org.lerchenflo.schneaggchatv3mp.settings.presentation.privacyandsecurity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.settings.domain.ProfilePicVisibility
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.profile_pic_visibility
import schneaggchatv3mp.composeapp.generated.resources.profile_pic_visibility_friends
import schneaggchatv3mp.composeapp.generated.resources.profile_pic_visibility_friends_and_groups
import schneaggchatv3mp.composeapp.generated.resources.profile_pic_visibility_info
import schneaggchatv3mp.composeapp.generated.resources.profile_pic_visibility_public

/** Label of a [ProfilePicVisibility] value in the privacy settings. */
fun ProfilePicVisibility.labelResource(): StringResource = when (this) {
    ProfilePicVisibility.PUBLIC -> Res.string.profile_pic_visibility_public
    ProfilePicVisibility.FRIENDS_AND_GROUPS -> Res.string.profile_pic_visibility_friends_and_groups
    ProfilePicVisibility.FRIENDS -> Res.string.profile_pic_visibility_friends
}

/**
 * Lets the user pick who may see their profile picture. [onConfirm] gets the picked value; the
 * caller saves it on the server, so the dialog never shows a value as active itself.
 */
@Composable
fun ProfilePicVisibilityDialog(
    current: ProfilePicVisibility,
    onConfirm: (ProfilePicVisibility) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.profile_pic_visibility)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.profile_pic_visibility_info),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(modifier = Modifier.selectableGroup()) {
                    ProfilePicVisibility.entries.forEach { visibility ->
                        VisibilityRow(
                            visibility = visibility,
                            selected = visibility == selected,
                            onSelect = { selected = visibility }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
private fun VisibilityRow(
    visibility: ProfilePicVisibility,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The row handles the click, so the button itself takes none
        RadioButton(selected = selected, onClick = null)
        Text(
            text = stringResource(visibility.labelResource()),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

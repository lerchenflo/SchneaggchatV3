package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionSettings
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.DEFAULT_SPLIT_CURRENCY
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.delete
import schneaggchatv3mp.composeapp.generated.resources.group_extension_create
import schneaggchatv3mp.composeapp.generated.resources.group_extension_create_title
import schneaggchatv3mp.composeapp.generated.resources.group_extension_currency
import schneaggchatv3mp.composeapp.generated.resources.group_extension_currency_hint
import schneaggchatv3mp.composeapp.generated.resources.group_extension_delete
import schneaggchatv3mp.composeapp.generated.resources.group_extension_delete_confirm
import schneaggchatv3mp.composeapp.generated.resources.group_extension_more_options
import schneaggchatv3mp.composeapp.generated.resources.group_extension_rename
import schneaggchatv3mp.composeapp.generated.resources.group_extension_title_label

private const val CURRENCY_CODE_LENGTH = 3

/**
 * Asks for the title of a new extension of [type], and for SPLIT also its currency (ISO 4217 code).
 * Create stays disabled until the input is valid, so there is no error text to show.
 */
@Composable
fun AddExtensionDialog(
    type: GroupExtensionType,
    onDismiss: () -> Unit,
    onCreate: (title: String, settings: ExtensionSettings) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf(DEFAULT_SPLIT_CURRENCY) }

    val isSplit = type == GroupExtensionType.SPLIT
    val isValid = validateExtensionTitle(title.trim()) == null &&
        (!isSplit || currency.length == CURRENCY_CODE_LENGTH)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.group_extension_create_title, type.toUiText().asString())) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(MAX_EXTENSION_TITLE_LENGTH) },
                    label = { Text(stringResource(Res.string.group_extension_title_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isSplit) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { input ->
                            currency = input.filter { it.isLetter() }.uppercase().take(CURRENCY_CODE_LENGTH)
                        },
                        label = { Text(stringResource(Res.string.group_extension_currency)) },
                        supportingText = { Text(stringResource(Res.string.group_extension_currency_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    val settings = when (type) {
                        GroupExtensionType.SPLIT -> ExtensionSettings.Split(currency = currency)
                        GroupExtensionType.ROUTE_PLANNER -> ExtensionSettings.Route()
                        GroupExtensionType.SHARED_TEXT,
                        GroupExtensionType.QUOTES -> ExtensionSettings.None
                    }
                    onCreate(title.trim(), settings)
                }
            ) {
                Text(stringResource(Res.string.group_extension_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/** Asks before an extension is deleted with all its entries, for every member of the group. */
@Composable
fun DeleteExtensionDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.group_extension_delete)) },
        text = { Text(stringResource(Res.string.group_extension_delete_confirm, title)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(Res.string.delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/** Top bar menu of an extension screen: rename for every member, delete only when [canDelete]. */
@Composable
fun ExtensionMenu(
    canDelete: Boolean,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.group_extension_more_options)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.group_extension_rename)) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    expanded = false
                    onRenameClick()
                }
            )
            if (canDelete) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(Res.string.group_extension_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        expanded = false
                        onDeleteClick()
                    }
                )
            }
        }
    }
}

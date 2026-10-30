package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionSettings
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.group_extension_add
import schneaggchatv3mp.composeapp.generated.resources.group_extension_item_count
import schneaggchatv3mp.composeapp.generated.resources.group_extensions_title

sealed interface ExtensionListAction {
    data class OnExtensionClick(val extensionId: String) : ExtensionListAction
    data class OnCreateExtension(
        val type: GroupExtensionType,
        val title: String,
        val settings: ExtensionSettings,
    ) : ExtensionListAction
}

/**
 * The extensions section of the group details screen: one row per extension plus an "add" row,
 * whose type menu opens the dialog for the new extension's title.
 */
@Composable
fun ExtensionListSection(
    extensions: List<GroupExtension>,
    onAction: (ExtensionListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddMenu by remember { mutableStateOf(false) }
    var addType by rememberSaveable { mutableStateOf<GroupExtensionType?>(null) }

    addType?.let { type ->
        AddExtensionDialog(
            type = type,
            onDismiss = { addType = null },
            onCreate = { title, settings ->
                addType = null
                onAction(ExtensionListAction.OnCreateExtension(type, title, settings))
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.group_extensions_title))

        extensions.forEach { extension ->
            key(extension.id) {
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(ExtensionListAction.OnExtensionClick(extension.id)) },
                    leadingContent = {
                        Icon(
                            imageVector = extension.type.icon(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = { Text(extension.title) },
                    supportingContent = {
                        val typeName = extension.type.toUiText().asString()
                        // A shared text is one document, counting its entries says nothing
                        if (extension.type == GroupExtensionType.SHARED_TEXT) {
                            Text(typeName)
                        } else {
                            val count = extension.items.size
                            Text("$typeName · ${pluralStringResource(Res.plurals.group_extension_item_count, count, count)}")
                        }
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddMenu = true },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                    )
                },
                headlineContent = { Text(stringResource(Res.string.group_extension_add)) }
            )

            // A group can hold several extensions of the same type, so every type is always offered
            DropdownMenu(
                expanded = showAddMenu,
                onDismissRequest = { showAddMenu = false }
            ) {
                GroupExtensionType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.toUiText().asString()) },
                        leadingIcon = {
                            Icon(
                                imageVector = type.icon(),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            showAddMenu = false
                            addType = type
                        }
                    )
                }
            }
        }
    }
}

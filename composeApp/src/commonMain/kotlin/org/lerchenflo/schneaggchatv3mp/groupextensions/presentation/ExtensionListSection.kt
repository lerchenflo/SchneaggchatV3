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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.group_extension_add
import schneaggchatv3mp.composeapp.generated.resources.group_extensions_title

sealed interface ExtensionListAction {
    data class OnExtensionClick(val extensionId: String) : ExtensionListAction
    data class OnAddExtensionClick(val type: GroupExtensionType) : ExtensionListAction
}

/** The extensions section of the group details screen: one row per extension plus an "add" row. */
@Composable
fun ExtensionListSection(
    extensions: List<GroupExtension>,
    onAction: (ExtensionListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddMenu by remember { mutableStateOf(false) }

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
                    supportingContent = { Text(extension.type.toUiText().asString()) }
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
                            onAction(ExtensionListAction.OnAddExtensionClick(type))
                        }
                    )
                }
            }
        }
    }
}

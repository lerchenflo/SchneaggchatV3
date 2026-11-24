package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionSettings
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsDivider
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.TitleMenuItem
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.group_extension_add
import schneaggchatv3mp.composeapp.generated.resources.group_extension_coming_soon
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
 * The extensions section of the group details screen: one row per extension. New extensions are
 * added through the ⋮ of the group details ([addExtensionMenuItem]).
 */
@Composable
fun ExtensionListSection(
    extensions: List<GroupExtension>,
    onAction: (ExtensionListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsDivider(
            title = stringResource(Res.string.group_extensions_title),
            showTopDivider = false
        )

        extensions.forEachIndexed { index, extension ->
            key(extension.id) {
                val typeName = extension.type.toUiText().asString()
                val count = extension.items.size

                SettingsOption(
                    icon = extension.type.icon(),
                    text = extension.title,
                    // A shared text is one document, counting its entries says nothing
                    subtext = if (extension.type == GroupExtensionType.SHARED_TEXT) {
                        typeName
                    } else {
                        "$typeName · ${pluralStringResource(Res.plurals.group_extension_item_count, count, count)}"
                    },
                    onClick = { onAction(ExtensionListAction.OnExtensionClick(extension.id)) }
                )

                // The caller draws the divider below the section
                if (index < extensions.lastIndex) HorizontalDivider()
            }
        }
    }
}

/**
 * "Add extension" for a top bar menu, its sub items are the extension types. A group can hold several
 * extensions of the same type, so every available type is always offered. Unfinished types are
 * listed as "coming soon" and can't be picked.
 */
@Composable
fun addExtensionMenuItem(onTypeClick: (GroupExtensionType) -> Unit): TitleMenuItem {
    val comingSoon = stringResource(Res.string.group_extension_coming_soon)

    return TitleMenuItem(
        text = stringResource(Res.string.group_extension_add),
        icon = Icons.Default.Add,
        subItems = GroupExtensionType.entries.map { type ->
            TitleMenuItem(
                text = type.toUiText().asString(),
                icon = type.icon(),
                onClick = { onTypeClick(type) },
                enabled = type.isAvailable,
                trailingText = comingSoon.takeUnless { type.isAvailable }
            )
        }
    )
}

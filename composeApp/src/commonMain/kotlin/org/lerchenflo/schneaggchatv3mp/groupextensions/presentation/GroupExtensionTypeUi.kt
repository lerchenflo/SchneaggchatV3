package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.graphics.vector.ImageVector
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.group_extension_type_quotes
import schneaggchatv3mp.composeapp.generated.resources.group_extension_type_route_planner
import schneaggchatv3mp.composeapp.generated.resources.group_extension_type_shared_text
import schneaggchatv3mp.composeapp.generated.resources.group_extension_type_split

fun GroupExtensionType.toUiText(): UiText = when (this) {
    GroupExtensionType.SHARED_TEXT -> UiText.StringResourceText(Res.string.group_extension_type_shared_text)
    GroupExtensionType.SPLIT -> UiText.StringResourceText(Res.string.group_extension_type_split)
    GroupExtensionType.QUOTES -> UiText.StringResourceText(Res.string.group_extension_type_quotes)
    GroupExtensionType.ROUTE_PLANNER -> UiText.StringResourceText(Res.string.group_extension_type_route_planner)
}

fun GroupExtensionType.icon(): ImageVector = when (this) {
    GroupExtensionType.SHARED_TEXT -> Icons.Default.TextFields
    GroupExtensionType.SPLIT -> Icons.Default.Money
    GroupExtensionType.QUOTES -> Icons.Default.FormatQuote
    GroupExtensionType.ROUTE_PLANNER -> Icons.Default.Route
}

package org.lerchenflo.schneaggchatv3mp.sharedUi.core

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.go_back

/**
 * One entry of a top bar menu ([TitleMenuButton], e.g. the ⋮ of [ActivityTitle]). Screens build
 * the list from their state, the menu draws it, so every top bar menu in the app looks the same.
 */
data class TitleMenuItem(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit = {},
    /** Red text and icon, for leaving and deleting. */
    val isDestructive: Boolean = false,
    val enabled: Boolean = true,
    /** Check mark at the end, for the picked option of a choice (e.g. a sort order). */
    val isSelected: Boolean = false,
    /** Small text at the end, e.g. "coming soon" on a disabled item. */
    val trailingText: String? = null,
    /**
     * A tap shows these in place of the menu, with a back row on top, instead of calling [onClick].
     * One level only: sub items' own [subItems] are not opened.
     */
    val subItems: List<TitleMenuItem> = emptyList(),
)

/** Icon button that opens [items] as a dropdown and closes it again after a pick. */
@Composable
fun TitleMenuButton(
    icon: ImageVector,
    contentDescription: String,
    items: List<TitleMenuItem>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    // Index in [items] of the entry whose sub items are shown, null for the top level
    var openedIndex by remember { mutableStateOf<Int?>(null) }

    fun close() {
        expanded = false
        openedIndex = null
    }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = ::close
        ) {
            val opened = openedIndex?.let(items::getOrNull)

            if (opened == null) {
                items.forEachIndexed { index, item ->
                    TitleMenuEntry(
                        item = item,
                        onClick = {
                            if (item.subItems.isNotEmpty()) {
                                openedIndex = index
                            } else {
                                close()
                                item.onClick()
                            }
                        }
                    )
                }
            } else {
                DropdownMenuItem(
                    text = { Text(text = opened.text, style = MaterialTheme.typography.titleSmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.go_back)
                        )
                    },
                    onClick = { openedIndex = null }
                )

                HorizontalDivider()

                opened.subItems.forEach { item ->
                    TitleMenuEntry(
                        item = item,
                        onClick = {
                            close()
                            item.onClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleMenuEntry(
    item: TitleMenuItem,
    onClick: () -> Unit,
) {
    val colors = if (item.isDestructive) {
        MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.error,
            leadingIconColor = MaterialTheme.colorScheme.error,
        )
    } else {
        MenuDefaults.itemColors()
    }

    DropdownMenuItem(
        text = { Text(item.text) },
        leadingIcon = { Icon(imageVector = item.icon, contentDescription = null) },
        trailingIcon = when {
            item.subItems.isNotEmpty() -> {
                { Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
            }
            item.isSelected -> {
                { Icon(imageVector = Icons.Default.Check, contentDescription = null) }
            }
            item.trailingText != null -> {
                { Text(text = item.trailingText, style = MaterialTheme.typography.labelSmall) }
            }
            else -> null
        },
        enabled = item.enabled,
        colors = colors,
        onClick = onClick
    )
}

package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapAction
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapState
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_map_style

@Composable
fun MapStyleDropdown(
    state: SchneaggmapState,
    onAction: (SchneaggmapAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        SmallFloatingActionButton(
            onClick = { onAction(SchneaggmapAction.ToggleMapStyleDropdown) },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Icon(Icons.Default.Layers, contentDescription = null)
        }

        DropdownMenu(
            expanded = state.isMapStyleDropdownVisible,
            onDismissRequest = { onAction(SchneaggmapAction.ToggleMapStyleDropdown) },
        ) {
            MapStyleDropdownContent(
                selectedStyle = state.mapStyle,
                onStyleClick = { onAction(SchneaggmapAction.SelectMapStyle(it)) },
            )
        }
    }
}

@Composable
fun MapStyleDropdownContent(
    selectedStyle: MapStyleSetting,
    onStyleClick: (MapStyleSetting) -> Unit,
) {
    Column(modifier = Modifier.width(180.dp)) {
        Text(
            text = stringResource(Res.string.schneaggmap_map_style),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        HorizontalDivider()

        MapStyleSetting.entries.forEach { style ->
            val selected = style == selectedStyle
            val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

            DropdownMenuItem(
                text = {
                    Text(
                        text = style.toUiText().asString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                    )
                },
                onClick = { onStyleClick(style) },
                leadingIcon = {
                    Icon(
                        imageVector = style.getIcon(),
                        contentDescription = null,
                        tint = color,
                    )
                },
            )
        }
    }
}
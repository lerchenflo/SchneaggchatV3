package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Card
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
        AnimatedVisibility(visible = state.isMapStyleDropdownVisible) {
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
    Card(modifier = Modifier.padding(top = 8.dp).width(180.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = stringResource(Res.string.schneaggmap_map_style),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            HorizontalDivider()

            MapStyleSetting.entries.forEach { style ->
                val selected = style == selectedStyle
                val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStyleClick(style) }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = style.getIcon(),
                        contentDescription = null,
                        tint = color,
                    )
                    Text(
                        text = style.toUiText().asString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

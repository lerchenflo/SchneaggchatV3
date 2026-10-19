package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationGroup
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.sortedTypes
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.stringRes
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapAction
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapState
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_filter_location_types

/**
 * The "Location types" filter menu. Anchors to its parent Box (the places chip in
 * [MapFilterChipsRow]).
 */
@Composable
fun ShownLocationsDropdown(
    state: SchneaggmapState,
    onAction: (SchneaggmapAction) -> Unit,
) {
    DropdownMenu(
        expanded = state.isFilterDropdownVisible,
        onDismissRequest = { onAction(SchneaggmapAction.ToggleFilterDropdown) },
    ) {
        LocationDropdownContent(
            onTypeClick = {
                onAction(SchneaggmapAction.ToggleMainType(it))
            },
            onGroupClick = {
                onAction(SchneaggmapAction.ToggleGroup(it))
            },
            onGroupExpandClick = {
                onAction(SchneaggmapAction.ToggleGroupExpanded(it))
            },
            enabledTypes = state.enabledTypes,
            expandedGroups = state.expandedFilterGroups,
        )
    }
}

@Composable
fun LocationDropdownContent(
    onTypeClick: (LocationType) -> Unit,
    onGroupClick: (LocationGroup) -> Unit,
    onGroupExpandClick: (LocationGroup) -> Unit,
    enabledTypes: Set<LocationType>,
    expandedGroups: Set<LocationGroup>
) {
    Column(modifier = Modifier.width(230.dp)) {
        Text(
            text = stringResource(Res.string.schneaggmap_filter_location_types),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        HorizontalDivider()

        LocationGroup.entries.forEach { group ->
            val expanded = group in expandedGroups
            val enabledCount = group.types.count { it in enabledTypes }

            //Expanded groups get a tinted card, a highlighted header and an accent line next to their types
            val sectionBackground by animateColorAsState(
                if (expanded) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent
            )
            val headerColor by animateColorAsState(
                if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            val accentColor = MaterialTheme.colorScheme.primary

            Column(
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(sectionBackground)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(group.stringRes()),
                            fontWeight = if (expanded) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = { onGroupExpandClick(group) },
                    leadingIcon = {
                        TriStateCheckbox(
                            state = when (enabledCount) {
                                0 -> ToggleableState.Off
                                group.types.size -> ToggleableState.On
                                else -> ToggleableState.Indeterminate
                            },
                            onClick = { onGroupClick(group) },
                        )
                    },
                    trailingIcon = {
                        val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f)
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.rotate(arrowRotation)
                        )
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = headerColor,
                        trailingIconColor = headerColor,
                    ),
                )

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .drawBehind {
                                val lineWidth = 3.dp.toPx()
                                drawRoundRect(
                                    color = accentColor,
                                    topLeft = Offset(16.dp.toPx(), 0f),
                                    size = Size(lineWidth, size.height),
                                    cornerRadius = CornerRadius(lineWidth / 2),
                                )
                            }
                    ) {
                        group.sortedTypes().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(stringResource(type.stringRes())) },
                                onClick = { onTypeClick(type) },
                                leadingIcon = {
                                    Checkbox(
                                        checked = type in enabledTypes,
                                        onCheckedChange = null,
                                    )
                                },
                                modifier = Modifier.padding(start = 20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
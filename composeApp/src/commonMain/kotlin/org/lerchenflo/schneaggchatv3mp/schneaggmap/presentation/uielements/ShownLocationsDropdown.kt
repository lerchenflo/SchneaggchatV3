package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationGroup
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.stringRes
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapAction
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapState
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.location_type_user
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_filter_location_types

@Composable
fun ShownLocationsDropdown(
    state: SchneaggmapState,
    onAction: (SchneaggmapAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        SmallFloatingActionButton(
            onClick = { onAction(SchneaggmapAction.ToggleFilterDropdown) },
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Icon(Icons.Default.FilterList, contentDescription = null)
            }
        }

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
                onToggleShowUsersClick = { onAction(SchneaggmapAction.ToggleShowUsers) },
                showUsers = state.showUsers,
            )
        }
    }
}

@Composable
fun LocationDropdownContent(
    onToggleShowUsersClick: () -> Unit,
    showUsers: Boolean,

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

        // Toggle users separately
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.location_type_user)) },
            onClick = onToggleShowUsersClick,
            leadingIcon = {
                Checkbox(
                    checked = showUsers,
                    onCheckedChange = null,
                )
            },
        )

        LocationGroup.entries.forEach { group ->
            val expanded = group in expandedGroups
            val enabledCount = group.types.count { it in enabledTypes }

            DropdownMenuItem(
                text = { Text(stringResource(group.stringRes())) },
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
            )

            AnimatedVisibility(visible = expanded) {
                Column {
                    group.types.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(stringResource(type.stringRes())) },
                            onClick = { onTypeClick(type) },
                            leadingIcon = {
                                Checkbox(
                                    checked = type in enabledTypes,
                                    onCheckedChange = null,
                                )
                            },
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
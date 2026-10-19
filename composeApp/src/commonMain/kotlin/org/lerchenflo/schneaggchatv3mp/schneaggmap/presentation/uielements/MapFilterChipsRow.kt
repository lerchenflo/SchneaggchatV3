package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lerchenflo.taptarget.tapTarget
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationGroup
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapAction
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapState
import org.lerchenflo.schneaggchatv3mp.sharedUi.horizontalScrollWithMouseWheel
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.location_type_user
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_chip_events
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_chip_places
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_chip_state_off
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_chip_state_on
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_filter_snail_trails

//Width kept free at the right edge for the zoom slider (32 dp handle + 8 dp edge padding), so the
//scrolling row never swallows drags meant for the slider on short/landscape screens.
private val ZOOM_SLIDER_CLEARANCE = 40.dp

/**
 * Quick filter chips below the map search bar. Every chip works on the same state as the matching
 * entry in the "Location types" dropdown, so both always stay in sync. The places chip doesn't
 * toggle anything, it opens that dropdown.
 */
@Composable
fun MapFilterChipsRow(
    state: SchneaggmapState,
    onAction: (SchneaggmapAction) -> Unit,
    modifier: Modifier = Modifier,
    endClearance: Dp = ZOOM_SLIDER_CLEARANCE,
) {
    val enabledGroupCount = LocationGroup.entries.count { group -> group.types.any { it in state.enabledTypes } }
    val placesVisible = state.enabledTypes.isNotEmpty()

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(end = endClearance)
            .horizontalScrollWithMouseWheel()
            .padding(horizontal = 16.dp)
    ) {
        MapFilterChip(
            label = stringResource(Res.string.location_type_user),
            icon = Icons.Outlined.Group,
            selected = state.showUsers,
            onClick = { onAction(SchneaggmapAction.ToggleShowUsers) },
        )

        //Box anchors the "Location types" dropdown below this chip
        Box(modifier = Modifier.tapTarget("schneaggmap_location_dropdown")) {
            MapFilterChip(
                label = stringResource(Res.string.schneaggmap_chip_places, enabledGroupCount, LocationGroup.entries.size),
                icon = Icons.Outlined.LocationOn,
                selected = placesVisible,
                showCheckWhenSelected = false,
                isLoading = state.isLoading,
                trailingIcon = Icons.Default.ArrowDropDown,
                onClick = {
                    if (!state.isFilterDropdownVisible) onAction(SchneaggmapAction.ToggleFilterDropdown)
                },
            )

            ShownLocationsDropdown(
                state = state,
                onAction = onAction,
            )
        }

        MapFilterChip(
            label = stringResource(Res.string.schneaggmap_chip_events),
            icon = Icons.Outlined.Event,
            selected = state.showEvents,
            onClick = { onAction(SchneaggmapAction.ToggleShowEvents) },
        )

        MapFilterChip(
            label = stringResource(Res.string.schneaggmap_filter_snail_trails),
            icon = Icons.Outlined.Route,
            selected = state.showSnailTrails,
            onClick = { onAction(SchneaggmapAction.ToggleSnailTrails) },
        )
    }
}

@Composable
private fun MapFilterChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    showCheckWhenSelected: Boolean = true,
    isLoading: Boolean = false,
    trailingIcon: ImageVector? = null,
) {
    val description = stringResource(
        if (selected) Res.string.schneaggmap_chip_state_on else Res.string.schneaggmap_chip_state_off,
        label
    )

    ElevatedFilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            //Never truncated - the row scrolls instead
            Text(text = label, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
        },
        leadingIcon = {
            if (isLoading) {
                //Map data still loading
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            } else {
                Icon(
                    imageVector = if (selected && showCheckWhenSelected) Icons.Default.Check else icon,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        },
        trailingIcon = trailingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        },
        modifier = Modifier.semantics { contentDescription = description }
    )
}

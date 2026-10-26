package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.SplitButtonShapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
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
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_filter_location_types
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_filter_snail_trails

/**
 * Quick filter chips below the map search bar. Every chip works on the same state as the matching
 * entry in the "Location types" dropdown, so both always stay in sync. Places use a split button:
 * its leading part shows or hides all places at once, its arrow opens that dropdown.
 */
@Composable
fun MapFilterChipsRow(
    state: SchneaggmapState,
    onAction: (SchneaggmapAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabledGroupCount = LocationGroup.entries.count { group -> group.types.any { it in state.enabledTypes } }
    val placesVisible = state.enabledTypes.isNotEmpty()

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScrollWithMouseWheel()
            .padding(horizontal = 16.dp)
    ) {
        MapFilterChip(
            label = stringResource(Res.string.location_type_user),
            icon = Icons.Outlined.Group,
            selected = state.showUsers,
            onClick = { onAction(SchneaggmapAction.ToggleShowUsers) },
        )

        //Box anchors the "Location types" dropdown below the split button
        Box(modifier = Modifier.tapTarget("schneaggmap_location_dropdown")) {
            PlacesSplitButton(
                label = stringResource(Res.string.schneaggmap_chip_places, enabledGroupCount, LocationGroup.entries.size),
                placesVisible = placesVisible,
                isLoading = state.isLoading,
                dropdownVisible = state.isFilterDropdownVisible,
                onToggleAllClick = { onAction(SchneaggmapAction.ToggleAllPlaces) },
                onDropdownClick = {
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

/**
 * Material 3 split button for places: the leading button shows or hides all places at once, the
 * trailing arrow opens the "Location types" dropdown for single types and stays checked while it is
 * open. Extra small size, so it is as high as the 32dp chips next to it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlacesSplitButton(
    label: String,
    placesVisible: Boolean,
    isLoading: Boolean,
    dropdownVisible: Boolean,
    onToggleAllClick: () -> Unit,
    onDropdownClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val height = SplitButtonDefaults.ExtraSmallContainerHeight

    //Same look as the filter chips: filled while places are shown, surface plus border otherwise
    val colors = ButtonDefaults.buttonColors(
        containerColor = if (placesVisible) colorScheme.primary else colorScheme.surface,
        contentColor = if (placesVisible) colorScheme.onPrimary else colorScheme.onSurface,
    )
    val border = if (placesVisible) null else BorderStroke(1.dp, colorScheme.outline)
    val elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)

    //Material's outer corners are fully round - use the chips' 8dp instead so it fits the row.
    //Inner corners and their pressed morph stay Material's extra small values.
    val outer = CornerSize(8.dp)
    val inner = SplitButtonDefaults.ExtraSmallInnerCornerSize
    val innerPressed = SplitButtonDefaults.ExtraSmallInnerCornerSizePressed
    val leadingShapes = SplitButtonShapes(
        shape = RoundedCornerShape(outer, inner, inner, outer),
        pressedShape = RoundedCornerShape(outer, innerPressed, innerPressed, outer),
        checkedShape = null,
    )
    val trailingShapes = SplitButtonShapes(
        shape = RoundedCornerShape(inner, outer, outer, inner),
        pressedShape = RoundedCornerShape(innerPressed, outer, outer, innerPressed),
        //Menu open: 8dp on all corners instead of Material's full pill
        checkedShape = RoundedCornerShape(outer),
    )

    val toggleDescription = stringResource(
        if (placesVisible) Res.string.schneaggmap_chip_state_on else Res.string.schneaggmap_chip_state_off,
        label
    )
    val dropdownDescription = stringResource(Res.string.schneaggmap_filter_location_types)

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = onToggleAllClick,
                shapes = leadingShapes,
                colors = colors,
                elevation = elevation,
                border = border,
                contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(height),
                modifier = Modifier
                    .heightIn(min = height)
                    .semantics { contentDescription = toggleDescription }
            ) {
                val iconSize = SplitButtonDefaults.leadingButtonIconSizeFor(height)

                if (isLoading) {
                    //Map data still loading - content color so it stays visible on the filled button
                    CircularProgressIndicator(
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(iconSize)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize)
                    )
                }

                Spacer(modifier = Modifier.width(ButtonDefaults.iconSpacingFor(height)))

                //Never truncated - the row scrolls instead
                Text(
                    text = label,
                    style = ButtonDefaults.textStyleFor(height),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible
                )
            }
        },
        trailingButton = {
            val arrowRotation by animateFloatAsState(if (dropdownVisible) 180f else 0f)

            SplitButtonDefaults.TrailingButton(
                checked = dropdownVisible,
                onCheckedChange = { onDropdownClick() },
                shapes = trailingShapes,
                colors = colors,
                elevation = elevation,
                border = border,
                contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(height),
                modifier = Modifier
                    .heightIn(min = height)
                    .semantics { contentDescription = dropdownDescription }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(SplitButtonDefaults.trailingButtonIconSizeFor(height))
                        .graphicsLayer { rotationZ = arrowRotation }
                )
            }
        }
    )
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

    //Chips float on the map, so the default chip roles (surfaceContainerLow / secondaryContainer)
    //vanish on light map tiles in some themes - use high-contrast roles plus a border instead
    val colorScheme = MaterialTheme.colorScheme

    ElevatedFilterChip(
        selected = selected,
        onClick = onClick,
        colors = FilterChipDefaults.elevatedFilterChipColors(
            containerColor = colorScheme.surface,
            labelColor = colorScheme.onSurface,
            iconColor = colorScheme.primary,
            selectedContainerColor = colorScheme.primary,
            selectedLabelColor = colorScheme.onPrimary,
            selectedLeadingIconColor = colorScheme.onPrimary,
            selectedTrailingIconColor = colorScheme.onPrimary,
        ),
        elevation = FilterChipDefaults.elevatedFilterChipElevation(elevation = 3.dp),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colorScheme.outline,
        ),
        label = {
            //Never truncated - the row scrolls instead
            Text(text = label, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
        },
        leadingIcon = {
            if (isLoading) {
                //Map data still loading - content color so it stays visible on the selected chip
                CircularProgressIndicator(
                    color = LocalContentColor.current,
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

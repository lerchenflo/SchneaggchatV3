package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.material3.DisappearingCompassButton
import org.maplibre.compose.material3.DisappearingScaleBar
import org.maplibre.compose.material3.ExpandingAttributionButton
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.StyleState
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.spatialk.geojson.Position
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_filter_location_types

@Composable
fun SchneaggmapScreenRoot(
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<SchneaggmapViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    SchneaggmapScreen(
        modifier = modifier,
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun SchneaggmapScreen(
    modifier: Modifier = Modifier,
    state: SchneaggmapState = SchneaggmapState(),
    onAction: (SchneaggmapAction) -> Unit = {},
) {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(9.3738, 48.2082),
            zoom = 12.0,
        )
    )
    val styleState = rememberStyleState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SchneaggmapMapContent(
                state = state,
                cameraState = cameraState,
                styleState = styleState,
                onAction = onAction,
            )

            MapIcon(
                cameraState = cameraState,
                targetPosition = Position(9.7333905, 47.4275590),
            ) {
                IconButton(
                    onClick = {}
                ) {
                    Icon(
                        Icons.Filled.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = Color.Red
                    )
                }
            }

            DisappearingCompassButton(
                cameraState = cameraState,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            )
            DisappearingScaleBar(
                metersPerDp = cameraState.metersPerDpAtTarget,
                zoom = cameraState.position.zoom,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp),
            )
            ExpandingAttributionButton(
                cameraState = cameraState,
                styleState = styleState,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
            )
            FilterPanel(
                state = state,
                onAction = onAction,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            )
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

        }
    }
}

@Composable
private fun SchneaggmapMapContent(
    state: SchneaggmapState,
    cameraState: CameraState,
    styleState: StyleState,
    onAction: (SchneaggmapAction) -> Unit,
) {

    MaplibreMap(
        modifier = Modifier.fillMaxSize(),
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        cameraState = cameraState,
        styleState = styleState,
        options = MapOptions(
            ornamentOptions = OrnamentOptions.AllDisabled
        )
    ) {

    }
}

@Composable
private fun EntryInfoCard(
    entry: MapEntry,
    mainTypeDisplayName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.width(220.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = mainTypeDisplayName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }
            if (entry.description.isNotBlank()) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            entry.attributes.entries.take(3).forEach { (key, value) ->
                val displayValue = when (value) {
                    is AttributeValue.StringVal -> value.value
                    is AttributeValue.IntVal -> value.value.toString()
                    is AttributeValue.DoubleVal -> value.value.toString()
                    is AttributeValue.BoolVal -> value.value.toString()
                }
                Text(
                    text = "$key: $displayValue",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun FilterPanel(
    state: SchneaggmapState,
    onAction: (SchneaggmapAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        FloatingActionButton(
            onClick = { onAction(SchneaggmapAction.ToggleFilterDropdown) },
        ) {
            Icon(Icons.Default.FilterList, contentDescription = null)
        }
        AnimatedVisibility(visible = state.isFilterDropdownVisible) {
            Card(modifier = Modifier.padding(top = 8.dp).width(200.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = stringResource(Res.string.schneaggmap_filter_location_types),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    HorizontalDivider()
                    state.mainTypes.forEach { mainType ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = mainType.key in state.enabledMainTypes,
                                onCheckedChange = { onAction(SchneaggmapAction.ToggleMainType(mainType.key)) },
                            )
                            Text(text = mainType.displayName)
                        }
                    }
                }
            }
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapAction
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapState
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_filter_location_types

@Composable
fun ShownLocationsDropdown(
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
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .padding(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.schneaggmap_filter_location_types),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    HorizontalDivider()
                    state.mainTypes.forEach { mainType ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth() //Max intrinsic size (for onclick listener)
                                .clickable{
                                    onAction(SchneaggmapAction.ToggleMainType(mainType.key))
                                }
                        ) {
                            Checkbox(
                                checked = mainType.key in state.enabledMainTypes,
                                onCheckedChange = null,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(text = mainType.displayName)
                        }
                    }
                }
            }
        }
    }
}

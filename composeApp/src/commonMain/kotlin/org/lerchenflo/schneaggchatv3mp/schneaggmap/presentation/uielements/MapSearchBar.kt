package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.drawableRes
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapAction
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapState
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_search_label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSearchBar(
    state: SchneaggmapState,
    onAction: (SchneaggmapAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchbarstate = rememberSearchBarState()
    val textfieldstate = rememberTextFieldState(initialText = state.searchTerm)
    val scope = rememberCoroutineScope()

    // Push every keystroke into the ViewModel for live filtering
    LaunchedEffect(textfieldstate) {
        snapshotFlow { textfieldstate.text.toString() }
            .collect { text ->
                if (text != state.searchTerm) {
                    onAction(SchneaggmapAction.OnSearchTermChange(text))
                }
            }
    }

    // Keep the field in sync if searchTerm changes externally (e.g. the clear button below)
    LaunchedEffect(state.searchTerm) {
        if (state.searchTerm != textfieldstate.text.toString()) {
            textfieldstate.setTextAndPlaceCursorAtEnd(state.searchTerm)
        }
    }

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            searchBarState = searchbarstate,
            textFieldState = textfieldstate,
            onSearch = {
                onAction(SchneaggmapAction.OnSearchTermChange(it))
                scope.launch { searchbarstate.animateToCollapsed() }
            },
            placeholder = {
                Text(
                    text = stringResource(Res.string.schneaggmap_search_label)
                )
            },
            leadingIcon = {

                if (state.searchTerm.isNotEmpty() or (searchbarstate.currentValue == SearchBarValue.Expanded)) {
                    //User is searching, show back icon from google maps for ios users to go back
                    IconButton(onClick = {
                        onAction(SchneaggmapAction.OnSearchTermChange(""))
                        scope.launch { searchbarstate.animateToCollapsed() }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else null


            },
            trailingIcon = {
                //Clear search
                if (state.searchTerm.isNotEmpty()) {
                    IconButton(onClick = { onAction(SchneaggmapAction.OnSearchTermChange("")) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
    }

    SearchBar(
        state = searchbarstate,
        inputField = inputField,
        modifier = modifier,
    )

    ExpandedFullScreenSearchBar(
        state = searchbarstate,
        inputField = inputField,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            state.searchResults.forEach { entry ->

                MapSearchResult(
                    entry = entry,
                    modifier = Modifier.clickable {
                        onAction(SchneaggmapAction.OnSearchResultClick(entry))
                        scope.launch { searchbarstate.animateToCollapsed() }
                    }
                )
            }
        }
    }


}

@Composable
fun MapSearchResult(
    entry: MapEntry,
    modifier: Modifier
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(entry.name) },
        leadingContent = {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                entry.locationData.forEach { data ->
                    Image(
                        painter = painterResource(data.locationtype.drawableRes()),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )

}
package org.lerchenflo.schneaggchatv3mp.sharedUi.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatformTools
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.MapRepository

/**
 * Default [ComboAnnotationSource]s, self-contained: names are looked up through the injected
 * repositories and clicks navigate via the [Navigator] singleton, so callers of [ComboText] /
 * [ComboInputField] don't need to provide anything.
 *
 * Extend this list when adding new annotation types (e.g. user mentions).
 */
@Composable
fun rememberComboAnnotationSources(): List<ComboAnnotationSource> {
    // getOrNull keeps @Previews (no Koin context) working — they just render raw text
    val koin = KoinPlatformTools.defaultContext().getOrNull() ?: return emptyList()
    val mapRepository = remember(koin) { koin.get<MapRepository>() }
    val navigator = remember(koin) { koin.get<Navigator>() }
    val scope = rememberCoroutineScope()

    val locationNames by remember(mapRepository) {
        mapRepository.getAllMapEntriesFlow()
            .map { entries -> entries.associate { it.id to it.name } }
    }.collectAsState(initial = emptyMap())

    return remember(locationNames) {
        listOf(
            ComboAnnotationSource(
                type = ComboAnnotationTypes.MAP_LOCATION,
                names = locationNames,
                onClick = { entryId ->
                    scope.launch { navigator.navigate(Route.Schneaggmap(initialEntryId = entryId)) }
                }
            )
        )
    }
}

package org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationGroup
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType

/**
 * What the shared map layers show, toggled by [MapFilterChipsRow]. Each map screen keeps its own
 * copy (not saved): place types start empty, sightings, events and friends start on.
 */
data class MapLayersDisplay(
    val enabledTypes: Set<LocationType> = emptySet(),
    val expandedGroups: Set<LocationGroup> = emptySet(),
    val isPlacesDropdownVisible: Boolean = false,
    // Police and mobile radars with a fresh sighting show even while their type is not enabled
    val showActiveSightings: Boolean = true,
    val showEvents: Boolean = true,
    val showUsers: Boolean = true,
)

sealed interface MapLayersAction {
    data object ToggleActiveSightings : MapLayersAction
    data object ToggleEvents : MapLayersAction
    data object ToggleUsers : MapLayersAction
    // Leading part of the places split button: hides all shown places, or shows every type when none are shown
    data object ToggleAllPlaces : MapLayersAction
    data object TogglePlacesDropdown : MapLayersAction
    data class ToggleType(val type: LocationType) : MapLayersAction
    data class ToggleGroup(val group: LocationGroup) : MapLayersAction
    data class ToggleGroupExpanded(val group: LocationGroup) : MapLayersAction
}

fun MapLayersDisplay.reduce(action: MapLayersAction): MapLayersDisplay = when (action) {
    MapLayersAction.ToggleActiveSightings -> copy(showActiveSightings = !showActiveSightings)
    MapLayersAction.ToggleEvents -> copy(showEvents = !showEvents)
    MapLayersAction.ToggleUsers -> copy(showUsers = !showUsers)
    MapLayersAction.ToggleAllPlaces -> {
        // Anything shown (even only some types): hide all. Nothing shown: show every type
        val allPlaceTypes = LocationGroup.entries.flatMapTo(mutableSetOf()) { it.types }
        copy(enabledTypes = if (enabledTypes.isNotEmpty()) emptySet() else allPlaceTypes)
    }
    MapLayersAction.TogglePlacesDropdown -> copy(isPlacesDropdownVisible = !isPlacesDropdownVisible)
    is MapLayersAction.ToggleType -> copy(enabledTypes = enabledTypes.toggle(action.type))
    is MapLayersAction.ToggleGroup -> copy(
        enabledTypes = if (enabledTypes.containsAll(action.group.types)) {
            enabledTypes - action.group.types.toSet()
        } else {
            enabledTypes + action.group.types
        }
    )
    is MapLayersAction.ToggleGroupExpanded -> copy(expandedGroups = expandedGroups.toggle(action.group))
}

/** Makes sure [types] are shown, e.g. for an entry the map was opened on. */
fun MapLayersDisplay.showingTypes(types: Collection<LocationType>): MapLayersDisplay =
    copy(enabledTypes = enabledTypes + types)

fun MapLayersDisplay.withPlacesDropdownClosed(): MapLayersDisplay =
    copy(isPlacesDropdownVisible = false)

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

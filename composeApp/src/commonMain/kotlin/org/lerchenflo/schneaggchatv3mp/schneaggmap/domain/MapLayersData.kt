package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.events.domain.Event

/**
 * Everything a map with the shared layers draws besides its own content: places, friends, events
 * and the own user. Loaded by [MapLayersSource], the same for every map screen.
 */
data class MapLayersData(
    val entries: List<MapEntry> = emptyList(),
    // Police and mobile radars with a fresh sighting show even while their type is not enabled.
    // Entry id to the types with an active sighting, only entries that have one
    val activeSightings: Map<String, Set<LocationType>> = emptyMap(),
    // Friends whose last location is recent enough to show
    val usersWithLocation: List<User> = emptyList(),
    val onlineFriendIds: Set<String> = emptySet(),
    val ownUser: User? = null,
    // Events with a location, without the ones the own user marked as not interested
    val eventsWithLocation: List<Event> = emptyList(),
    // Events the own user is going to (accepted, joined or created) - their pins use the primary color
    val joinedEventIds: Set<String> = emptySet(),
    // Settings "merge map locations" (cluster places) and "merge map users" (hocks)
    val useClustering: Boolean = false,
    val mergeUsers: Boolean = true,
)

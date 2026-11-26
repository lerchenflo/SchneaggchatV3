@file:OptIn(ExperimentalCoroutinesApi::class)

package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.events.domain.EventRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.statusOf
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * Loads the shared map layers ([MapLayersData]) for every map screen (Schneaggmap, route planner),
 * so each map shows the same places, friends and events. [ownUserIdFlow] is the own user id while
 * logged in (injected, it has to follow autologin).
 */
class MapLayersSource(
    private val mapRepository: MapRepository,
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository,
    private val settingsRepository: SettingsRepository,
    private val ownUserIdFlow: Flow<String?>,
) {

    fun observe(): Flow<MapLayersData> {
        val entriesFlow = mapRepository.getAllMapEntriesFlow()
        return combine(
            entriesFlow,
            activeSightingsFlow(entriesFlow),
            peopleFlow(),
            eventsFlow(),
            settingsFlow(),
        ) { entries, activeSightings, people, events, settings ->
            MapLayersData(
                entries = entries,
                activeSightings = activeSightings,
                usersWithLocation = people.usersWithLocation,
                onlineFriendIds = people.onlineFriendIds,
                ownUserId = people.ownUserId,
                ownUser = people.ownUser,
                eventsWithLocation = events.withLocation,
                joinedEventIds = events.joinedIds,
                useClustering = settings.useClustering,
                mergeUsers = settings.mergeUsers,
            )
        }
    }

    // Checked every minute too: a sighting runs out while the map stays open
    private fun activeSightingsFlow(entriesFlow: Flow<List<MapEntry>>): Flow<Map<String, Set<LocationType>>> {
        val everyMinute = flow {
            while (true) {
                emit(Unit)
                delay(1.minutes)
            }
        }
        return combine(entriesFlow, everyMinute) { entries, _ ->
            val now = Clock.System.now()
            val timeZone = TimeZone.currentSystemDefault()
            entries.associate { it.id to it.activeSightingTypes(now, timeZone) }
                .filterValues { it.isNotEmpty() }
        }.distinctUntilChanged()
    }

    private data class People(
        val usersWithLocation: List<User>,
        val onlineFriendIds: Set<String>,
        val ownUserId: String?,
        val ownUser: User?,
    )

    private fun peopleFlow(): Flow<People> = combine(
        userRepository.getFriendsFlow("").map { friends -> friends.filter { it.isLocationValid() } },
        userRepository.onlineFriendIdsFlow,
        ownUserIdFlow.flatMapLatest { ownId ->
            if (ownId == null) flowOf(null to null) else userRepository.getUserByIdFlow(ownId).map { ownId to it }
        },
    ) { usersWithLocation, onlineFriendIds, (ownUserId, ownUser) ->
        People(usersWithLocation, onlineFriendIds, ownUserId, ownUser)
    }

    private data class Events(val withLocation: List<Event>, val joinedIds: Set<String>)

    private fun eventsFlow(): Flow<Events> = combine(
        eventRepository.getAllEventsFlow(),
        ownUserIdFlow,
    ) { events, ownUserId ->
        // Events the user marked as not interested are hidden from the map
        val withLocation = events.filter { event ->
            event.location != null &&
                (ownUserId == null || event.statusOf(ownUserId) != EventParticipationStatus.DISMISSED)
        }
        val joinedIds = ownUserId?.let { id ->
            withLocation
                .filter { it.statusOf(id) == EventParticipationStatus.ACCEPTED }
                .mapTo(mutableSetOf()) { it.id }
        } ?: emptySet()
        Events(withLocation, joinedIds)
    }

    private data class LayerSettings(val useClustering: Boolean, val mergeUsers: Boolean)

    private fun settingsFlow(): Flow<LayerSettings> = combine(
        settingsRepository.getMergeMapLocationsFlow(),
        settingsRepository.getMergeMapUsersFlow(),
    ) { useClustering, mergeUsers -> LayerSettings(useClustering, mergeUsers) }
}

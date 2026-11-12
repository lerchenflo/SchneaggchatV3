package org.lerchenflo.schneaggchatv3mp.sharedUi.text

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.EventRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapRepository

/** How long the tables stay collected after the last text using them is gone (e.g. rotation). */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Names the inline annotations of [ComboText] resolve to (entity id -> name), kept once for the
 * whole app. Every text bubble and chat row reads them, so collecting the tables per text would
 * rebuild every visible text on each write - the users table changes with every presence update.
 * Each map only emits when a name actually changes.
 */
class ComboAnnotationNames(
    mapRepository: MapRepository,
    userRepository: UserRepository,
    eventRepository: EventRepository,
    applicationScope: ApplicationScope,
) {
    val locationNames: StateFlow<Map<String, String>> = mapRepository.getAllMapEntriesFlow()
        .map { entries -> entries.associate { it.id to it.name } }
        .shareNames(applicationScope)

    /** The nickname where one is set, like everywhere else in the chat. */
    val userNames: StateFlow<Map<String, String>> = userRepository.getAllUsersFlow()
        .map { users -> users.associate { it.id to it.displayName } }
        .shareNames(applicationScope)

    val eventNames: StateFlow<Map<String, String>> = eventRepository.getAllEventsFlow()
        .map { events -> events.associate { it.id to it.title } }
        .shareNames(applicationScope)

    private fun Flow<Map<String, String>>.shareNames(scope: ApplicationScope): StateFlow<Map<String, String>> =
        distinctUntilChanged()
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())
}

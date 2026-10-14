package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.events.domain.EventRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.events.domain.EventVisibility
import org.lerchenflo.schneaggchatv3mp.events.domain.GroupDeleteDelay
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong

/**
 * In-memory [EventRepository]. The reads serve [events] (the connected event of a group comes
 * from [eventsByGroupId]); the writes are recorded and answer with [writeResult] /
 * [joinedGroupId].
 */
class FakeEventRepository : EventRepository {

    data class DetachCall(val eventId: String, val groupId: String?, val deleteGroup: Boolean, val deleteEvent: Boolean)

    val events = MutableStateFlow<List<Event>>(emptyList())

    /** The connected event per group id, served by [getEventFlowForGroup]. */
    val eventsByGroupId = MutableStateFlow<Map<String, Event>>(emptyMap())

    var writeResult: Boolean = true
    var joinedGroupId: String? = "event-group"

    var onSyncEvents: suspend () -> Unit = {}

    val syncCalls = CallLog<Unit>()
    val upsertedEventIds = CallLog<String?>()
    val participationChanges = CallLog<Pair<String, EventParticipationStatus>>()
    val joinedEvents = CallLog<String>()
    val detachCalls = CallLog<DetachCall>()

    override fun getAllEventsFlow(): Flow<List<Event>> = events

    override fun getEventFlowForGroup(groupId: String): Flow<Event?> = eventsByGroupId.map { it[groupId] }

    override suspend fun syncEvents() {
        syncCalls.record(Unit)
        onSyncEvents()
    }

    override suspend fun upsertEvent(
        eventId: String?,
        type: EventType,
        title: String,
        description: String,
        createGroup: Boolean,
        location: LatLong?,
        startDate: Long,
        closeDate: Long?,
        invitedUsers: List<String>,
        visibility: EventVisibility,
        maxUsers: Int?,
        groupDeleteDelay: GroupDeleteDelay,
        profilePic: ByteArray?,
    ): Boolean {
        upsertedEventIds.record(eventId)
        return writeResult
    }

    override suspend fun setEventParticipation(eventId: String, status: EventParticipationStatus): Boolean {
        participationChanges.record(eventId to status)
        return writeResult
    }

    override suspend fun joinEvent(eventId: String): String? {
        joinedEvents.record(eventId)
        return joinedGroupId
    }

    override suspend fun detachEvent(eventId: String, groupId: String?, deleteGroup: Boolean, deleteEvent: Boolean): Boolean {
        detachCalls.record(DetachCall(eventId, groupId, deleteGroup, deleteEvent))
        return writeResult
    }
}

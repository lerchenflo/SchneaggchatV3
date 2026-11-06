package org.lerchenflo.schneaggchatv3mp.events.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong

/**
 * Events: reads come from the local database, writes go to the server first and update the local
 * row with the server's answer. Write calls return their error, the caller maps it to UI.
 */
interface EventRepository {

    fun getAllEventsFlow(): Flow<List<Event>>

    /** The event connected to the group [groupId], null if the group has none. */
    fun getEventFlowForGroup(groupId: String): Flow<Event?>

    /** Pulls changed and deleted events from the server into the local database. */
    suspend fun syncEvents()

    /** Creates ([eventId] null) or updates an event. */
    suspend fun upsertEvent(
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
        profilePic: ByteArray? = null,
    ): EmptyDataResult<DataError>

    /**
     * Records how the current user responded to an event (opened it, accepted or dismissed it).
     */
    suspend fun setEventParticipation(eventId: String, status: EventParticipationStatus): EmptyDataResult<DataError>

    /**
     * Joins the event and stores the group belonging to it.
     * @return The id of the group belonging to the event
     */
    suspend fun joinEvent(eventId: String): DataResult<String, DataError>

    /**
     * Breaks the event <-> group link, optionally deleting the group and/or the event.
     * [groupId] only matters when [deleteGroup] is true.
     */
    suspend fun detachEvent(
        eventId: String,
        groupId: String? = null,
        deleteGroup: Boolean = false,
        deleteEvent: Boolean = false,
    ): EmptyDataResult<DataError>
}

package org.lerchenflo.schneaggchatv3mp.events.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel

/**
 * Events: reads come from the local database, writes go to the server first and update the local
 * row with the server's answer. Errors of the write calls are sent to the [ErrorChannel].
 */
interface EventRepository {

    fun getAllEventsFlow(): Flow<List<Event>>

    /** The event connected to the group [groupId], null if the group has none. */
    fun getEventFlowForGroup(groupId: String): Flow<Event?>

    /** Pulls changed and deleted events from the server into the local database. */
    suspend fun syncEvents()

    /** Creates ([eventId] null) or updates an event. @return true on success */
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
    ): Boolean

    /**
     * Records how the current user responded to an event (opened it, accepted or dismissed it).
     * @return true on success
     */
    suspend fun setEventParticipation(eventId: String, status: EventParticipationStatus): Boolean

    /**
     * Joins the event and stores the group belonging to it.
     * @return The id of the group belonging to the event, null on error
     */
    suspend fun joinEvent(eventId: String): String?

    /**
     * Breaks the event <-> group link, optionally deleting the group and/or the event.
     * [groupId] only matters when [deleteGroup] is true.
     * @return true on success
     */
    suspend fun detachEvent(
        eventId: String,
        groupId: String? = null,
        deleteGroup: Boolean = false,
        deleteEvent: Boolean = false,
    ): Boolean
}

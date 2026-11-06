package org.lerchenflo.schneaggchatv3mp.events.data

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.asEmptyDataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.map
import org.lerchenflo.schneaggchatv3mp.app.domain.onSuccess
import org.lerchenflo.schneaggchatv3mp.chat.data.local.GroupLocalDataSource
import org.lerchenflo.schneaggchatv3mp.events.data.local.EventLocalDataSource
import org.lerchenflo.schneaggchatv3mp.events.domain.EventRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.events.domain.EventVisibility
import org.lerchenflo.schneaggchatv3mp.events.domain.GroupDeleteDelay
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.events.data.remote.dto.EventRequest
import org.lerchenflo.schneaggchatv3mp.events.data.remote.dto.toEvent
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.events.data.remote.EventRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.toGroup

/**
 * [EventRepository] on top of the local [EventLocalDataSource] and the server via [EventRemoteDataSource].
 * [joinEvent] and [detachEvent] also write the event's group through [GroupLocalDataSource].
 */
class OfflineFirstEventRepository(
    private val eventLocalDataSource: EventLocalDataSource,
    private val groupLocalDataSource: GroupLocalDataSource,
    private val eventRemoteDataSource: EventRemoteDataSource,
    private val loggingRepository: Logger,
) : EventRepository {

    override fun getAllEventsFlow(): Flow<List<Event>> =
        eventLocalDataSource.getAllEventsFlow()

    override fun getEventFlowForGroup(groupId: String): Flow<Event?> =
        eventLocalDataSource.getEventFlowForGroup(groupId)

    // ─── Event sync ───────────────────────────────────────────────────────────

    override suspend fun syncEvents() {
        SessionCache.requireLoggedIn() ?: return

        val localEntries = eventLocalDataSource.getEventChangeIds()
        var currentPage = 0
        var moreEntries = true
        while (moreEntries) {
            val response = eventRemoteDataSource.eventSync(
                entries = localEntries.map { IdTimeStamp(it.id, it.updatedAt) },
                page = currentPage,
            ).trackConnectivity()
            when (response) {
                is DataResult.Error<*> -> { loggingRepository.logWarning("Event sync error: ${response.error}"); break }
                is DataResult.Success -> {
                    response.data.updatedEvents.forEach { eventLocalDataSource.upsertEvent(it.toEvent()) }
                    response.data.deletedEvents.forEach { eventLocalDataSource.deleteEvent(it) }
                    moreEntries = response.data.moreEntries
                }
            }
            currentPage++
        }
    }

    // ─── Event CRUD ───────────────────────────────────────────────────────────
    // WS broadcast handles local DB upsert/delete automatically after server write.

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
    ): EmptyDataResult<DataError> =
        eventRemoteDataSource.upsertEvent(
            EventRequest(
                eventId = eventId,
                type = type,
                title = title,
                description = description,
                location = location,
                startDate = startDate,
                closeDate = closeDate,
                invitedUsers = invitedUsers,
                visibility = visibility,
                maxUsers = maxUsers,
                groupDeleteDelay = groupDeleteDelay,
                createGroup = createGroup,
            ),
            profilePic = profilePic,
        ).trackConnectivity()
            .onSuccess { eventLocalDataSource.upsertEvent(it.toEvent()) }
            .asEmptyDataResult()

    /**
     * Records how the current user responded to an event (opened it, accepted or dismissed it).
     * The server answers with the full event, so the local row is fresh before the matching
     * EventChange push even arrives.
     */
    override suspend fun setEventParticipation(eventId: String, status: EventParticipationStatus): EmptyDataResult<DataError> =
        eventRemoteDataSource.setEventParticipation(eventId, status).trackConnectivity()
            .onSuccess {
                eventLocalDataSource.upsertEvent(it.toEvent())
            }
            .asEmptyDataResult()

    /**
     * Joins the event, upserts the group belonging to the event and returns the group id
     * @return The id of the group belonging to the event
     */
    override suspend fun joinEvent(eventId: String): DataResult<String, DataError> =
        eventRemoteDataSource.joinEvent(eventId).trackConnectivity()
            .onSuccess { response ->
                // The joiner is not necessarily in the event's push audience (a stranger joining a
                // public event is neither invited nor a friend of the creator), so their own
                // ACCEPTED would otherwise only land on the next full sync
                response.event?.let { eventLocalDataSource.upsertEvent(it.toEvent()) }

                groupLocalDataSource.upsertGroup(response.groupResponse.toGroup())
            }
            .map { it.groupResponse.id }

    /**
     * Breaks the event <-> group link. Both flags false detaches only - the event and its group
     * both survive, and the local DB catches up via the `eventchange` WS broadcast. [groupId] only
     * matters when [deleteGroup] is true - the local group delete here is just an optimistic fast
     * path, the server's own group deletion + notifyGroupUpdate / group sync converge on the same
     * result regardless.
     */
    override suspend fun detachEvent(eventId: String, groupId: String?, deleteGroup: Boolean, deleteEvent: Boolean): EmptyDataResult<DataError> =
        eventRemoteDataSource.detachEvent(eventId, deleteGroup, deleteEvent).trackConnectivity()
            .onSuccess {
                if (deleteEvent) {
                    eventLocalDataSource.deleteEvent(eventId)
                }
                if (deleteGroup && groupId != null) {
                    groupLocalDataSource.deleteGroup(groupId)
                }
            }
            .asEmptyDataResult()
}

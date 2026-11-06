package org.lerchenflo.schneaggchatv3mp.events.data.remote

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.events.data.remote.dto.EventJoinResponse
import org.lerchenflo.schneaggchatv3mp.events.data.remote.dto.EventRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.EventResponse
import org.lerchenflo.schneaggchatv3mp.events.data.remote.dto.EventSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus

/** Server calls for events: sync, upsert, join, participation and detach/delete. */
interface EventRemoteDataSource {

    suspend fun eventSync(
        entries: List<IdTimeStamp>,
        page: Int,
    ): DataResult<EventSyncResponse, DataError>

    suspend fun upsertEvent(
        request: EventRequest,
        profilePic: ByteArray? = null,
    ): DataResult<EventResponse, DataError>

    suspend fun joinEvent(
        eventId: String
    ): DataResult<EventJoinResponse, DataError>

    suspend fun setEventParticipation(
        eventId: String,
        status: EventParticipationStatus,
    ): DataResult<EventResponse, DataError>

    suspend fun detachEvent(eventId: String, deleteGroup: Boolean = false, deleteEvent: Boolean = false): DataResult<Unit, DataError>
}

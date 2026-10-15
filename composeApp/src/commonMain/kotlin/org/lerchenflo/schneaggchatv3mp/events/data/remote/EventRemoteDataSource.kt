package org.lerchenflo.schneaggchatv3mp.events.data.remote

import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventJoinResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus

/** Server calls for events: sync, upsert, join, participation and detach/delete. */
interface EventRemoteDataSource {

    suspend fun eventSync(
        entries: List<IdTimeStamp>,
        page: Int,
    ): NetworkResult<EventSyncResponse, NetworkingError>

    suspend fun upsertEvent(
        request: EventRequest,
        profilePic: ByteArray? = null,
    ): NetworkResult<EventResponse, NetworkingError>

    suspend fun joinEvent(
        eventId: String
    ): NetworkResult<EventJoinResponse, NetworkingError>

    suspend fun setEventParticipation(
        eventId: String,
        status: EventParticipationStatus,
    ): NetworkResult<EventResponse, NetworkingError>

    suspend fun detachEvent(eventId: String, deleteGroup: Boolean = false, deleteEvent: Boolean = false): NetworkResult<Unit, NetworkingError>
}

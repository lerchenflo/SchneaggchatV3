package org.lerchenflo.schneaggchatv3mp.events.data.remote

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.appendJsonPart
import org.lerchenflo.schneaggchatv3mp.datasource.network.appendProfilePic
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventJoinRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventJoinResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventParticipationRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.EventSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus

/** [EventRemoteDataSource] over Ktor, against the server URL from [Preferencemanager]. */
class KtorEventRemoteDataSource(
    httpClient: HttpClient,
    preferenceManager: Preferencemanager,
    loggingRepository: Logger,
) : EventRemoteDataSource {

    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    override suspend fun eventSync(
        entries: List<IdTimeStamp>,
        page: Int,
    ): NetworkResult<EventSyncResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/events/sync?page=$page&page_size=400",
            body = entries,
        )
    }

    override suspend fun upsertEvent(
        request: EventRequest,
        profilePic: ByteArray?,
    ): NetworkResult<EventResponse, NetworkingError> {
        return server.safePostMultipart(
            endpoint = "/events/upsert",
            parts = {
                appendJsonPart(key = "request", serializer = EventRequest.serializer(), value = request)
                profilePic?.let {
                    appendProfilePic(it, fileName = "eventtype.png", contentType = "image/png")
                }
            }
        )
    }

    override suspend fun joinEvent(
        eventId: String
    ): NetworkResult<EventJoinResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/events/join",
            body = EventJoinRequest(
                eventId = eventId
            ),
        )
    }

    override suspend fun setEventParticipation(
        eventId: String,
        status: EventParticipationStatus,
    ): NetworkResult<EventResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/events/participation",
            body = EventParticipationRequest(
                eventId = eventId,
                status = status,
            )
        )
    }

    override suspend fun detachEvent(eventId: String, deleteGroup: Boolean, deleteEvent: Boolean): NetworkResult<Unit, NetworkingError> {
        return server.safeDelete(endpoint = "/events/delete?eventid=$eventId&deletegroup=$deleteGroup&deleteevent=$deleteEvent")
    }
}

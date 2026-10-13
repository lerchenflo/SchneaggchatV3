package org.lerchenflo.schneaggchatv3mp.events.data

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.Event

/**
 * Local (Room) storage for events.
 */
interface EventLocalDataSource {

    suspend fun getEventChangeIds(): List<IdChangeDate>

    suspend fun upsertEvent(event: Event)

    suspend fun deleteEvent(id: String)

    fun getAllEventsFlow(): Flow<List<Event>>

    fun getEventFlowForGroup(groupId: String): Flow<Event?>
}

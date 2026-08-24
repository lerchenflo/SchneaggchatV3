package org.lerchenflo.schneaggchatv3mp.events.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toDto
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toEvent
import org.lerchenflo.schneaggchatv3mp.events.domain.Event

class EventRepository(
    private val database: AppDatabase,
) {

    // ─── Events ───────────────────────────────────────────────────────────────

    suspend fun getEventChangeIds(): List<IdChangeDate> =
        database.eventDao().getEventIdsWithChangeDates()

    suspend fun upsertEvent(event: Event) =
        database.eventDao().upsert(event.toDto())

    suspend fun deleteEvent(id: String) =
        database.eventDao().delete(id)

    fun getAllEventsFlow(): Flow<List<Event>> =
        database.eventDao().getAllFlow().map { list -> list.map { it.toEvent() } }

    fun getEventFlowForGroup(groupId: String): Flow<Event?> =
        database.eventDao().getEventFlowByGroupId(groupId).map { it?.toEvent() }
}

package org.lerchenflo.schneaggchatv3mp.events.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.events.data.remote.dto.toDto
import org.lerchenflo.schneaggchatv3mp.events.data.remote.dto.toEvent
import org.lerchenflo.schneaggchatv3mp.events.domain.Event

class RoomEventDataSource(
    private val database: AppDatabase,
) : EventLocalDataSource {

    // ─── Events ───────────────────────────────────────────────────────────────

    override suspend fun getEventChangeIds(): List<IdChangeDate> =
        database.eventDao().getEventIdsWithChangeDates()

    override suspend fun upsertEvent(event: Event) =
        database.eventDao().upsert(event.toDto())

    override suspend fun deleteEvent(id: String) =
        database.eventDao().delete(id)

    override fun getAllEventsFlow(): Flow<List<Event>> =
        database.eventDao().getAllFlow().map { list -> list.map { it.toEvent() } }

    override fun getEventFlowForGroup(groupId: String): Flow<Event?> =
        database.eventDao().getEventFlowByGroupId(groupId).map { it?.toEvent() }
}

package org.lerchenflo.schneaggchatv3mp.feedback.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.feedback.data.local.entity.toEntity
import org.lerchenflo.schneaggchatv3mp.feedback.data.local.entity.toFeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry

class RoomFeedbackDataSource(
    private val database: AppDatabase,
) : FeedbackLocalDataSource {

    override suspend fun getFeedbackEntryChangeIds(): List<IdChangeDate> =
        database.feedbackEntryDao().getFeedbackEntryIdsWithChangeDates()

    override suspend fun upsertFeedbackEntries(entries: List<FeedbackEntry>) {
        if (entries.isEmpty()) return
        database.feedbackEntryDao().upsertAll(entries.map { it.toEntity() })
    }

    override suspend fun deleteFeedbackEntries(ids: List<String>) {
        if (ids.isEmpty()) return
        database.feedbackEntryDao().deleteByIds(ids)
    }

    override fun getAllFeedbackEntriesFlow(): Flow<List<FeedbackEntry>> =
        database.feedbackEntryDao().getAllFlow().map { list -> list.mapNotNull { it.toFeedbackEntry() } }
}

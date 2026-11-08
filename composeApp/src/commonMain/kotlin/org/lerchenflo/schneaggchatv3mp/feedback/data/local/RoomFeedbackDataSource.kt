package org.lerchenflo.schneaggchatv3mp.feedback.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.feedback.data.local.entity.FeedbackCommentSeenEntity
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
        val dao = database.feedbackEntryDao()
        dao.upsertAll(entries.map { it.toEntity() })
        // Old comments of an entry the device sees the first time don't count as new
        val baselines = entries.filter { it.isOwn }.map { FeedbackCommentSeenEntity(it.id, it.commentCount) }
        if (baselines.isNotEmpty()) dao.insertSeenCommentCountsIfMissing(baselines)
    }

    override suspend fun deleteFeedbackEntries(ids: List<String>) {
        if (ids.isEmpty()) return
        database.feedbackEntryDao().deleteByIds(ids)
        database.feedbackEntryDao().deleteSeenCommentCounts(ids)
    }

    override fun getAllFeedbackEntriesFlow(): Flow<List<FeedbackEntry>> =
        database.feedbackEntryDao().getAllFlow().map { list -> list.mapNotNull { it.toFeedbackEntry() } }

    override fun getSeenCommentCountsFlow(): Flow<Map<String, Int>> =
        database.feedbackEntryDao().getSeenCommentCountsFlow()
            .map { rows -> rows.associate { it.entryId to it.seenCommentCount } }

    override suspend fun setSeenCommentCount(entryId: String, commentCount: Int) =
        database.feedbackEntryDao().upsertSeenCommentCount(FeedbackCommentSeenEntity(entryId, commentCount))

    override suspend fun incrementSeenCommentCount(entryId: String) =
        database.feedbackEntryDao().incrementSeenCommentCount(entryId)
}

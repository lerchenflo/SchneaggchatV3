package org.lerchenflo.schneaggchatv3mp.feedback.data.local

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry

/** Local (Room) storage for feedback board entries. */
interface FeedbackLocalDataSource {

    /** Id and `updatedAt` of every stored entry, the client list for the delta sync. */
    suspend fun getFeedbackEntryChangeIds(): List<IdChangeDate>

    suspend fun upsertFeedbackEntries(entries: List<FeedbackEntry>)

    suspend fun deleteFeedbackEntries(ids: List<String>)

    fun getAllFeedbackEntriesFlow(): Flow<List<FeedbackEntry>>
}

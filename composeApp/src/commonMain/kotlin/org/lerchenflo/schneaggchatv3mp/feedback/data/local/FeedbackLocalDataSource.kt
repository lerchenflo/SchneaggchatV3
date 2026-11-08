package org.lerchenflo.schneaggchatv3mp.feedback.data.local

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry

/** Local (Room) storage for feedback board entries. */
interface FeedbackLocalDataSource {

    /** Id and `updatedAt` of every stored entry, the client list for the delta sync. */
    suspend fun getFeedbackEntryChangeIds(): List<IdChangeDate>

    /** Own entries stored the first time get their current comment count as seen (baseline). */
    suspend fun upsertFeedbackEntries(entries: List<FeedbackEntry>)

    suspend fun deleteFeedbackEntries(ids: List<String>)

    fun getAllFeedbackEntriesFlow(): Flow<List<FeedbackEntry>>

    /** Entry id → comment count the user has seen on this device (own entries only). */
    fun getSeenCommentCountsFlow(): Flow<Map<String, Int>>

    suspend fun setSeenCommentCount(entryId: String, commentCount: Int)

    /** The user's own new comment: seen at once, so it never shows as new. */
    suspend fun incrementSeenCommentCount(entryId: String)
}

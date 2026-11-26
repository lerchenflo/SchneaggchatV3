package org.lerchenflo.schneaggchatv3mp.feedback.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult

/**
 * The feedback board (feature requests and bug reports). The entries are stored locally and kept
 * up to date by [syncFeedback]; writes go to the server first and store the server's answer.
 * The details of an entry (comments, location, app version) are always fetched live.
 */
interface FeedbackRepository {

    /** All stored entries, features and bugs together, with [FeedbackEntry.newCommentCount] filled. */
    fun getEntriesFlow(): Flow<List<FeedbackEntry>>

    /** Admin flag from the last sync, false until the first sync is done. */
    fun getViewerIsAdminFlow(): Flow<Boolean>

    /** Pulls changed and deleted entries from the server into the local store. */
    suspend fun syncFeedback(): EmptyDataResult<DataError>

    /** Home screen badge: the sum of [getTabUnseenCountsFlow], so it stays until every tab with news was opened. */
    fun getUnseenCountFlow(): Flow<Int>

    /** Per tab: entries with a [FeedbackMark] since the user last opened that tab. */
    fun getTabUnseenCountsFlow(): Flow<Map<FeedbackTab, Int>>

    /** Server time the user last opened each tab at, 0 if never. */
    suspend fun getTabsLastSeen(): Map<FeedbackTab, Long>

    /** The user opened [tab]: everything stored in it counts as seen (server time of the last successful sync). */
    suspend fun markTabSeen(tab: FeedbackTab)

    /** The user opened [entryId]: its [commentCount] comments count as seen on this device. */
    suspend fun markCommentsSeen(entryId: String, commentCount: Int)

    /** Also stores the fresh entry that comes with the details, and marks its comments as seen. */
    suspend fun getDetails(entryId: String): DataResult<FeedbackEntryDetail, DataError>

    suspend fun create(entry: NewFeedbackEntry): DataResult<FeedbackEntry, DataError>

    /**
     * Saves [changes] to [entry] (creator or admin) and stores the server's answer. An edit that
     * changes nothing comes back unchanged, without an edit time.
     */
    suspend fun edit(entry: FeedbackEntry, changes: FeedbackEntryChanges): DataResult<FeedbackEntry, DataError>

    /** Stores the vote locally at once and replaces it with the server's answer, or undoes it on an error. */
    suspend fun vote(entry: FeedbackEntry, vote: FeedbackVote): DataResult<FeedbackEntry, DataError>

    /** [isBug] attaches app version and platform, which only matter for bug reports. */
    suspend fun comment(entryId: String, text: String, isBug: Boolean): DataResult<FeedbackComment, DataError>

    suspend fun deleteEntry(entryId: String): EmptyDataResult<DataError>

    suspend fun deleteComment(commentId: String): EmptyDataResult<DataError>

    /** New [text] for an own comment. */
    suspend fun editComment(commentId: String, text: String): DataResult<FeedbackComment, DataError>

    suspend fun setStatus(entryId: String, status: FeedbackStatus): DataResult<FeedbackEntry, DataError>
}

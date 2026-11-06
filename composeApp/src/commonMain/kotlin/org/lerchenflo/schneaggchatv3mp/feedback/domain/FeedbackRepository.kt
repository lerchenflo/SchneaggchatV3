package org.lerchenflo.schneaggchatv3mp.feedback.domain

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult

/** The feedback board (feature requests and bug reports), always read live from the server. */
interface FeedbackRepository {

    suspend fun getEntries(type: FeedbackType): DataResult<FeedbackList, DataError>

    suspend fun getDetails(entryId: String): DataResult<FeedbackEntryDetail, DataError>

    suspend fun create(entry: NewFeedbackEntry): DataResult<FeedbackEntry, DataError>

    suspend fun vote(entryId: String, vote: FeedbackVote): DataResult<FeedbackEntry, DataError>

    /** [isBug] attaches app version and platform, which only matter for bug reports. */
    suspend fun comment(entryId: String, text: String, isBug: Boolean): DataResult<FeedbackComment, DataError>

    suspend fun deleteEntry(entryId: String): EmptyDataResult<DataError>

    suspend fun deleteComment(commentId: String): EmptyDataResult<DataError>

    suspend fun setStatus(entryId: String, status: FeedbackStatus): DataResult<FeedbackEntry, DataError>
}

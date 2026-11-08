package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackComment
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryDetail
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackRepository
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.feedback.domain.NewFeedbackEntry

/** In-memory [FeedbackRepository]. The reads serve [entries] and [unseenCount]; the writes fail. */
class FakeFeedbackRepository : FeedbackRepository {

    val entries = MutableStateFlow<List<FeedbackEntry>>(emptyList())
    val unseenCount = MutableStateFlow(0)

    val syncCalls = CallLog<Unit>()

    override fun getEntriesFlow(): Flow<List<FeedbackEntry>> = entries

    override fun getViewerIsAdminFlow(): Flow<Boolean> = MutableStateFlow(false)

    override suspend fun syncFeedback(): EmptyDataResult<DataError> {
        syncCalls.record(Unit)
        return DataResult.Success(Unit)
    }

    override fun getUnseenCountFlow(): Flow<Int> = unseenCount

    override suspend fun getLastSeen(): Long = 0L

    override suspend fun markAllSeen() {
        unseenCount.value = 0
    }

    override suspend fun getDetails(entryId: String): DataResult<FeedbackEntryDetail, DataError> =
        DataResult.Error(DataError.Unknown())

    override suspend fun create(entry: NewFeedbackEntry): DataResult<FeedbackEntry, DataError> =
        DataResult.Error(DataError.Unknown())

    override suspend fun vote(entry: FeedbackEntry, vote: FeedbackVote): DataResult<FeedbackEntry, DataError> =
        DataResult.Error(DataError.Unknown())

    override suspend fun comment(entryId: String, text: String, isBug: Boolean): DataResult<FeedbackComment, DataError> =
        DataResult.Error(DataError.Unknown())

    override suspend fun deleteEntry(entryId: String): EmptyDataResult<DataError> = DataResult.Error(DataError.Unknown())

    override suspend fun deleteComment(commentId: String): EmptyDataResult<DataError> = DataResult.Error(DataError.Unknown())

    override suspend fun setStatus(entryId: String, status: FeedbackStatus): DataResult<FeedbackEntry, DataError> =
        DataResult.Error(DataError.Unknown())
}
